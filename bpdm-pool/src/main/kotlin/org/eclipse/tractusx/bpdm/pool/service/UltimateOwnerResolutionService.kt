/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class UltimateOwnerResolutionService(
    private val relationRepository: RelationRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val changelogService: PartnerChangelogService
) {
    private val logger = KotlinLogging.logger { }

    @Transactional(readOnly = true)
    fun resolveUltimateOwner(legalEntity: LegalEntityDb): String? {
        val visited = mutableSetOf<String>()
        return resolveHighestFlaggedAncestorWithCycleProtection(legalEntity, visited)
    }


    private fun resolveHighestFlaggedAncestorWithCycleProtection(legalEntity: LegalEntityDb, visited: MutableSet<String>): String? {
        val currentBpn = legalEntity.bpn

        if (currentBpn in visited) {
            logger.warn { "Cycle detected in ownership chain at BPNL: $currentBpn" }
            return null
        }
        visited.add(currentBpn)

        val owningRelations = relationRepository.findByTypeAndStartNode(LegalEntityRelationType.IsOwnedBy, legalEntity)
        val validOwningRelations = owningRelations.filter { isRelationCurrentlyValid(it) }

        var highestFlaggedOwner = if (legalEntity.ownershipUltimate) currentBpn else null

        for (relation in validOwningRelations) {
            val parent = relation.endNode
            val parentHighestFlaggedOwner = resolveHighestFlaggedAncestorWithCycleProtection(parent, visited)
            if (parentHighestFlaggedOwner != null) {
                highestFlaggedOwner = parentHighestFlaggedOwner
            }
        }

        return highestFlaggedOwner
    }

    @Transactional
    fun updateUltimateOwnerForEntityAndDescendants(legalEntity: LegalEntityDb, visited: MutableSet<String> = mutableSetOf()) {
        val currentBpn = legalEntity.bpn

        if (currentBpn in visited) {
            logger.warn { "Cycle detected in descendant update at BPNL: $currentBpn" }
            return
        }
        visited.add(currentBpn)

        val resolvedUltimateOwnerBpnl = resolveUltimateOwner(legalEntity)
        // The flagged legal entity itself has no ultimateOwnerBpnl; only descendants point to it.
        val ultimateOwnerBpnlToStore = if (resolvedUltimateOwnerBpnl == legalEntity.bpn) null else resolvedUltimateOwnerBpnl
        val previousUltimateOwnerBpnl = legalEntity.ultimateOwnerBpnl

        if (previousUltimateOwnerBpnl != ultimateOwnerBpnlToStore) {
            legalEntity.ultimateOwnerBpnl = ultimateOwnerBpnlToStore
            legalEntityRepository.save(legalEntity)
            changelogService.createChangelogEntry(
                ChangelogEntryCreateRequest(legalEntity.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY)
            )
            logger.debug { "Updated ultimateOwnerBpnl for ${legalEntity.bpn} from $previousUltimateOwnerBpnl to $ultimateOwnerBpnlToStore" }
        } else {
            logger.debug { "ultimateOwnerBpnl for ${legalEntity.bpn} unchanged: $ultimateOwnerBpnlToStore" }
        }

        val childRelations = relationRepository.findByTypeAndEndNode(LegalEntityRelationType.IsOwnedBy, legalEntity)
        val validChildRelations = childRelations.filter { isRelationCurrentlyValid(it) }

        for (relation in validChildRelations) {
            val child = relation.startNode
            updateUltimateOwnerForEntityAndDescendants(child, visited)
        }
    }

    private fun isRelationCurrentlyValid(relation: org.eclipse.tractusx.bpdm.pool.entity.RelationDb): Boolean {
        if (relation.validityPeriods.isEmpty()) {
            return false
        }
        
        val today = LocalDate.now()
        return relation.validityPeriods.any { period ->
            val isAfterOrOnStart = today >= period.validFrom
            val isBeforeOrOnEnd = period.validTo == null || today <= period.validTo
            isAfterOrOnStart && isBeforeOrOnEnd
        }
    }


    @Transactional(readOnly = true)
    fun validateOnlyOneUltimateOwnerInHierarchy(legalEntity: LegalEntityDb) {
        val visited = mutableSetOf<String>()
        val flaggedEntities = mutableListOf<String>()

        collectAllEntitiesInHierarchy(legalEntity, visited, flaggedEntities)

        if (flaggedEntities.size > 1) {
            throw BpdmValidationException(
                "Multiple ultimate owners detected in the same IsOwnedBy hierarchy. " +
                "The following entities are flagged as ownership ultimate: ${flaggedEntities.joinToString(", ")}. " +
                "An IsOwnedBy hierarchy can have at most one entity with ownershipUltimate = true."
            )
        }
    }

    private fun collectAllEntitiesInHierarchy(
        legalEntity: LegalEntityDb,
        visited: MutableSet<String>,
        flaggedEntities: MutableList<String>
    ) {
        val currentBpn = legalEntity.bpn

        if (currentBpn in visited) {
            logger.debug { "Already visited $currentBpn, skipping to avoid cycle" }
            return
        }
        visited.add(currentBpn)

        if (legalEntity.ownershipUltimate) {
            flaggedEntities.add(currentBpn)
        }

        val owningRelations = relationRepository.findByTypeAndStartNode(LegalEntityRelationType.IsOwnedBy, legalEntity)
        val validOwningRelations = owningRelations.filter { isRelationCurrentlyValid(it) }

        for (relation in validOwningRelations) {
            val parent = relation.endNode
            collectAllEntitiesInHierarchy(parent, visited, flaggedEntities)
        }

        val ownedRelations = relationRepository.findByTypeAndEndNode(LegalEntityRelationType.IsOwnedBy, legalEntity)
        val validOwnedRelations = ownedRelations.filter { isRelationCurrentlyValid(it) }

        for (relation in validOwnedRelations) {
            val child = relation.startNode
            collectAllEntitiesInHierarchy(child, visited, flaggedEntities)
        }
    }

}
