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

import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlternativeHeadquarterRelationUpsertService(
    private val relationUpsertService: RelationUpsertService,
    private val relationRepository: RelationRepository
): IRelationUpsertStrategyService {

    @Transactional
    override fun upsertRelation(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): UpsertResult<RelationDb>{
        val alternative = upsertRequest.source
        val main = upsertRequest.target

        validateStarTopology(alternative, main, upsertRequest)
        validateAlternativeNotInOwnership(alternative, upsertRequest)
        validateAlternativeNotFlagged(alternative)

        val result = relationUpsertService.upsertRelation(
            RelationUpsertService.UpsertRequest(
                source = alternative,
                target = main,
                legalEntityRelationType = LegalEntityRelationType.IsAlternativeHeadquarterFor,
                validityPeriods = upsertRequest.validityPeriods,
                existingRelation = upsertRequest.existingRelation,
                reasonCode = upsertRequest.reasonCode
            )
        )

        return result
    }


    private fun validateStarTopology(alternative: LegalEntityDb, main: LegalEntityDb, upsertRequest: IRelationUpsertStrategyService.UpsertRequest) {
        val allAltHqRelations = relationRepository.findInSourceOrTarget(LegalEntityRelationType.IsAlternativeHeadquarterFor, alternative)
        val overlappingRelations = relationUpsertService.filterOverlappingRelations(upsertRequest, allAltHqRelations)

        val otherRelations = overlappingRelations.filterNot { it.startNode.bpn == alternative.bpn && it.endNode.bpn == main.bpn }

        for (relation in otherRelations) {
            when {
                relation.endNode.bpn == alternative.bpn && relation.startNode.bpn != main.bpn -> {
                    throw BpdmValidationException(
                        "Star topology violated: Legal entity '${alternative.bpn}' is already the main of an " +
                        "alternative relation with '${relation.startNode.bpn}' in an overlapping period. " +
                        "Cannot also be alternative to '${main.bpn}'."
                    )
                }
                relation.startNode.bpn == main.bpn && relation.endNode.bpn != alternative.bpn -> {
                    throw BpdmValidationException(
                        "Star topology violated: Legal entity '${main.bpn}' is already alternative to " +
                        "'${relation.endNode.bpn}' in an overlapping period. Cannot also be main to '${alternative.bpn}'."
                    )
                }
                relation.startNode.bpn == alternative.bpn && relation.endNode.bpn != main.bpn -> {
                    throw BpdmValidationException(
                        "Star topology violated: Legal entity '${alternative.bpn}' is already alternative to " +
                        "'${relation.endNode.bpn}' in an overlapping period. Cannot also be alternative to '${main.bpn}'."
                    )
                }
            }
        }

        val allMainRelations = relationRepository.findInSourceOrTarget(LegalEntityRelationType.IsAlternativeHeadquarterFor, main)
        val overlappingMainRelations = relationUpsertService.filterOverlappingRelations(upsertRequest, allMainRelations)

        val targetAlreadyAlternative = overlappingMainRelations.any {
            it.startNode.bpn == main.bpn && it.endNode.bpn != alternative.bpn
        }
        if (targetAlreadyAlternative) {
            val conflictingRelation = overlappingMainRelations.first { it.startNode.bpn == main.bpn && it.endNode.bpn != alternative.bpn }
            throw BpdmValidationException(
                "Star topology violated: Legal entity '${main.bpn}' is already alternative to '${conflictingRelation.endNode.bpn}' in an overlapping period. " +
                        "Cannot also be main for '${alternative.bpn}'."
            )
        }

        val reverseRelation = overlappingMainRelations.find { it.startNode.bpn == main.bpn && it.endNode.bpn == alternative.bpn }
        if (reverseRelation != null) {
            throw BpdmValidationException(
                "Cannot reverse alternative headquarter relation: '${main.bpn}' cannot be alternative to '${alternative.bpn}' " +
                "while the reverse relation ('${alternative.bpn}' alternative to '${main.bpn}') exists in an overlapping period. " +
                "End the original relation first."
            )
        }
    }


    private fun validateAlternativeNotInOwnership(alternative: LegalEntityDb, upsertRequest: IRelationUpsertStrategyService.UpsertRequest) {
        val ownedByRelations = relationRepository.findInSourceOrTarget(LegalEntityRelationType.IsOwnedBy, alternative)
        val overlappingOwnedByRelations = relationUpsertService.filterOverlappingRelations(upsertRequest, ownedByRelations)

        if (overlappingOwnedByRelations.isNotEmpty()) {
            val relation = overlappingOwnedByRelations.first()
            val role = if (relation.startNode.bpn == alternative.bpn) "owned" else "owning"
            throw BpdmValidationException(
                "Invalid alternative headquarter relation: Legal entity '${alternative.bpn}' cannot be alternative " +
                "because it participates in an overlapping IsOwnedBy relation (as $role entity with '${if (role == "owned") relation.endNode.bpn else relation.startNode.bpn}')."
            )
        }
    }

    private fun validateAlternativeNotFlagged(alternative: LegalEntityDb) {
        if (alternative.ownershipUltimate) {
            throw BpdmValidationException(
                "Invalid alternative headquarter relation: Legal entity '${alternative.bpn}' cannot be alternative " +
                "because it carries the ownershipUltimate flag."
            )
        }
    }
}