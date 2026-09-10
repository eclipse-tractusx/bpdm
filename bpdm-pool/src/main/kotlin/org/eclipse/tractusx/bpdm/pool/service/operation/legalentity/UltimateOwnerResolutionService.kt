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

package org.eclipse.tractusx.bpdm.pool.service.operation.legalentity

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.isValidOn
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Derives which legal entity ultimately owns another, from the currently valid `IsOwnedBy` relations and the
 * `ownershipUltimate` flags. Read-only: it computes, it never writes.
 *
 * The flag holder highest up the ownership chain wins, and an entity flagged itself is its own ultimate owner even when it
 * is owned by others. A chain that reaches no flagged entity resolves to `null`. Ownership cycles are guarded against and
 * logged rather than throwing, so a corrupt graph degrades to "no ultimate owner" instead of failing the write.
 */
@Service
class UltimateOwnerResolutionService(
    private val relationRepository: RelationRepository
) {

    private val logger = KotlinLogging.logger { }

    /**
     * The BPNL of [legalEntity]'s ultimate owner — itself when it is the flag holder — or null when its ownership chain
     * reaches no ultimate owner.
     */
    @Transactional(readOnly = true)
    fun resolve(legalEntity: LegalEntityDb): String? =
        resolveWithAlternativeGuard(legalEntity, mutableSetOf())

    private fun resolveWithAlternativeGuard(legalEntity: LegalEntityDb, visited: MutableSet<String>): String? {
        if (!visited.add(legalEntity.bpn)) {
            logger.warn { "Cycle detected in alternative headquarter chain at BPNL: ${legalEntity.bpn}" }
            return null
        }
        val alternativeMain = mainOfAlternative(legalEntity)
        if (alternativeMain != null) {
            return resolveWithAlternativeGuard(alternativeMain, visited)
        }
        return when (val resolution = resolveWithCycleProtection(legalEntity, mutableSetOf())) {
            is Resolution.UltimateOwner -> resolution.bpnl
            Resolution.CycleDetected -> null
        }
    }

    /**
     * Resolves [legalEntities] and every entity owned by them, transitively — the set whose ultimate owner can change
     * when one of [legalEntities] changes. Keyed by entity, in encounter order, each entity appearing once.
     */
    @Transactional(readOnly = true)
    fun resolveForEntitiesAndDescendants(legalEntities: List<LegalEntityDb>): Map<LegalEntityDb, String?> {
        val resolved = LinkedHashMap<LegalEntityDb, String?>()
        val visited = mutableSetOf<String>()
        legalEntities.forEach { collectWithDescendants(it, visited, resolved) }
        return resolved
    }

    private fun collectWithDescendants(
        legalEntity: LegalEntityDb,
        visited: MutableSet<String>,
        resolved: MutableMap<LegalEntityDb, String?>
    ) {
        if (!visited.add(legalEntity.bpn)) {
            logger.warn { "Cycle detected in descendant resolution at BPNL: ${legalEntity.bpn}" }
            return
        }

        resolved[legalEntity] = resolve(legalEntity)

        if (mainOfAlternative(legalEntity) != null) {
            return
        }

        currentlyValidRelations(relationRepository.findByTypeAndEndNode(LegalEntityRelationType.IsAlternativeHeadquarterFor, legalEntity))
            .forEach { alternativeRelation ->
                val alternative = alternativeRelation.startNode
                if (visited.add(alternative.bpn)) {
                    resolved[alternative] = resolved[legalEntity]
                }
            }

        currentlyValidRelations(relationRepository.findByTypeAndEndNode(LegalEntityRelationType.IsOwnedBy, legalEntity))
            .forEach { collectWithDescendants(it.startNode, visited, resolved) }
    }

    private fun mainOfAlternative(legalEntity: LegalEntityDb): LegalEntityDb? =
        currentlyValidRelations(relationRepository.findByTypeAndStartNode(LegalEntityRelationType.IsAlternativeHeadquarterFor, legalEntity))
            .firstOrNull()
            ?.endNode

    private fun resolveWithCycleProtection(legalEntity: LegalEntityDb, visited: MutableSet<String>): Resolution {
        val currentBpn = legalEntity.bpn

        if (!visited.add(currentBpn)) {
            logger.warn { "Cycle detected in ownership chain at BPNL: $currentBpn" }
            return Resolution.CycleDetected
        }

        var highestFlagHolder = currentBpn.takeIf { legalEntity.ownershipUltimate }
        val owningRelations = currentlyValidRelations(relationRepository.findByTypeAndStartNode(LegalEntityRelationType.IsOwnedBy, legalEntity))

        owningRelations.forEach { relation ->
            when (val ownerResolution = resolveWithCycleProtection(relation.endNode, visited)) {
                // A cycle anywhere above makes the whole chain untrustworthy, so it invalidates the flag holders found below it.
                Resolution.CycleDetected -> return Resolution.CycleDetected
                is Resolution.UltimateOwner -> highestFlagHolder = ownerResolution.bpnl ?: highestFlagHolder
            }
        }

        return Resolution.UltimateOwner(highestFlagHolder)
    }

    private fun currentlyValidRelations(relations: Collection<RelationDb>): List<RelationDb> {
        val today = LocalDate.now()
        return relations.filter { it.isValidOn(today) }
    }

    /** The outcome of walking one ownership chain: the flag holder it reached, or the cycle that stopped the walk. */
    private sealed interface Resolution {
        data class UltimateOwner(val bpnl: String?) : Resolution
        data object CycleDetected : Resolution
    }
}
