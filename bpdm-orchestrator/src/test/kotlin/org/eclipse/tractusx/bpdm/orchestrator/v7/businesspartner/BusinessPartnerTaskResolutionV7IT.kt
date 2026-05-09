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

package org.eclipse.tractusx.bpdm.orchestrator.v7.businesspartner

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.orchestrator.v7.UnscheduledOrchestratorTestBaseV7
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

class BusinessPartnerTaskResolutionV7IT: UnscheduledOrchestratorTestBaseV7() {
    /**
     * GIVEN reserved task
     * WHEN user resolves task as legal entity
     * THEN user sees resolved task data in next step
     */
    @Test
    fun `resolve reserved task as legal entity`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)
        val reservedTask = testDataClient.reserveBusinessPartnerTasks(createdTask.processingState.step).reservedTasks.single()

        //WHEN
        val businessPartnerResult = requestFactory.buildLegalEntityBusinessPartner("result $testName")
        val resultEntry = TaskStepResultEntryDto(reservedTask.taskId, businessPartnerResult, emptyList())
        val resultRequest = TaskStepResultRequest(createdTask.processingState.step, listOf(resultEntry))
        orchestratorClient.goldenRecordTasks.resolveStepResults(resultRequest)

        //THEN
        val actualReservationResponse = orchestratorClient.goldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(step = TaskStep.PoolSync))

        val expectedEntry = TaskStepReservationEntryDto(reservedTask.taskId, reservedTask.recordId, businessPartnerResult)
        val expectedResponse = TaskStepReservationResponse(reservedTasks = listOf(expectedEntry), timeout = Instant.now().plus(resultFactory.pendingTimeout))

        assertRepo.assertBusinessPartnerTaskReservationResponseEqual(actualReservationResponse, expectedResponse, ignoreRecordId = false)
    }

    /**
     * GIVEN reserved task
     * WHEN user resolves task as site
     * THEN user sees resolved task data in next step
     */
    @Test
    fun `resolve reserved task as site`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)
        val reservedTask = testDataClient.reserveBusinessPartnerTasks(createdTask.processingState.step).reservedTasks.single()

        //WHEN
        val businessPartnerResult = requestFactory.buildSiteBusinessPartner("result $testName")
        val resultEntry = TaskStepResultEntryDto(reservedTask.taskId, businessPartnerResult, emptyList())
        val resultRequest = TaskStepResultRequest(createdTask.processingState.step, listOf(resultEntry))
        orchestratorClient.goldenRecordTasks.resolveStepResults(resultRequest)

        //THEN
        val actualReservationResponse = orchestratorClient.goldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(step = TaskStep.PoolSync))

        val expectedEntry = TaskStepReservationEntryDto(reservedTask.taskId, reservedTask.recordId, businessPartnerResult)
        val expectedResponse = TaskStepReservationResponse(reservedTasks = listOf(expectedEntry), timeout = Instant.now().plus(resultFactory.pendingTimeout))

        assertRepo.assertBusinessPartnerTaskReservationResponseEqual(actualReservationResponse, expectedResponse, ignoreRecordId = false)
    }

    /**
     * GIVEN reserved task
     * WHEN user resolves task as legal address site
     * THEN user sees resolved task data in next step
     */
    @Test
    fun `resolve reserved task as legal address site`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)
        val reservedTask = testDataClient.reserveBusinessPartnerTasks(createdTask.processingState.step).reservedTasks.single()

        //WHEN
        val businessPartnerResult = requestFactory.buildLegalAddressSiteBusinessPartner("result $testName")
        val resultEntry = TaskStepResultEntryDto(reservedTask.taskId, businessPartnerResult, emptyList())
        val resultRequest = TaskStepResultRequest(createdTask.processingState.step, listOf(resultEntry))
        orchestratorClient.goldenRecordTasks.resolveStepResults(resultRequest)

        //THEN
        val actualReservationResponse = orchestratorClient.goldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(step = TaskStep.PoolSync))

        val expectedEntry = TaskStepReservationEntryDto(reservedTask.taskId, reservedTask.recordId, businessPartnerResult)
        val expectedResponse = TaskStepReservationResponse(reservedTasks = listOf(expectedEntry), timeout = Instant.now().plus(resultFactory.pendingTimeout))

        assertRepo.assertBusinessPartnerTaskReservationResponseEqual(actualReservationResponse, expectedResponse, ignoreRecordId = false)
    }

    /**
     * GIVEN reserved task
     * WHEN user resolves task as legal entity additional address
     * THEN user sees resolved task data in next step
     */
    @Test
    fun `resolve reserved task as legal entity additional address`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)
        val reservedTask = testDataClient.reserveBusinessPartnerTasks(createdTask.processingState.step).reservedTasks.single()

        //WHEN
        val businessPartnerResult = requestFactory.buildLegalEntityAdditionalAddressBusinessPartner("result $testName")
        val resultEntry = TaskStepResultEntryDto(reservedTask.taskId, businessPartnerResult, emptyList())
        val resultRequest = TaskStepResultRequest(createdTask.processingState.step, listOf(resultEntry))
        orchestratorClient.goldenRecordTasks.resolveStepResults(resultRequest)

        //THEN
        val actualReservationResponse = orchestratorClient.goldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(step = TaskStep.PoolSync))

        val expectedEntry = TaskStepReservationEntryDto(reservedTask.taskId, reservedTask.recordId, businessPartnerResult)
        val expectedResponse = TaskStepReservationResponse(reservedTasks = listOf(expectedEntry), timeout = Instant.now().plus(resultFactory.pendingTimeout))

        assertRepo.assertBusinessPartnerTaskReservationResponseEqual(actualReservationResponse, expectedResponse, ignoreRecordId = false)
    }

    /**
     * GIVEN reserved task
     * WHEN user resolves task as site additional address
     * THEN user sees resolved task data in next step
     */
    @Test
    fun `resolve reserved task as site additional address`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)
        val reservedTask = testDataClient.reserveBusinessPartnerTasks(createdTask.processingState.step).reservedTasks.single()

        //WHEN
        val businessPartnerResult = requestFactory.buildSiteAdditionalAddressBusinessPartner("result $testName")
        val resultEntry = TaskStepResultEntryDto(reservedTask.taskId, businessPartnerResult, emptyList())
        val resultRequest = TaskStepResultRequest(createdTask.processingState.step, listOf(resultEntry))
        orchestratorClient.goldenRecordTasks.resolveStepResults(resultRequest)

        //THEN
        val actualReservationResponse = orchestratorClient.goldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(step = TaskStep.PoolSync))

        val expectedEntry = TaskStepReservationEntryDto(reservedTask.taskId, reservedTask.recordId, businessPartnerResult)
        val expectedResponse = TaskStepReservationResponse(reservedTasks = listOf(expectedEntry), timeout = Instant.now().plus(resultFactory.pendingTimeout))

        assertRepo.assertBusinessPartnerTaskReservationResponseEqual(actualReservationResponse, expectedResponse, ignoreRecordId = false)
    }

    /**
     * WHEN user tries to resolve not existing task
     * THEN user sees HTTP BAD REQUEST error
     */
    @Test
    fun `try resolve not existing task`(){
        //WHEN
        val businessPartnerResult = requestFactory.buildAdditionalAddressOfSiteBusinessPartner(testName)
        val resultEntry = TaskStepResultEntryDto("NOT EXISTING", businessPartnerResult, emptyList())
        val resultRequest = TaskStepResultRequest(TaskStep.CleanAndSync, listOf(resultEntry))
        val request: () -> Unit = { orchestratorClient.goldenRecordTasks.resolveStepResults(resultRequest) }

        //THEN
        Assertions.assertThatThrownBy(request).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN task not reserved in step
     * WHEN user tries to resolve task for step
     * THEN user sees HTTP BAD REQUEST error
     *
     */
    @Test
    @Disabled("ToDo: Possible error behaviour https://github.com/eclipse-tractusx/bpdm/issues/1579")
    fun `try resolve not reserved step task`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)

        //WHEN
        val businessPartnerResult = requestFactory.buildAdditionalAddressOfSiteBusinessPartner(testName)
        val resultEntry = TaskStepResultEntryDto(createdTask.taskId, businessPartnerResult, emptyList())
        val resultRequest = TaskStepResultRequest(createdTask.processingState.step, listOf(resultEntry))
        val request: () -> Unit = { orchestratorClient.goldenRecordTasks.resolveStepResults(resultRequest) }

        //THEN
        Assertions.assertThatThrownBy(request).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN task has been resolved for step
     * WHEN user tries to resolve task for step again
     * THEN request accepted but result ignored
     *
     */
    @Test
    fun `resolve task again`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)
        val resultRequest1 = testDataClient.reserveAndResolveBusinessPartnerTask(createdTask.taskId, createdTask.processingState.step, "Resolved $testName")

        //WHEN
        val businessPartnerResult = requestFactory.buildAdditionalAddressOfSiteBusinessPartner(testName)
        val resultEntry = TaskStepResultEntryDto(createdTask.taskId, businessPartnerResult, emptyList())
        val resultRequest2 = TaskStepResultRequest(createdTask.processingState.step, listOf(resultEntry))
        orchestratorClient.goldenRecordTasks.resolveStepResults(resultRequest2)

        //THEN
        val actualReservationResponse = orchestratorClient.goldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(step = TaskStep.PoolSync))

        val expectedEntry = TaskStepReservationEntryDto(resultRequest1.taskId, "any UUID", resultRequest1.businessPartner)
        val expectedResponse = TaskStepReservationResponse(reservedTasks = listOf(expectedEntry), timeout = Instant.now().plus(resultFactory.pendingTimeout))

        assertRepo.assertBusinessPartnerTaskReservationResponseEqual(actualReservationResponse, expectedResponse, ignoreRecordId = true)
    }

    /**
     * GIVEN business partner tasks to resolve
     * WHEN posting too many cleaning results (over the upsert limit)
     * THEN show 400 BAD REQUEST error
     */
    @Test
    fun `expect exception on posting too many business partner task results`() {
        //GIVEN
        (1 .. 11).forEach { testDataClient.createBusinessPartnerTask("$testName $it") }
        val reservedTasks = (1 .. 2).flatMap {
            orchestratorClient.goldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(10, step = TaskStep.CleanAndSync)).reservedTasks
        }

        //WHEN
        val requestBody = TaskStepResultRequest(
            step = TaskStep.CleanAndSync,
            reservedTasks.map { TaskStepResultEntryDto(it.taskId, it.businessPartner) }
        )
        val request: () -> Unit = { orchestratorClient.goldenRecordTasks.resolveStepResults(requestBody) }

        //THEN
        Assertions.assertThatThrownBy(request).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN aborted business partner task
     * WHEN user tries to resolve task
     * THEN user sees 200 OK response
     */
    @Test
    fun `aborted business partner task throws no error when trying to resolve`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)
        val reservedTask = testDataClient.reserveBusinessPartnerTasks(createdTask.processingState.step).reservedTasks.single()
        testDataClient.createBusinessPartnerTask(testName, recordId = createdTask.recordId)

        //WHEN
        val businessPartnerResult = requestFactory.buildLegalEntityBusinessPartner("result $testName")
        val resultEntry = TaskStepResultEntryDto(reservedTask.taskId, businessPartnerResult, emptyList())
        val resultRequest = TaskStepResultRequest(createdTask.processingState.step, listOf(resultEntry))
        orchestratorClient.goldenRecordTasks.resolveStepResults(resultRequest)
    }
}