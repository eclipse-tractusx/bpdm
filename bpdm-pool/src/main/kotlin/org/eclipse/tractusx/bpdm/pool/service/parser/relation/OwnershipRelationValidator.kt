/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.parser.relation

import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.RelationUpsertError
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationValidityPeriodRaw
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.UltimateOwnerUniquenessValidator
import org.springframework.stereotype.Service

/**
 * Checks that an ownership between two legal entities keeps every ownership hierarchy a tree with a single ultimate owner
 * and no alternative headquarter in it.
 */
@Service
class OwnershipRelationValidator(
    private val relationRepository: RelationRepository,
    private val ultimateOwnerUniquenessValidator: UltimateOwnerUniquenessValidator
) {

    /**
     * Reports why [owned] cannot be owned by [owner] during [validityPeriods], where [existingRelation] is the ownership being rewritten.
     */
    fun validate(
        owned: LegalEntityDb,
        owner: LegalEntityDb,
        validityPeriods: List<RelationValidityPeriodRaw>,
        existingRelation: RelationDb?
    ): List<RelationUpsertError> {
        val relationsDuringPeriods = { relations: Collection<RelationDb> ->
            overlappingRelations(relations, existingRelation, validityPeriods) { it.validityPeriods }
        }

        return listOfNotNull(
            multipleOwners(owned, owner, relationsDuringPeriods),
            ownershipCycle(owned, owner, relationsDuringPeriods),
            multipleUltimateOwners(owned, owner),
            ownershipOfAlternative(owned, relationsDuringPeriods) ?: ownershipOfAlternative(owner, relationsDuringPeriods)
        )
    }

    private fun multipleOwners(
        owned: LegalEntityDb,
        owner: LegalEntityDb,
        relationsDuringPeriods: (Collection<RelationDb>) -> List<RelationDb>
    ): RelationUpsertError? =
        relationsDuringPeriods(relationRepository.findByTypeAndStartNode(LegalEntityRelationTypeV7.IsOwnedBy, owned))
            .firstOrNull { it.endNode.bpn != owner.bpn }
            ?.let { RelationUpsertError.MultipleOwners(owned.bpn, owner.bpn, it.endNode.bpn) }

    private fun ownershipCycle(
        owned: LegalEntityDb,
        owner: LegalEntityDb,
        relationsDuringPeriods: (Collection<RelationDb>) -> List<RelationDb>
    ): RelationUpsertError? {
        val ancestorBpns = mutableSetOf<String>()
        val ancestorQueue = ArrayDeque(listOf(owner))

        while (ancestorQueue.isNotEmpty()) {
            val ancestor = ancestorQueue.removeFirst()
            if (!ancestorBpns.add(ancestor.bpn)) continue

            relationsDuringPeriods(relationRepository.findByTypeAndStartNode(LegalEntityRelationTypeV7.IsOwnedBy, ancestor))
                .forEach { ancestorQueue.addFirst(it.endNode) }
        }

        return RelationUpsertError.OwnershipCycle(owned.bpn, owner.bpn).takeIf { owned.bpn in ancestorBpns }
    }

    private fun multipleUltimateOwners(owned: LegalEntityDb, owner: LegalEntityDb): RelationUpsertError? =
        ultimateOwnerUniquenessValidator.validateOwnershipEdge(owned, owner).firstOrNull()
            ?.let { RelationUpsertError.MultipleUltimateOwners(owned.bpn, owner.bpn, it.conflictingBpnls) }

    private fun ownershipOfAlternative(
        legalEntity: LegalEntityDb,
        relationsDuringPeriods: (Collection<RelationDb>) -> List<RelationDb>
    ): RelationUpsertError? =
        relationsDuringPeriods(relationRepository.findByTypeAndStartNode(LegalEntityRelationTypeV7.IsAlternativeHeadquarterFor, legalEntity))
            .firstOrNull()
            ?.let { RelationUpsertError.OwnershipOfAlternative(legalEntity.bpn, it.endNode.bpn) }
}
