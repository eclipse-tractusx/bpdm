/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.config

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.test.containers.OrchestratorMockConfiguration
import org.eclipse.tractusx.bpdm.test.containers.OrchestratorMockContextInitializer
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartner
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.BusinessPartnerRelations
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskErrorDto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskErrorType
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsErrorDto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsErrorType
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepReservationEntryDto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepResultEntryDto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationEntryDto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepResultEntryDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class],
    properties = ["bpdm.tasks.cron=-", "bpdm.sharingMemberRecords.cron=-"]
)
@ActiveProfiles("test-no-auth")
@Import(OrchestratorMockConfiguration::class)
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OrchestratorMockContextInitializer::class])
class GoldenRecordTaskSchedulerTest @Autowired constructor(
    val goldenRecordTaskScheduler: GoldenRecordTaskScheduler,
    val orchestratorMockDataFactory: OrchestratorMockDataFactory,
    val dbTestHelpers: DbTestHelpers
) {

    @BeforeEach
    fun beforeEach() {
        dbTestHelpers.truncateDbTables()
    }

    @Test
    fun `golden record task whose result did not reach the Orchestrator is reported as error in the next run`() {
        orchestratorMockDataFactory.mockReservedTasks(listOf(TaskStepReservationEntryDto("TASK_1", "RECORD_1", BusinessPartner.empty)))
        orchestratorMockDataFactory.mockStepResultsResponse(500)
        goldenRecordTaskScheduler.processTasks()

        orchestratorMockDataFactory.mockStepResultsResponse(200)
        orchestratorMockDataFactory.resetRecordedRequests()
        goldenRecordTaskScheduler.processTasks()

        assertThat(orchestratorMockDataFactory.getStepResults()).containsExactly(
            TaskStepResultEntryDto("TASK_1", BusinessPartner.empty, listOf(TaskErrorDto(TaskErrorType.Unspecified, UNREPORTED_TASK_ERROR)))
        )
        assertThat(orchestratorMockDataFactory.getRelationStepResults()).isEmpty()
    }

    @Test
    fun `relation task whose result did not reach the Orchestrator is reported as error in the next run`() {
        orchestratorMockDataFactory.mockReservedRelationTasks(listOf(TaskRelationsStepReservationEntryDto("TASK_1", "RECORD_1", BusinessPartnerRelations.empty)))
        orchestratorMockDataFactory.mockRelationStepResultsResponse(500)
        goldenRecordTaskScheduler.processTasks()

        orchestratorMockDataFactory.mockRelationStepResultsResponse(200)
        orchestratorMockDataFactory.resetRecordedRequests()
        goldenRecordTaskScheduler.processTasks()

        assertThat(orchestratorMockDataFactory.getRelationStepResults()).containsExactly(
            TaskRelationsStepResultEntryDto("TASK_1", BusinessPartnerRelations.empty, listOf(TaskRelationsErrorDto(TaskRelationsErrorType.Unspecified, UNREPORTED_TASK_ERROR)))
        )
        assertThat(orchestratorMockDataFactory.getStepResults()).isEmpty()
    }

    @Test
    fun `task reported as error is not reported again`() {
        orchestratorMockDataFactory.mockReservedTasks(listOf(TaskStepReservationEntryDto("TASK_1", "RECORD_1", BusinessPartner.empty)))
        orchestratorMockDataFactory.mockStepResultsResponse(500)
        goldenRecordTaskScheduler.processTasks()
        orchestratorMockDataFactory.mockStepResultsResponse(200)
        goldenRecordTaskScheduler.processTasks()

        orchestratorMockDataFactory.resetRecordedRequests()
        goldenRecordTaskScheduler.processTasks()

        assertThat(orchestratorMockDataFactory.getStepResults()).isEmpty()
    }

    @Test
    fun `task unknown to the Orchestrator is not reported again`() {
        orchestratorMockDataFactory.mockReservedTasks(listOf(TaskStepReservationEntryDto("TASK_1", "RECORD_1", BusinessPartner.empty)))
        orchestratorMockDataFactory.mockStepResultsResponse(500)
        goldenRecordTaskScheduler.processTasks()
        orchestratorMockDataFactory.mockStepResultsResponse(400)
        goldenRecordTaskScheduler.processTasks()

        orchestratorMockDataFactory.mockStepResultsResponse(200)
        orchestratorMockDataFactory.resetRecordedRequests()
        goldenRecordTaskScheduler.processTasks()

        assertThat(orchestratorMockDataFactory.getStepResults()).isEmpty()
    }

    @Test
    fun `task whose error report failed is reported again in the next run`() {
        orchestratorMockDataFactory.mockReservedTasks(listOf(TaskStepReservationEntryDto("TASK_1", "RECORD_1", BusinessPartner.empty)))
        orchestratorMockDataFactory.mockStepResultsResponse(500)
        goldenRecordTaskScheduler.processTasks()
        goldenRecordTaskScheduler.processTasks()

        orchestratorMockDataFactory.mockStepResultsResponse(200)
        orchestratorMockDataFactory.resetRecordedRequests()
        goldenRecordTaskScheduler.processTasks()

        assertThat(orchestratorMockDataFactory.getStepResults().map { it.taskId }).containsExactly("TASK_1")
    }

    @Test
    fun `tracked task is reported even when reserving golden record tasks fails`() {
        orchestratorMockDataFactory.mockReservedTasks(listOf(TaskStepReservationEntryDto("TASK_1", "RECORD_1", BusinessPartner.empty)))
        orchestratorMockDataFactory.mockStepResultsResponse(500)
        goldenRecordTaskScheduler.processTasks()

        orchestratorMockDataFactory.mockStepReservationsResponse(500)
        orchestratorMockDataFactory.mockStepResultsResponse(200)
        orchestratorMockDataFactory.resetRecordedRequests()
        goldenRecordTaskScheduler.processTasks()

        assertThat(orchestratorMockDataFactory.getStepResults().map { it.taskId }).containsExactly("TASK_1")
    }

    companion object {
        private const val UNREPORTED_TASK_ERROR = "Unknown golden record process error during Pool communication"
    }
}
