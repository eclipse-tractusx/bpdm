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
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.BusinessPartnerTestDataFactory
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import java.time.Instant

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "bpdm.security.enabled=false"
    ]
)
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class GoldenRecordTaskEventControllerIT @Autowired constructor(
    private val orchestratorClient: OrchestrationApiClient,
    private val dbTestHelpers: DbTestHelpers
){

    private val testDataFactory = BusinessPartnerTestDataFactory()
    private val defaultBusinessPartner1 = testDataFactory.createFullBusinessPartner("BP1")
    private val defaultBusinessPartner2 = testDataFactory.createFullBusinessPartner("BP2")


    @BeforeEach
    fun cleanUp() {
        dbTestHelpers.truncateDbTables()
    }

    /*
    GIVEN no existing tasks
    WHEN requesting finished tasks event
    THEN empty
     */
    @Test
    fun `get empty finished task events no tasks`(){
        val events = orchestratorClient.finishedTaskEvents.getEvents(Instant.ofEpochSecond(0), PaginationRequest()).content
        assertThat(events).isEmpty()
    }

    /*
   GIVEN no finished tasks
   WHEN requesting finished tasks event
   THEN empty
    */
    @Test
    fun `get empty finished task events no finished tasks`(){
        orchestratorClient.goldenRecordTasks.createTasks(TaskCreateRequest(TaskMode.UpdateFromSharingMember,
            listOf(
                TaskCreateRequestEntry(null, defaultBusinessPartner1),
                TaskCreateRequestEntry(null, defaultBusinessPartner2)

            )
        )).createdTasks


        val events = orchestratorClient.finishedTaskEvents.getEvents(Instant.ofEpochSecond(0), PaginationRequest()).content
        assertThat(events).isEmpty()
    }

    /*
  GIVEN finished tasks
  WHEN requesting finished tasks event
  THEN finished task events
   */
    @Test
    fun `get finished task events`(){
        val finishedTaskIds = createFinishedTasks(3)

        val events = orchestratorClient.finishedTaskEvents.getEvents(Instant.ofEpochSecond(0), PaginationRequest()).content
        assertThat(events.map { it.taskId }).isEqualTo(finishedTaskIds)
    }

    /*
    GIVEN finished tasks
    WHEN requesting finished tasks after time
    THEN return only events after time
    */
    @Test
    fun `filter finished task events by timestamp`(){
        createFinishedTasks(3)

        val timeAfterFirstFinish = Instant.now()

        val finishedTaskIds = createFinishedTasks(3)


        val events = orchestratorClient.finishedTaskEvents.getEvents(timeAfterFirstFinish, PaginationRequest()).content
        assertThat(events.map { it.taskId }).containsExactly(*finishedTaskIds.toTypedArray())
    }

    /*
    GIVEN finished tasks
    WHEN requesting paginated finished tasks
    THEN return paginated
    */
    @Test
    fun `get paginated finished task events`(){
        val taskIds = createFinishedTasks(6)

        val eventPage1 = orchestratorClient.finishedTaskEvents.getEvents(Instant.ofEpochSecond(0), PaginationRequest(0, 3)).content
        val eventPage2 = orchestratorClient.finishedTaskEvents.getEvents(Instant.ofEpochSecond(0), PaginationRequest(1, 3)).content
        val eventPage3 = orchestratorClient.finishedTaskEvents.getEvents(Instant.ofEpochSecond(0), PaginationRequest(2, 3)).content

        assertThat(eventPage1.size).isEqualTo(3)
        assertThat(eventPage1.map { it.taskId }).isSubsetOf(*taskIds.toTypedArray())

        assertThat(eventPage2.size).isEqualTo(3)
        assertThat(eventPage2.map { it.taskId }).isSubsetOf(*taskIds.toTypedArray())

        assertThat(eventPage3).isEmpty()

        assertThat(eventPage1.plus(eventPage2).map { it.taskId }).containsExactly(*taskIds.toTypedArray())
    }

    /*
       GIVEN finished tasks
       WHEN requesting paginated finished tasks after timestamp
       THEN return paginated events after time
   */
    @Test
    fun `filter paginated finished task events`(){
        createFinishedTasks(3)

        val timeAfterFirstFinish = Instant.now()

        val taskIds = createFinishedTasks(6)

        val eventPage1 = orchestratorClient.finishedTaskEvents.getEvents(timeAfterFirstFinish, PaginationRequest(0, 3)).content
        val eventPage2 = orchestratorClient.finishedTaskEvents.getEvents(timeAfterFirstFinish, PaginationRequest(1, 3)).content
        val eventPage3 = orchestratorClient.finishedTaskEvents.getEvents(timeAfterFirstFinish, PaginationRequest(2, 3)).content

        assertThat(eventPage1.size).isEqualTo(3)
        assertThat(eventPage1.map { it.taskId }).isSubsetOf(*taskIds.toTypedArray())

        assertThat(eventPage2.size).isEqualTo(3)
        assertThat(eventPage2.map { it.taskId }).isSubsetOf(*taskIds.toTypedArray())

        assertThat(eventPage3).isEmpty()

        assertThat(eventPage1.plus(eventPage2).map { it.taskId }).containsExactly(*taskIds.toTypedArray())

    }


    fun createFinishedTasks(count: Int): List<String>{
        val createdTasks = orchestratorClient.goldenRecordTasks.createTasks(TaskCreateRequest(TaskMode.UpdateFromSharingMember,
            (1 .. count).map { TaskCreateRequestEntry(null, testDataFactory.createFullBusinessPartner(it.toString())) })
        ).createdTasks

        orchestratorClient.goldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(count, TaskStep.CleanAndSync))
        orchestratorClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest( TaskStep.CleanAndSync,
            createdTasks.map { TaskStepResultEntryDto(it.taskId, it.businessPartnerResult) })
        )

        orchestratorClient.goldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(count, TaskStep.PoolSync))
        orchestratorClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest( TaskStep.PoolSync,
            createdTasks.map { TaskStepResultEntryDto(it.taskId, it.businessPartnerResult) })
        )

        return createdTasks.map { it.taskId }
    }




}