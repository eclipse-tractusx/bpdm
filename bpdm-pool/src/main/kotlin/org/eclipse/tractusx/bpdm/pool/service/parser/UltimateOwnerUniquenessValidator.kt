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

package org.eclipse.tractusx.bpdm.pool.service.parser

import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.isValidOn
import org.eclipse.tractusx.bpdm.pool.model.error.MultipleUltimateOwnersInHierarchy
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * The rule that an ownership tree may hold at most one legal entity flagged as ultimate owner. It decides on the state the
 * write *would* leave behind, never on state already written: the two things that can break the rule — the flag itself and
 * an `IsOwnedBy` edge joining two trees — are passed in as proposals.
 *
 * Only relations valid today count as edges, matching how the ultimate owner is derived. A tree that already holds more
 * than one flag is reported too, rather than tolerated because it predates the rule: clearing a flag is always allowed, so
 * the surplus can still be removed.
 */
@Service
class UltimateOwnerUniquenessValidator(
    private val relationRepository: RelationRepository
) {

    /**
     * The violations each entry of a legal-entity update batch would cause, positional with [targets] and
     * [requestedFlags]: an unresolved target or a flag left unstated by the request yields none.
     */
    @Transactional(readOnly = true)
    fun validate(targets: List<LegalEntityDb?>, requestedFlags: List<Boolean?>): List<List<MultipleUltimateOwnersInHierarchy>> {
        require(targets.size == requestedFlags.size) { "targets and requestedFlags must be positionally aligned" }

        // Entries of the same batch can sit in one tree, so an entry is judged against what its neighbours will be flagged
        // as after this batch, not against what they are flagged as now.
        val flagsAfterBatch = targets.zip(requestedFlags)
            .mapNotNull { (target, requestedFlag) -> target?.let { it.bpn to (requestedFlag ?: it.ownershipUltimate) } }
            .toMap()

        return targets.zip(requestedFlags).map { (target, requestedFlag) ->
            when {
                target == null -> emptyList()
                !(requestedFlag ?: target.ownershipUltimate) -> emptyList()
                else -> violationsOf(conflictingFlagHoldersOf(target, flagsAfterBatch))
            }
        }
    }

    /** The violations that adding an `IsOwnedBy` relation from [source] to [target] would cause. */
    @Transactional(readOnly = true)
    fun validateOwnershipEdge(source: LegalEntityDb, target: LegalEntityDb): List<MultipleUltimateOwnersInHierarchy> {
        val mergedTree = (ownershipTreeOf(source) + ownershipTreeOf(target)).distinctBy { it.bpn }
        val flagHolders = mergedTree.filter { it.ownershipUltimate }.map { it.bpn }

        return if (flagHolders.size > 1) violationsOf(flagHolders) else emptyList()
    }

    private fun conflictingFlagHoldersOf(target: LegalEntityDb, flagsAfterBatch: Map<String, Boolean>): List<String> =
        ownershipTreeOf(target)
            .filter { it.bpn != target.bpn }
            .filter { flagsAfterBatch[it.bpn] ?: it.ownershipUltimate }
            .map { it.bpn }

    private fun violationsOf(flagHolders: List<String>): List<MultipleUltimateOwnersInHierarchy> =
        if (flagHolders.isEmpty()) emptyList() else listOf(MultipleUltimateOwnersInHierarchy(flagHolders.distinct().sorted()))

    private fun ownershipTreeOf(legalEntity: LegalEntityDb): List<LegalEntityDb> {
        val today = LocalDate.now()
        val collected = LinkedHashMap<String, LegalEntityDb>()
        val pending = ArrayDeque(listOf(legalEntity))

        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (collected.put(current.bpn, current) != null) continue

            relationRepository.findByTypeAndStartNode(LegalEntityRelationType.IsOwnedBy, current)
                .filter { it.isValidOn(today) }
                .forEach { pending.add(it.endNode) }
            relationRepository.findByTypeAndEndNode(LegalEntityRelationType.IsOwnedBy, current)
                .filter { it.isValidOn(today) }
                .forEach { pending.add(it.startNode) }
        }

        return collected.values.toList()
    }
}
