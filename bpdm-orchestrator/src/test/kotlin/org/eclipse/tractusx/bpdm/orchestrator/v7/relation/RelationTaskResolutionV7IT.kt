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
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.Disabled
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
        val resultEntry = TaskRelationsStepResultEntryDto(reservedTask.taskId, relationResult)
        val resultRequest = TaskRelationsStepResultRequest(createdTask.processingState.step, listOf(resultEntry))
        orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(resultRequest)

        //THEN
        val actualReservationResponse = orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(step = TaskStep.PoolSync))

        val expectedEntry = TaskRelationsStepReservationEntryDto(reservedTask.taskId, reservedTask.recordId, relationResult)
        val expectedResponse = TaskRelationsStepReservationResponse(
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
        val resultEntry = TaskRelationsStepResultEntryDto("NOT EXISTING", relation, emptyList())
        val resultRequest = TaskRelationsStepResultRequest(TaskStep.CleanAndSync, listOf(resultEntry))
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
    @Disabled("ToDo: Possible error behaviour https://github.com/eclipse-tractusx/bpdm/issues/1579")
    fun `try resolve not reserved step task`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)

        //WHEN
        val relation = requestFactory.buildRelation(testName)
        val resultEntry = TaskRelationsStepResultEntryDto(createdTask.taskId, relation, emptyList())
        val resultRequest = TaskRelationsStepResultRequest(createdTask.processingState.step, listOf(resultEntry))
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
        val resultEntry = TaskRelationsStepResultEntryDto(createdTask.taskId, relation, emptyList())
        val resultRequest2 = TaskRelationsStepResultRequest(createdTask.processingState.step, listOf(resultEntry))
        orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(resultRequest2)

        //THEN
        val actualReservationResponse = orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(step = TaskStep.PoolSync))

        val expectedEntry = TaskRelationsStepReservationEntryDto(resultRequest1.taskId, "any UUID", resultRequest1.businessPartnerRelations)
        val expectedResponse = TaskRelationsStepReservationResponse(reservedTasks = listOf(expectedEntry), timeout = Instant.now().plus(resultFactory.pendingTimeout))

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
            .flatMap { orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(10, step = TaskStep.CleanAndSync,
            )).reservedTasks }

        //WHEN
        val requestBody = TaskRelationsStepResultRequest(
            step = TaskStep.CleanAndSync,
            reservedTasks.map { TaskRelationsStepResultEntryDto(it.taskId, it.businessPartnerRelations) }
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
        val resultEntry = TaskRelationsStepResultEntryDto(reservedTask.taskId, businessPartnerResult, emptyList())
        val resultRequest = TaskRelationsStepResultRequest(createdTask.processingState.step, listOf(resultEntry))
        orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(resultRequest)
    }
}