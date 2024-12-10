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

import jakarta.persistence.EntityManager
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.TaskClientStateDto
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRequestEntry
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class TaskCreationBatchService(
    private val taskCreationService: TaskCreationChunkService,
    private val entityManager: EntityManager
){
    private val logger = KotlinLogging.logger { }


    fun createTasksForReadyBusinessPartners(){
        logger.info { "Started scheduled task to create golden record tasks from ready business partners" }

        var totalCreatedTasks = 0
        do {
            val createdTasks = taskCreationService.createTasksForReadyBusinessPartners()
            totalCreatedTasks += createdTasks

            entityManager.clear()
        }while (createdTasks != 0)

        logger.debug { "Total created $totalCreatedTasks new golden record tasks from ready business partners" }

    }
}

@Service
class TaskCreationChunkService(
    private val sharingStateRepository: SharingStateRepository,
    private val sharingStateService: SharingStateService,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val orchestratorMappings: OrchestratorMappings,
    private val orchestrationApiClient: OrchestrationApiClient,
    private val properties: GoldenRecordTaskConfigProperties,
    @Value("\${bpdm.origin-id}")
    private val originId: String
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createTasksForReadyBusinessPartners(): Int {
        logger.info { "Create next chunk of golden record tasks from ready business partners" }

        val pageRequest = Pageable.ofSize(properties.creation.fromSharingMember.batchSize)
        val foundStates = sharingStateRepository.findBySharingStateType(SharingStateType.Ready, pageRequest).content

        logger.debug { "Found ${foundStates.size} business partners in ready state" }

        val foundPartners = businessPartnerRepository.findBySharingStateInAndStage(foundStates, StageType.Input)
        val orchestratorBusinessPartnersDto = foundPartners.map { orchestratorMappings.toCreateRequest(it) }
        val createdTasks = createGoldenRecordTasks(TaskMode.UpdateFromSharingMember, orchestratorBusinessPartnersDto)

        foundPartners.zip(createdTasks).forEach { (partner, task) ->
            if(partner.sharingState.orchestratorRecordId == null) partner.sharingState.orchestratorRecordId = UUID.fromString(task.recordId)
            sharingStateService.setPending(partner.sharingState, task.taskId)
        }

        return createdTasks.size
    }

    private fun createGoldenRecordTasks(mode: TaskMode, orchestratorBusinessPartnersDto: List<TaskCreateRequestEntry>): List<TaskClientStateDto> {
        if (orchestratorBusinessPartnersDto.isEmpty())
            return emptyList()

        return orchestrationApiClient.goldenRecordTasks.createTasks(TaskCreateRequest(mode, orchestratorBusinessPartnersDto, originId)).createdTasks
    }
}