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
import org.springframework.stereotype.Service

/**
 * Checks that an alternative headquarter relation keeps alternative headquarters in a star around one main legal entity
 * and out of every ownership hierarchy.
 */
@Service
class AlternativeHeadquarterRelationValidator(
    private val relationRepository: RelationRepository
) {

    /**
     * Reports why [alternative] cannot be an alternative headquarter of [main] during [validityPeriods], where
     * [existingRelation] is the relation being rewritten.
     */
    fun validate(
        alternative: LegalEntityDb,
        main: LegalEntityDb,
        validityPeriods: List<RelationValidityPeriodRaw>,
        existingRelation: RelationDb?
    ): List<RelationUpsertError> {
        val relationsDuringPeriods = { relations: Collection<RelationDb> ->
            overlappingRelations(relations, existingRelation, validityPeriods) { it.validityPeriods }
        }

        return listOfNotNull(
            starTopologyViolation(alternative, main, relationsDuringPeriods),
            alternativeInOwnership(alternative, relationsDuringPeriods),
            RelationUpsertError.AlternativeFlaggedUltimate(alternative.bpn).takeIf { alternative.ownershipUltimate }
        )
    }

    private fun starTopologyViolation(
        alternative: LegalEntityDb,
        main: LegalEntityDb,
        relationsDuringPeriods: (Collection<RelationDb>) -> List<RelationDb>
    ): RelationUpsertError? {
        val alternativeRelations = relationsDuringPeriods(
            relationRepository.findInSourceOrTarget(LegalEntityRelationTypeV7.IsAlternativeHeadquarterFor, alternative)
        ).filterNot { it.startNode.bpn == alternative.bpn && it.endNode.bpn == main.bpn }

        val alternativeViolation = alternativeRelations.firstNotNullOfOrNull { relation ->
            when {
                relation.endNode.bpn == alternative.bpn && relation.startNode.bpn != main.bpn ->
                    RelationUpsertError.AlternativeAlreadyMain(alternative.bpn, main.bpn, relation.startNode.bpn)
                relation.startNode.bpn == main.bpn && relation.endNode.bpn != alternative.bpn ->
                    RelationUpsertError.MainAlreadyAlternativeTo(main.bpn, alternative.bpn, relation.endNode.bpn)
                relation.startNode.bpn == alternative.bpn && relation.endNode.bpn != main.bpn ->
                    RelationUpsertError.AlternativeAlreadyAlternative(alternative.bpn, main.bpn, relation.endNode.bpn)
                else -> null
            }
        }
        if (alternativeViolation != null) return alternativeViolation

        val mainRelations = relationsDuringPeriods(relationRepository.findInSourceOrTarget(LegalEntityRelationTypeV7.IsAlternativeHeadquarterFor, main))
        val mainAsOtherAlternative = mainRelations.find { it.startNode.bpn == main.bpn && it.endNode.bpn != alternative.bpn }
        if (mainAsOtherAlternative != null)
            return RelationUpsertError.MainAlreadyAlternative(main.bpn, alternative.bpn, mainAsOtherAlternative.endNode.bpn)

        return RelationUpsertError.ReversedAlternative(alternative.bpn, main.bpn)
            .takeIf { mainRelations.any { it.startNode.bpn == main.bpn && it.endNode.bpn == alternative.bpn } }
    }

    private fun alternativeInOwnership(
        alternative: LegalEntityDb,
        relationsDuringPeriods: (Collection<RelationDb>) -> List<RelationDb>
    ): RelationUpsertError? =
        relationsDuringPeriods(relationRepository.findInSourceOrTarget(LegalEntityRelationTypeV7.IsOwnedBy, alternative))
            .firstOrNull()
            ?.let { ownership ->
                val isOwned = ownership.startNode.bpn == alternative.bpn
                val otherBpn = if (isOwned) ownership.endNode.bpn else ownership.startNode.bpn
                RelationUpsertError.AlternativeInOwnership(alternative.bpn, isOwned, otherBpn)
            }
}
