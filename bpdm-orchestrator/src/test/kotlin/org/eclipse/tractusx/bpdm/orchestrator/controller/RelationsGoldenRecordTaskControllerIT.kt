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

package org.eclipse.tractusx.bpdm.orchestrator.controller

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "bpdm.security.enabled=false",
        "bpdm.api.upsert-limit=3",
        "bpdm.task.timeoutCheckCron=* * * * * ?",       // check every sec
        "bpdm.task.taskPendingTimeout=3s",
        "bpdm.task.taskRetentionTimeout=5s"
    ]
)
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class RelationsGoldenRecordTaskControllerIT @Autowired constructor(
    private val orchestratorClient: OrchestrationApiClient,
    private val dbTestHelpers: DbTestHelpers,
) {

    private val defaultRelations1 = BusinessPartnerRelations(relationType = RelationType.IsAlternativeHeadquarterFor, businessPartnerSourceBpnl = "BPNL1", businessPartnerTargetBpnl = "BPNL2")
    private val defaultRelations2 = BusinessPartnerRelations(relationType = RelationType.IsManagedBy, businessPartnerSourceBpnl = "BPNL3", businessPartnerTargetBpnl = "BPNL4")

    @BeforeEach
    fun cleanUp() {
        dbTestHelpers.truncateDbTables()
    }

    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `create relations golden record task for existing gate record`(taskMode: TaskMode){
        //Create records by creating tasks first for now and there is scope for improvement once bpdm-gate sync relations logic is in place
        val existingRecordIds = createTasksWithoutRecordId(taskMode).createdTasks.map { it.recordId }

        val requestsWithRecords = listOf(defaultRelations1, defaultRelations2)
            .zip(existingRecordIds)
            .map { (bpr, recordId) -> TaskCreateRelationsRequestEntry(recordId, bpr) }

        requestsWithRecords.forEach { assertThat(it.recordId).isNotNull() }

        val tasksWithRecords = orchestratorClient.relationsGoldenRecordTasks.createTasks(TaskCreateRelationsRequest(taskMode, requestsWithRecords)).createdTasks

        tasksWithRecords.zip(existingRecordIds).forEach { (actualTask, expectedRecordId) -> assertThat(actualTask.recordId).isEqualTo(expectedRecordId) }
    }


    @ParameterizedTest
    @EnumSource(TaskMode::class)
    fun `abort relations golden record task when outdated`(taskMode: TaskMode){
        val createdTasks  = createTasks(mode = taskMode, entries = listOf(defaultRelations1, defaultRelations2).map { TaskCreateRelationsRequestEntry(null, it) }).createdTasks
        val createdTaskIds = createdTasks.map { it.toTaskSearchIdentity() }
        val createdRecordIds = createdTasks.map { it.recordId }

        //Create newer tasks for the given records
        createTasks(mode = taskMode, entries = createdRecordIds
            .zip(listOf(defaultRelations1, defaultRelations2))
            .map { (recordId, bp) -> TaskCreateRelationsRequestEntry(recordId, bp) }
        ).createdTasks

        val olderTasks = searchTaskStates(createdTaskIds).tasks

        olderTasks.forEach { assertThat(it.processingState.resultState).isEqualTo(ResultState.Error) }
    }

    private fun createTasksWithoutRecordId(mode: TaskMode, businessPartnersRelations: List<BusinessPartnerRelations>? = null): TaskCreateRelationsResponse =
        createTasks(mode, (businessPartnersRelations ?: listOf(defaultRelations1, defaultRelations2)).map{ bpr -> TaskCreateRelationsRequestEntry(null, bpr) })

    private fun createTasks(mode: TaskMode,
                            entries: List<TaskCreateRelationsRequestEntry>? = null
    ): TaskCreateRelationsResponse{
        val resolvedEntries = entries ?: listOf(defaultRelations1, defaultRelations2).map { bpr -> TaskCreateRelationsRequestEntry(null, bpr) }
        return orchestratorClient.relationsGoldenRecordTasks.createTasks(TaskCreateRelationsRequest(mode = mode, requests = resolvedEntries))
    }

    private fun TaskClientRelationsStateDto.toTaskSearchIdentity() = TaskStateRequest.Entry(taskId, recordId)

    private fun searchTaskStates(taskIds: List<TaskStateRequest.Entry>) =
        orchestratorClient.relationsGoldenRecordTasks.searchTaskStates(
            TaskStateRequest(taskIds)
        )

}