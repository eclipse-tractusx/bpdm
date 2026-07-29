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

package org.eclipse.tractusx.bpdm.pool.service.operation

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
 * A chain that reaches no entity flagged as ultimate owner resolves to `null`. Ownership cycles are guarded against and
 * logged rather than throwing, so a corrupt graph degrades to "no ultimate owner" instead of failing the write.
 */
@Service
class UltimateOwnerResolutionService(
    private val relationRepository: RelationRepository
) {

    private val logger = KotlinLogging.logger { }

    /** Resolves the BPNL of [legalEntity]'s ultimate owner, or null when its ownership chain reaches no ultimate owner. */
    @Transactional(readOnly = true)
    fun resolve(legalEntity: LegalEntityDb): String? =
        resolveWithCycleProtection(legalEntity, mutableSetOf())

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

        currentlyValidRelations(relationRepository.findByTypeAndEndNode(LegalEntityRelationType.IsOwnedBy, legalEntity))
            .forEach { collectWithDescendants(it.startNode, visited, resolved) }
    }

    private fun resolveWithCycleProtection(legalEntity: LegalEntityDb, visited: MutableSet<String>): String? {
        val currentBpn = legalEntity.bpn

        if (!visited.add(currentBpn)) {
            logger.warn { "Cycle detected in ownership chain at BPNL: $currentBpn" }
            return null
        }

        val owningRelations = currentlyValidRelations(relationRepository.findByTypeAndStartNode(LegalEntityRelationType.IsOwnedBy, legalEntity))
        if (owningRelations.isEmpty()) return if (legalEntity.ownershipUltimate) currentBpn else null

        return owningRelations.firstNotNullOfOrNull { resolveWithCycleProtection(it.endNode, visited) }
    }

    private fun currentlyValidRelations(relations: Collection<RelationDb>): List<RelationDb> {
        val today = LocalDate.now()
        return relations.filter { it.isValidOn(today) }
    }
}
