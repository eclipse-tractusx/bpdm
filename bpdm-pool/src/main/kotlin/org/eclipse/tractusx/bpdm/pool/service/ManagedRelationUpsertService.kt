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

import org.eclipse.tractusx.bpdm.pool.api.model.DataSpaceParticipantDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.request.DataSpaceParticipantUpdateRequest
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManagedRelationUpsertService(
    private val relationUpsertService: RelationUpsertService,
    private val relationRepository: RelationRepository,
    private val dataSpaceParticipantsService: DataSpaceParticipantsService
):IRelationUpsertStrategyService {

    @Transactional
    override fun upsertRelation(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): UpsertResult<RelationDb> {

        validateManagingEntityIsParticipant(upsertRequest.target)
        validateSingleManager(upsertRequest)
        validateNoChain(upsertRequest)

        val result = relationUpsertService.upsertRelation(
            RelationUpsertService.UpsertRequest(
                source = upsertRequest.source,
                target = upsertRequest.target,
                legalEntityRelationType = LegalEntityRelationType.IsManagedBy,
                validityPeriods = upsertRequest.validityPeriods,
                reasonCode = upsertRequest.reasonCode
            )
        )
        makeManagedEntityParticipantIfRequired(upsertRequest.source)

        return result
    }

    private fun validateSingleManager(upsertRequest: IRelationUpsertStrategyService.UpsertRequest){
        val source = upsertRequest.source

        val existingManagers = relationRepository.findByTypeAndStartNode(LegalEntityRelationType.IsManagedBy, source)
        val overlappingExistingManagers = relationUpsertService.filterOverlappingRelations(upsertRequest, existingManagers)

        if (overlappingExistingManagers.isNotEmpty()) {
            val managerBpns = overlappingExistingManagers.joinToString { it.endNode.bpn }
            throw BpdmValidationException(
                "Invalid 'IsManagedBy' relation: The Managed Legal Entity with BPNL '${source.bpn}' is already managed by another Managing Legal Entity '${managerBpns}'. " +
                        "A Managed Legal Entity may only have one Managing Legal Entity at a time."
            )
        }
    }

    private fun validateNoChain(upsertRequest: IRelationUpsertStrategyService.UpsertRequest) {
        val source = upsertRequest.source
        val target = upsertRequest.target

        val sourceRelations = relationRepository.findInSourceOrTarget(LegalEntityRelationType.IsManagedBy, source)
        val targetRelations = relationRepository.findInSourceOrTarget(LegalEntityRelationType.IsManagedBy, target)

        val overlappingSourceRelations = relationUpsertService.filterOverlappingRelations(upsertRequest, sourceRelations)
        val overlappingTargetRelations = relationUpsertService.filterOverlappingRelations(upsertRequest, targetRelations)

        val sourceIsTarget = overlappingSourceRelations.any { it.endNode.id == source.id }
        val targetIsSource = overlappingTargetRelations.any { it.startNode.id == target.id }

        when {
            sourceIsTarget -> throw BpdmValidationException(
                "Invalid 'IsManagedBy' relation: The legal entity with BPNL '${source.bpn}' is already a Managing Legal Entity. " +
                        "A Managing Legal Entity cannot also act as a Managed Legal Entity."
            )
            targetIsSource -> throw BpdmValidationException(
                "Invalid 'IsManagedBy' relation: The legal entity with BPNL '${target.bpn}' is already a Managed Legal Entity. " +
                        "A Managed Legal Entity cannot also act as a Managing Legal Entity."
            )
        }
    }

    private fun validateManagingEntityIsParticipant(target: LegalEntityDb) {
        if (!target.isCatenaXMemberData) {
            throw BpdmValidationException(
                "Invalid 'IsManagedBy' relation: The Managing Legal Entity with BPNL '${target.bpn}' is not a dataspace participant. " +
                        "Only dataspace participants can manage other entities."
            )
        }
    }

    private fun makeManagedEntityParticipantIfRequired(source: LegalEntityDb) {
        if (!source.isCatenaXMemberData) {
            dataSpaceParticipantsService.updateMemberships(
                DataSpaceParticipantUpdateRequest(
                    listOf(DataSpaceParticipantDto(source.bpn, true))
                )
            )
        }
    }
}