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
import org.eclipse.tractusx.bpdm.gate.entity.SyncTypeDb
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
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
    private val properties: GoldenRecordTaskConfigProperties,
    private val syncRecordService: SyncRecordService,
    private val poolClient: PoolApiClient
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createTasksForReadyBusinessPartners() {
        logger.info { "Started scheduled task to create golden record tasks from ready business partners" }

        val pageRequest = Pageable.ofSize(properties.creation.fromSharingMember.batchSize)
        val foundStates = sharingStateRepository.findBySharingStateType(SharingStateType.Ready, pageRequest).content

        logger.debug { "Found ${foundStates.size} business partners in ready state" }

        val partners = businessPartnerRepository.findByStageAndExternalIdIn(StageType.Input, foundStates.map { it.externalId })

        val orchestratorBusinessPartnersDto = partners.map { orchestratorMappings.toBusinessPartnerGenericDto(it) }

        val createdTasks = createGoldenRecordTasks(TaskMode.UpdateFromSharingMember, orchestratorBusinessPartnersDto)

        val pendingRequests = partners.zip(createdTasks)
            .map { (partner, task) ->
                SharingStateService.PendingRequest(
                    SharingStateService.SharingStateIdentifierDto(
                        partner.externalId,
                        BusinessPartnerType.GENERIC
                    ), task.taskId
                )
            }

        sharingStateService.setPending(pendingRequests)

        logger.info { "Created ${createdTasks.size} new golden record tasks from ready business partners" }
    }

    @Transactional
    fun resolvePendingTasks() {
        logger.info { "Started scheduled task to resolve pending golden record tasks" }

        val pageRequest = Pageable.ofSize(properties.check.batchSize)
        val sharingStates = sharingStateRepository.findBySharingStateTypeAndTaskIdNotNull(SharingStateType.Pending, pageRequest).content

        logger.debug { "Found ${sharingStates.size} business partners in pending state" }

        val tasks = orchestrationApiClient.goldenRecordTasks.searchTaskStates(TaskStateRequest(sharingStates.map { it.taskId!! })).tasks
        val sharingStateMap = sharingStates.associateBy { it.taskId }

        val taskStatesByResult = tasks
            .map { Pair(it, sharingStateMap[it.taskId]!!) }
            .groupBy { (task, _) -> task.processingState.resultState }

        val sharingStatesWithoutTasks = sharingStates.filter { it.taskId !in tasks.map { task -> task.taskId } }

        val businessPartnersToUpsert = taskStatesByResult[ResultState.Success]?.map { (task, sharingState) ->
            orchestratorMappings.toBusinessPartner(task.businessPartnerResult!!, sharingState.externalId,sharingState.associatedOwnerBpnl)
        } ?: emptyList()
        businessPartnerService.upsertBusinessPartnersOutputFromCandidates(businessPartnersToUpsert)

        val errorRequests = (taskStatesByResult[ResultState.Error]?.map { (task, sharingState) ->
            SharingStateService.ErrorRequest(
                SharingStateService.SharingStateIdentifierDto(sharingState.externalId, sharingState.businessPartnerType),
                BusinessPartnerSharingError.SharingProcessError,
                if (task.processingState.errors.isNotEmpty()) task.processingState.errors.joinToString(" // ") { it.description }.take(255) else null
            )
        } ?: emptyList()).toMutableList()

        errorRequests.addAll(sharingStatesWithoutTasks.map { sharingState ->
            SharingStateService.ErrorRequest(
                SharingStateService.SharingStateIdentifierDto(sharingState.externalId, sharingState.businessPartnerType),
                BusinessPartnerSharingError.MissingTaskID,
                errorMessage = "Missing Task in Orchestrator"
            )
        })

        sharingStateService.setError(errorRequests)

        logger.info { "Resolved ${businessPartnersToUpsert.size} tasks as successful and ${errorRequests.size} as errors" }
    }

    @Transactional
    fun createTasksForGoldenRecordUpdates() {
        logger.info { "Started scheduled task to create golden record tasks from Pool updates" }

        val syncRecord = syncRecordService.getOrCreateRecord(SyncTypeDb.POOL_TO_GATE_OUTPUT)

        val pageRequest = PaginationRequest(0, properties.creation.fromPool.batchSize)
        val changelogSearchRequest = ChangelogSearchRequest(syncRecord.finishedAt)
        val poolChangelogEntries = poolClient.changelogs.getChangelogEntries(changelogSearchRequest, pageRequest)

        val poolUpdatedEntries = poolChangelogEntries.content.filter { it.changelogType == ChangelogType.UPDATE }

        val bpnA =
            poolUpdatedEntries.filter { it.businessPartnerType == BusinessPartnerType.ADDRESS }.map { it.bpn }
        val bpnL = poolUpdatedEntries.filter { it.businessPartnerType == BusinessPartnerType.LEGAL_ENTITY }
            .map { it.bpn }
        val bpnS =
            poolUpdatedEntries.filter { it.businessPartnerType == BusinessPartnerType.SITE }.map { it.bpn }

        val gateOutputEntries = businessPartnerRepository.findByStageAndBpnLInOrBpnSInOrBpnAIn(StageType.Output, bpnL, bpnS, bpnA)

        val businessPartnerGenericDtoList = gateOutputEntries.map { bp ->
            orchestratorMappings.toBusinessPartnerGenericDto(bp)
        }

        val tasks = createGoldenRecordTasks(TaskMode.UpdateFromPool, businessPartnerGenericDtoList)

        val pendingRequests = gateOutputEntries.zip(tasks)
            .map { (partner, task) ->
                SharingStateService.PendingRequest(
                    SharingStateService.SharingStateIdentifierDto(
                        partner.externalId,
                        partner.parentType ?: BusinessPartnerType.GENERIC
                    ), task.taskId
                )
            }
        sharingStateService.setPending(pendingRequests)

        if (poolUpdatedEntries.isNotEmpty()) {
            syncRecordService.setSynchronizationStart(SyncTypeDb.POOL_TO_GATE_OUTPUT)
            syncRecordService.setSynchronizationSuccess(SyncTypeDb.POOL_TO_GATE_OUTPUT, poolUpdatedEntries.last().timestamp)
        }

        logger.info { "Created ${tasks.size} new golden record tasks from pool updates" }
    }

    private fun createGoldenRecordTasks(mode: TaskMode, orchestratorBusinessPartnersDto: List<BusinessPartnerGenericDto>): List<TaskClientStateDto> {
        if (orchestratorBusinessPartnersDto.isEmpty())
            return emptyList()

        return orchestrationApiClient.goldenRecordTasks.createTasks(TaskCreateRequest(mode, orchestratorBusinessPartnersDto)).createdTasks
    }
}