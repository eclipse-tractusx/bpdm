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

import jakarta.transaction.Transactional
import org.eclipse.tractusx.bpdm.pool.api.model.AddressRelationType
import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.repository.ReasonCodeRepository
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.RelationValidityPeriod
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.springframework.stereotype.Service
import org.eclipse.tractusx.orchestrator.api.model.RelationType as OrchestratorRelationType

@Service
class TaskAddressRelationsStepBuildService(
    private val logisticAddressRepository: LogisticAddressRepository,
    private val addressRelationUpsertService: AddressRelationUpsertService,
    private val reasonCodeRepository: ReasonCodeRepository
) {

    @Transactional
    fun upsertAddressRelations(taskEntry: TaskRelationsStepReservationEntryDto): TaskRelationsStepResultEntryDto {

        val addressRelationDto = taskEntry.businessPartnerRelations

        if (addressRelationDto.businessPartnerSourceBpn == addressRelationDto.businessPartnerTargetBpn) {
            throw BpdmValidationException("An Address cannot have a relation to itself (BPNA: ${addressRelationDto.businessPartnerSourceBpn}).")
        }

        // Fetch source & target addresses
        val sourceAddress = logisticAddressRepository.findByBpn(addressRelationDto.businessPartnerSourceBpn)
            ?: throw BpdmValidationException("Source address BPNA ${addressRelationDto.businessPartnerSourceBpn} not found")

        val targetAddress = logisticAddressRepository.findByBpn(addressRelationDto.businessPartnerTargetBpn)
            ?: throw BpdmValidationException("Target address BPNA ${addressRelationDto.businessPartnerTargetBpn} not found")

        val reasonCode  = reasonCodeRepository.findByTechnicalKey(addressRelationDto.reasonCode)
            ?: throw BpdmValidationException("Relation reason code '${addressRelationDto.reasonCode}' not found")

        validateValidityPeriods(addressRelationDto)

        // Map states from orchestrator
        val validityPeriods = addressRelationDto.validityPeriods.map {
            RelationValidityPeriodDb(
                validFrom = it.validFrom,
                validTo = it.validTo
            )
        }

        val upsertRequest = IAddressRelationUpsertStratergyService.UpsertRequest(
            source = sourceAddress,
            target = targetAddress,
            validityPeriods = validityPeriods,
            reasonCode = reasonCode
        )
        val strategyService: IAddressRelationUpsertStratergyService = when(addressRelationDto.relationType) {
            OrchestratorRelationType.IsReplacedBy -> addressRelationUpsertService
            else -> throw BpdmValidationException("Unsupported address relation type: ${addressRelationDto.relationType}")
        }

        val upsertResult = strategyService.upsertRelation(upsertRequest)
        return TaskRelationsStepResultEntryDto(
            taskId = taskEntry.taskId,
            errors = emptyList(),
            businessPartnerRelations = upsertResult.relation.toTaskDto()
        )
    }

    private fun AddressRelationDb.toTaskDto(): BusinessPartnerRelations{
        return BusinessPartnerRelations(
            relationType = this.type.toTaskDto(),
            businessPartnerSourceBpn = this.startAddress.bpn,
            businessPartnerTargetBpn = this.endAddress.bpn,
            validityPeriods = this.validityPeriods.sortedBy { it.validFrom }.map {
                RelationValidityPeriod(
                    validFrom = it.validFrom,
                    validTo = it.validTo
                )
            },
            reasonCode = reasonCode.technicalKey
        )
    }

    private fun AddressRelationType.toTaskDto(): OrchestratorRelationType{
        return when(this){
            AddressRelationType.IsReplacedBy -> OrchestratorRelationType.IsReplacedBy
        }
    }

    private fun validateValidityPeriods(relation: BusinessPartnerRelations) {
        val orderedValidityPeriods = relation.validityPeriods.sortedBy { it.validFrom }

        if(orderedValidityPeriods.isEmpty()){
            throw BpdmValidationException("Relation validity periods cannot be empty, at least one validity needed.")
        }

        orderedValidityPeriods.first().let { state ->
            if (state.validTo != null && state.validFrom.isAfter(state.validTo)) {
                throw BpdmValidationException("Relation validity period validFrom '${state.validFrom}' cannot be after validTo '${state.validTo}'.")
            }
        }

        val orderedTimePeriods = orderedValidityPeriods.map { RelationUpsertService.TimePeriod.fromUnlimited(it.validFrom, it.validTo) }
        val consecutiveTimePeriodPairs =  orderedTimePeriods.zip(orderedTimePeriods.drop(1))

        val anyOverlap =  consecutiveTimePeriodPairs
            .any { (state1, state2) -> state1.hasOverlap(state2) }

        if(anyOverlap){
            throw BpdmValidationException("Relation validity periods must not overlap.")
        }
    }
}
