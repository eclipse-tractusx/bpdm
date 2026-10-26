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

package org.eclipse.tractusx.bpdm.gate.service

import jakarta.persistence.EntityManager
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.util.joinIdentifiersForLog
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskResolutionBatchService(
    private val taskResolutionService: TaskResolutionChunkService,
    private val entityManager: EntityManager
){
    private val logger = KotlinLogging.logger { }

    fun healthCheck(){
        logger.debug { "Start process Task Health Check..." }

        val totalTaskMissing = mutableListOf<String>()
        var totalProcessed = 0
        var pageToQuery = 0
        do{
            val result = taskResolutionService.healthCheck(pageToQuery)
            totalProcessed += result.processed
            totalTaskMissing.addAll(result.missing)
            pageToQuery++

            entityManager.clear()
        }while (result.hasNextPage)

        if (totalTaskMissing.isNotEmpty())
            logger.info {
                "Task health check: processed $totalProcessed tasks, ${totalTaskMissing.size} of them unhealthy: ${totalTaskMissing.joinIdentifiersForLog()}"
            }
        else
            logger.debug { "Task health check: processed $totalProcessed tasks, none unhealthy" }
    }
}

@Service
class TaskResolutionChunkService(
    private val sharingStateRepository: SharingStateRepository,
    private val sharingStateService: SharingStateService,
    private val taskProperties: GoldenRecordTaskConfigProperties,
    private val orchestrationApiClient: OrchestrationApiClient
) {

    @Transactional
    fun healthCheck(pageToQuery: Int): HealthCheckResult{
        val pendingSharingStatePage = sharingStateRepository.findBySharingStateType(SharingStateType.Pending, PageRequest.of(pageToQuery, taskProperties.healthCheck.batchSize))
        val pendingSharingStates = pendingSharingStatePage.content
        val pendingTaskIds = pendingSharingStates.map {  it.taskId.toString() }

        val resultStates = orchestrationApiClient.goldenRecordTasks.searchTaskResultStates(TaskResultStateSearchRequest(pendingTaskIds)).resultStates

        val sharingStatesMissingTask = pendingSharingStates.zip(resultStates)
            .filter { (_, resultState) -> resultState == null }
            .map { (sharingState, _) -> sharingState }

        sharingStatesMissingTask.forEach { sharingStateService.setError(it, BusinessPartnerSharingError.MissingTaskID, "Missing Task in Orchestrator") }

        return HealthCheckResult(pendingSharingStates.size, sharingStatesMissingTask.map { it.externalId }, pendingSharingStatePage.hasNext())
    }


    data class HealthCheckResult(
        val processed: Int,
        val missing: List<String>,
        val hasNextPage: Boolean
    )

}