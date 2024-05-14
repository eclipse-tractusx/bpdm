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

package org.eclipse.tractusx.bpdm.gate.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.SyncTypeDb
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GoldenRecordTaskService(
    private val sharingStateRepository: SharingStateRepository,
    private val sharingStateService: SharingStateService,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val businessPartnerService: BusinessPartnerService,
    private val orchestratorMappings: OrchestratorMappings,
    private val orchestrationApiClient: OrchestrationApiClient,
    private val properties: GoldenRecordTaskConfigProperties
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createTasksForReadyBusinessPartners() {
        logger.info { "Started scheduled task to create golden record tasks from ready business partners" }

        val pageRequest = Pageable.ofSize(properties.creation.fromSharingMember.batchSize)
        val foundStates = sharingStateRepository.findBySharingStateType(SharingStateType.Ready, pageRequest).content

        logger.debug { "Found ${foundStates.size} business partners in ready state" }

        foundStates.groupBy { it.associatedOwnerBpnl }.forEach{ (ownerBpnl, states) ->

            val partners = businessPartnerRepository.findByStageAndAssociatedOwnerBpnlAndExternalIdIn(StageType.Input, ownerBpnl, states.map { it.externalId }, Pageable.unpaged() ).content

            val orchestratorBusinessPartnersDto = partners.map { orchestratorMappings.toOrchestratorDto(it) }

            val createdTasks = createGoldenRecordTasks(TaskMode.UpdateFromSharingMember, orchestratorBusinessPartnersDto)

            val pendingRequests = partners.zip(createdTasks)
                .map { (partner, task) ->
                    SharingStateService.PendingRequest(
                        partner.externalId,
                        task.taskId
                    )
                }

            sharingStateService.setPending(pendingRequests, ownerBpnl)

            logger.info { "Created ${createdTasks.size} new golden record tasks from ready business partners for owner $ownerBpnl" }
        }
    }

    @Transactional
    fun resolvePendingTasks() {
        logger.info { "Started scheduled task to resolve pending golden record tasks" }

        val pageRequest = Pageable.ofSize(properties.check.batchSize)
        val sharingStates = sharingStateRepository.findBySharingStateTypeAndTaskIdNotNull(SharingStateType.Pending, pageRequest).content

        logger.debug { "Found ${sharingStates.size} business partners in pending state" }

        val tasks = orchestrationApiClient.goldenRecordTasks.searchTaskStates(TaskStateRequest(sharingStates.map { it.taskId!! })).tasks


        sharingStates.groupBy { it.associatedOwnerBpnl }.forEach{ (ownerBpnl, states) ->
            resolvePendingTasksForOwner(tasks, states, ownerBpnl)
        }
    }

    private fun createGoldenRecordTasks(mode: TaskMode, orchestratorBusinessPartnersDto: List<BusinessPartner>): List<TaskClientStateDto> {
        if (orchestratorBusinessPartnersDto.isEmpty())
            return emptyList()

        return orchestrationApiClient.goldenRecordTasks.createTasks(TaskCreateRequest(mode, orchestratorBusinessPartnersDto)).createdTasks
    }

    private fun resolvePendingTasksForOwner(allTasks: List<TaskClientStateDto>, sharingStates: List<SharingStateDb>, ownerBpnl: String?){

        val allTasksById = allTasks.associateBy { it.taskId }
        val inputsByExternalId =  businessPartnerRepository.findByStageAndAssociatedOwnerBpnlAndExternalIdIn(StageType.Input, ownerBpnl, sharingStates.map { it.externalId }, Pageable.unpaged())
            .associateBy { it.externalId }

        val (statesWithTask, statesWithoutTask) = sharingStates.map { state -> Pair(state, allTasksById[state.taskId])}.partition { (_, task) -> task != null }
        val (statesWithTaskAndInput, statesWithoutInput) = statesWithTask.map { (state, task) -> Triple(state, task!!, inputsByExternalId[state.externalId]) }.partition { (_, _, input) -> input != null }
        val statesInSuccess = statesWithTaskAndInput.filter { (_,task,_) -> task.processingState.resultState == ResultState.Success }
        val statesInError = statesWithTaskAndInput.filter { (_,task,_) -> task.processingState.resultState == ResultState.Error }

        val (statesWithOutput, statesWithoutOutput) = statesInSuccess.map { (state, task, input) ->
            val roles = input!!.roles.toSortedSet()
            val output =  try{
                orchestratorMappings.toBusinessPartner(task.businessPartnerResult, state.externalId, state.associatedOwnerBpnl, roles)
            }catch (_: Exception){ null }
            Pair(state, output)
        }.partition { (_, output) -> output != null }

        val upsertedPartners = businessPartnerService.upsertBusinessPartnersOutputFromCandidates(statesWithOutput.map { (_, output) -> output!! }, ownerBpnl)

        val statesWithoutTaskErrors = statesWithoutTask.map { (state, _) ->
            SharingStateService.ErrorRequest(
                state.externalId,
                BusinessPartnerSharingError.MissingTaskID,
                errorMessage = "Missing Task in Orchestrator"
            )
        }

        val statesWithoutInputErrors = statesWithoutInput.map { (state, _, _) ->
            SharingStateService.ErrorRequest(
                state.externalId,
                BusinessPartnerSharingError.SharingProcessError,
                errorMessage = "No input data found for the references business partner"
            )
        }

        val statesInErrorErrors = statesInError.map { (state, task, _) ->
            SharingStateService.ErrorRequest(
                state.externalId,
                BusinessPartnerSharingError.SharingProcessError,
                if (task.processingState.errors.isNotEmpty()) task.processingState.errors.joinToString(" // ") { it.description }.take(255) else null
            )
        }

        val statesWithoutOutputError = statesWithoutOutput.map { (state, _) ->
            SharingStateService.ErrorRequest(
                state.externalId,
                BusinessPartnerSharingError.SharingProcessError,
                "Output could not be created from golden record result"
            )
        }

        val allErrors = statesWithoutTaskErrors + statesWithoutInputErrors + statesInErrorErrors + statesWithoutOutputError

        sharingStateService.setError(allErrors, ownerBpnl)

        logger.info { "Resolved ${upsertedPartners.size} tasks as successful and ${allErrors.size} as errors for owner $ownerBpnl" }
    }


}