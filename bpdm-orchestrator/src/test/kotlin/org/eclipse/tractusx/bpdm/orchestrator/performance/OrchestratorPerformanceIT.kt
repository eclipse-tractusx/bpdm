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

package org.eclipse.tractusx.bpdm.orchestrator.performance

import org.eclipse.tractusx.bpdm.orchestrator.config.StateMachineConfigProperties
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.BusinessPartnerTestDataFactory
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import java.time.Duration
import java.time.Instant
import java.util.*

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "bpdm.security.enabled=false",
        "bpdm.task.timeoutCheckCron=-"
    ]
)
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class OrchestratorPerformanceIT@Autowired constructor(
    private val orchestratorClient: OrchestrationApiClient,
    private val stateMachineConfigProperties: StateMachineConfigProperties
) {

    private val testDataFactory = BusinessPartnerTestDataFactory()
    private val taskMode = stateMachineConfigProperties.modeSteps.keys.first()
    private val step = stateMachineConfigProperties.modeSteps[taskMode]!!.first()

    @ParameterizedTest
    @ValueSource(ints = [100])
    fun `measure insert, search, reserve and update` (totalSize: Int){
        val batchSize = 100
        var count = 0
        var insertNew = 0L
        var insertUpdate = 0L
        var search = 0L
        var reserve = 0L
        var resolve = 0L

        //make orchestrator access database to initialize connection
        //do it before measurements
        orchestratorClient.goldenRecordTasks.searchTaskResultStates(TaskResultStateSearchRequest(listOf(UUID.randomUUID().toString())))

        do {
            val measurements = measurePerformance(batchSize)
            insertNew += measurements.insertNew
            insertUpdate += measurements.insertUpdate
            search += measurements.search
            reserve += measurements.reserve
            resolve += measurements.resolve
            count += batchSize
        }while (count < totalSize)

        println("Performance result for $totalSize records: ")
        println("Insert new tasks: ${Duration.ofNanos(insertNew)}")
        println("Update existing task record: ${Duration.ofNanos(insertUpdate)}")
        println("Search tasks: ${Duration.ofNanos(search)}")
        println("Reserve tasks: ${Duration.ofNanos(reserve)}")
        println("Resolve tasks: ${Duration.ofNanos(resolve)}")
    }

    private fun measurePerformance(batchSize: Int): PerformanceResult{
        val beforeCreate = Instant.now()
        val createdTasks = createNewRecordTasks(batchSize)
        val beforeUpdate = Instant.now()
        val updatedTasks = updateRecords(createdTasks.map { it.recordId })
        val beforeSearch = Instant.now()
        searchTasks(updatedTasks)
        val beforeReserve = Instant.now()
        val reservedTasks = reserveTasks(batchSize)
        val beforeResolve = Instant.now()
        resolveTasks(reservedTasks)

        return PerformanceResult(
            insertNew = Duration.between(beforeCreate, beforeUpdate).toNanos(),
            insertUpdate = Duration.between(beforeUpdate, beforeSearch).toNanos(),
            search = Duration.between(beforeSearch, beforeReserve).toNanos(),
            reserve = Duration.between(beforeReserve, beforeResolve).toNanos(),
            resolve = Duration.between(beforeResolve, Instant.now()).toNanos()
        )
    }

    private fun createNewRecordTasks(size: Int): List<TaskStateRequest.Entry>{
        return (1 .. size)
            .map { TaskCreateRequestEntry(null, testDataFactory.createFullBusinessPartner("BP$it")) }
            .let { TaskCreateRequest(mode = taskMode, requests = it) }
            .let {  orchestratorClient.goldenRecordTasks.createTasks(it).createdTasks }
            .map { TaskStateRequest.Entry(it.taskId, it.recordId) }
    }

    private fun updateRecords(recordIds: List<String>): List<TaskStateRequest.Entry>{
        return recordIds
            .map { TaskCreateRequestEntry(it, testDataFactory.createFullBusinessPartner(it)) }
            .let { TaskCreateRequest(mode = taskMode, requests = it) }
            .let {  orchestratorClient.goldenRecordTasks.createTasks(it).createdTasks }
            .map { TaskStateRequest.Entry(it.taskId, it.recordId) }
    }

    private fun searchTasks(identities: List<TaskStateRequest.Entry>){
        orchestratorClient.goldenRecordTasks.searchTaskStates(TaskStateRequest(identities))
    }

    private fun reserveTasks(size: Int): List<String>{
        return TaskStepReservationRequest(size, step)
            .let { orchestratorClient.goldenRecordTasks.reserveTasksForStep(it).reservedTasks }
            .map { it.taskId }
    }

    private fun resolveTasks(taskIds: List<String>){
        taskIds
            .map { TaskStepResultEntryDto(it, testDataFactory.createFullBusinessPartner("Resolved $it")) }
            .run { orchestratorClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest(step, this)) }
    }

    data class PerformanceResult(
        val insertNew: Long,
        val insertUpdate: Long,
        val search: Long,
        val reserve: Long,
        val resolve: Long
    )

}