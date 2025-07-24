/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.pool.api.model.RelationType
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ManagedRelationUpsertService(
    private val relationUpsertService: RelationUpsertService,
    private val relationRepository: RelationRepository
):IRelationUpsertStrategyService {

    @Transactional
    override fun upsertRelation(upsertRequest: IRelationUpsertStrategyService.UpsertRequest): UpsertResult<RelationDb> {
        val (proposedSource, proposedTarget) = upsertRequest

        validateNoChain(proposedSource, proposedTarget)
        validateSingleManager(proposedSource)

        val result = relationUpsertService.upsertRelation(
            RelationUpsertService.UpsertRequest(
                source = proposedSource,
                target = proposedTarget,
                relationType = RelationType.IsManagedBy,
                states = listOf(
                    RelationUpsertService.RelationStateRequest(
                        type = BusinessStateType.ACTIVE,
                        validFrom = LocalDateTime.now(),
                        validTo = LocalDateTime.of(9999, 12, 31, 0, 0)
                    )
                ),
            )
        )

        return result

    }

    private fun validateNoChain(source: LegalEntityDb, target: LegalEntityDb) {
        val sourceRelations = relationRepository.findInSourceOrTarget(RelationType.IsManagedBy, source)
        val targetRelations = relationRepository.findInSourceOrTarget(RelationType.IsManagedBy, target)

        val sourceIsTarget = sourceRelations.any { it.endNode.id == source.id }
        val targetIsSource = targetRelations.any { it.startNode.id == target.id }

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

    private fun validateSingleManager(source: LegalEntityDb) {
        val existingManagers = relationRepository.findInSourceOrTarget(RelationType.IsManagedBy, source)
            .filter { it.startNode.id == source.id }

        if (existingManagers.isNotEmpty()) {
            val managerBpns = existingManagers.joinToString { it.endNode.bpn }
            throw BpdmValidationException(
                "Invalid 'IsManagedBy' relation: The Managed Legal Entity with BPNL '${source.bpn}' is already managed by another Managing Legal Entity '${managerBpns}'. " +
                        "A Managed Legal Entity may only have one Managing Legal Entity at a time."
            )
        }
    }

}