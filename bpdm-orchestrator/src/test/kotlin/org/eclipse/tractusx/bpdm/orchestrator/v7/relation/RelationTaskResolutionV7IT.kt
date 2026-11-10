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

package org.eclipse.tractusx.bpdm.orchestrator.v7.relation

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.orchestrator.v7.UnscheduledOrchestratorTestBaseV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepReservationEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepReservationV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepResultEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepResultV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationV7Request
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

class RelationTaskResolutionV7IT: UnscheduledOrchestratorTestBaseV7() {

    /**
     * GIVEN reserved relation task
     * WHEN user resolves task as legal entity
     * THEN user sees resolved task data in next step
     */
    @Test
    fun `resolve reserved relation`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)
        val reservedTask = testDataClient.reserveRelationTask(createdTask)

        //WHEN
        val relationResult = requestFactory.buildRelation("Result $testName")
        val resultEntry = TaskRelationsStepResultEntryV7Dto(reservedTask.taskId, relationResult)
        val resultRequest = TaskRelationsStepResultV7Request(createdTask.processingState.step, listOf(resultEntry))
        orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(resultRequest)

        //THEN
        val actualReservationResponse = orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(TaskStepReservationV7Request(step = TaskStepV7.PoolSync))

        val expectedEntry = TaskRelationsStepReservationEntryV7Dto(reservedTask.taskId, reservedTask.recordId, relationResult)
        val expectedResponse = TaskRelationsStepReservationV7Response(
            reservedTasks = listOf(expectedEntry),
            timeout = Instant.now().plus(resultFactory.pendingTimeout)
        )

        assertRepo.assertRelationTaskReservationResponseEqual(actualReservationResponse, expectedResponse, ignoreRecordId = false)
    }


    /**
     * WHEN user tries to resolve not existing relation task
     * THEN user sees HTTP BAD REQUEST error
     */
    @Test
    fun `try resolve not existing task`(){
        //WHEN
        val relation = requestFactory.buildRelation(testName)
        val resultEntry = TaskRelationsStepResultEntryV7Dto("NOT EXISTING", relation, emptyList())
        val resultRequest = TaskRelationsStepResultV7Request(TaskStepV7.CleanAndSync, listOf(resultEntry))
        val request: () -> Unit = { orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(resultRequest) }

        //THEN
        Assertions.assertThatThrownBy(request).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN relation task not reserved in step
     * WHEN user tries to resolve task for step
     * THEN user sees HTTP BAD REQUEST error
     *
     */
    @Test
    fun `try resolve not reserved step task`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)

        //WHEN
        val relation = requestFactory.buildRelation(testName)
        val resultEntry = TaskRelationsStepResultEntryV7Dto(createdTask.taskId, relation, emptyList())
        val resultRequest = TaskRelationsStepResultV7Request(createdTask.processingState.step, listOf(resultEntry))
        val request: () -> Unit = { orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(resultRequest) }

        //THEN
        Assertions.assertThatThrownBy(request).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN relation task has been resolved for step
     * WHEN user tries to resolve task for step again
     * THEN request accepted but result ignored
     *
     */
    @Test
    fun `resolve task again`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)
        val resultRequest1 = testDataClient.reserveAndResolveRelationTask(createdTask, "Resolved $testName")

        //WHEN
        val relation = requestFactory.buildRelation(testName)
        val resultEntry = TaskRelationsStepResultEntryV7Dto(createdTask.taskId, relation, emptyList())
        val resultRequest2 = TaskRelationsStepResultV7Request(createdTask.processingState.step, listOf(resultEntry))
        orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(resultRequest2)

        //THEN
        val actualReservationResponse = orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(TaskStepReservationV7Request(step = TaskStepV7.PoolSync))

        val expectedEntry = TaskRelationsStepReservationEntryV7Dto(resultRequest1.taskId, "any UUID", resultRequest1.businessPartnerRelations)
        val expectedResponse = TaskRelationsStepReservationV7Response(reservedTasks = listOf(expectedEntry), timeout = Instant.now().plus(resultFactory.pendingTimeout))

        assertRepo.assertRelationTaskReservationResponseEqual(actualReservationResponse, expectedResponse, ignoreRecordId = true)
    }

    /**
     * GIVEN relation tasks to resolve
     * WHEN posting too many cleaning results (over the upsert limit)
     * THEN show 400 BAD REQUEST error
     */
    @Test
    fun `expect exception on posting too many relation task results`() {
        //GIVEN
        (1 .. 11).forEach { testDataClient.createRelationTask("$testName $it") }

        val reservedTasks = (1 .. 2)
            .flatMap { orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(TaskStepReservationV7Request(10, step = TaskStepV7.CleanAndSync,
            )).reservedTasks }

        //WHEN
        val requestBody = TaskRelationsStepResultV7Request(
            step = TaskStepV7.CleanAndSync,
            reservedTasks.map { TaskRelationsStepResultEntryV7Dto(it.taskId, it.businessPartnerRelations) }
        )
        val request: () -> Unit = { orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(requestBody) }

        //THEN
        Assertions.assertThatThrownBy(request).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN aborted relation task
     * WHEN user tries to resolve task
     * THEN user sees 200 OK response
     */
    @Test
    fun `aborted relation task throws no error when trying to resolve`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)
        val reservedTask = testDataClient.reserveRelationTask(createdTask)
        testDataClient.createBusinessPartnerTask(testName, recordId = createdTask.recordId)

        //WHEN
        val businessPartnerResult = requestFactory.buildRelation("result $testName")
        val resultEntry = TaskRelationsStepResultEntryV7Dto(reservedTask.taskId, businessPartnerResult, emptyList())
        val resultRequest = TaskRelationsStepResultV7Request(createdTask.processingState.step, listOf(resultEntry))
        orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(resultRequest)
    }

    /**
     * GIVEN reserved relation task
     * WHEN user resolves task with invalid relation data
     * THEN user sees bad request
     */
    @Test
    fun `reject invalid relation result`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)
        val reservedTask = testDataClient.reserveRelationTask(createdTask)

        //WHEN
        val invalidRelation = createdTask.businessPartnerRelationsResult.copy(
            businessPartnerSourceBpn = "",
            businessPartnerTargetBpn = ""
        )
        val resultEntry = TaskRelationsStepResultEntryV7Dto(reservedTask.taskId, invalidRelation)
        val resultRequest = TaskRelationsStepResultV7Request(createdTask.processingState.step, listOf(resultEntry))

        //THEN
        Assertions.assertThatThrownBy { orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(resultRequest) }
            .isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN reserved relation task with existing validation errors
     * WHEN user resolves task with invalid relation data
     * THEN user still sees bad request
     */
    @Test
    fun `reject invalid relation result even with errors`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)
        val reservedTask = testDataClient.reserveRelationTask(createdTask)

        //WHEN
        val invalidRelation = createdTask.businessPartnerRelationsResult.copy(
            businessPartnerSourceBpn = "",
            businessPartnerTargetBpn = ""
        )
        val resultEntry = TaskRelationsStepResultEntryV7Dto(reservedTask.taskId, invalidRelation, emptyList())
        val resultRequest = TaskRelationsStepResultV7Request(createdTask.processingState.step, listOf(resultEntry))

        //THEN
        Assertions.assertThatThrownBy { orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(resultRequest) }
            .isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }
}