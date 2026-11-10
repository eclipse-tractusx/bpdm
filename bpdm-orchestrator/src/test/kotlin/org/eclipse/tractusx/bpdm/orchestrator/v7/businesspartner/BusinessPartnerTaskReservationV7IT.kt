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
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.SiteGoldenRecordRelationV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.SiteGoldenRecordRelationTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskCreateV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskModeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationV7Response
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

class BusinessPartnerTaskReservationV7IT: UnscheduledOrchestratorTestBaseV7() {

    /**
     * GIVEN task in queue in step
     * WHEN user requests reservation of new tasks for that step
     * THEN user sees reserved task
     */
    @Test
    fun `reserve queued tasks`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)

        //WHEN
        val reservationRequest = TaskStepReservationV7Request(step = createdTask.processingState.step)
        val reservedTasks = orchestratorClient.goldenRecordTasks.reserveTasksForStep(reservationRequest)

        //THEN
        val expectedEntry = TaskStepReservationEntryV7Dto(taskId = createdTask.taskId, recordId = createdTask.recordId, businessPartner = createdTask.businessPartnerResult)
        val expectedResult = TaskStepReservationV7Response(listOf(expectedEntry), Instant.now().plus(resultFactory.pendingTimeout))
        assertRepo.assertBusinessPartnerTaskReservationResponseEqual(reservedTasks, expectedResult, ignoreRecordId = true)
    }

    /**
     * GIVEN task in queue whose site carries golden record relations
     * WHEN user requests reservation of new tasks for that step
     * THEN user sees those relations on the reserved task's site
     *
     * The shared request factory does not populate golden record relations, so the relations are set on the
     * request here; reserving reads the task back and therefore covers the whole persistence round trip.
     */
    @Test
    fun `reserve queued task carrying site golden record relations`(){
        //GIVEN
        val siteRelation = SiteGoldenRecordRelationV7Dto(
            relationType = SiteGoldenRecordRelationTypeV7.IsReplacedBy,
            sourceBpn = "BPNS0000000001XY",
            targetBpn = "BPNS0000000002XY"
        )
        val entry = requestFactory.buildBusinessPartnerTaskCreateEntry(testName)
        val entryWithSiteRelation = entry.copy(
            recordId = null,
            businessPartner = entry.businessPartner.copy(
                site = entry.businessPartner.site?.copy(goldenRecordRelations = listOf(siteRelation))
            )
        )
        val createdTask = orchestratorClient.goldenRecordTasks
            .createTasks(TaskCreateV7Request(TaskModeV7.UpdateFromSharingMember, listOf(entryWithSiteRelation)))
            .createdTasks.single()

        //WHEN
        val reservedTasks = orchestratorClient.goldenRecordTasks
            .reserveTasksForStep(TaskStepReservationV7Request(step = createdTask.processingState.step))

        //THEN
        val createdBusinessPartner = createdTask.businessPartnerResult
        val expectedEntry = TaskStepReservationEntryV7Dto(
            taskId = createdTask.taskId,
            recordId = createdTask.recordId,
            businessPartner = createdBusinessPartner.copy(
                site = requireNotNull(createdBusinessPartner.site).copy(goldenRecordRelations = listOf(siteRelation))
            )
        )
        val expectedResult = TaskStepReservationV7Response(listOf(expectedEntry), Instant.now().plus(resultFactory.pendingTimeout))
        assertRepo.assertBusinessPartnerTaskReservationResponseEqual(reservedTasks, expectedResult, ignoreRecordId = true)
    }

    /**
     * GIVEN no tasks in queue of step
     * WHEN user requests reservation of new tasks in that step
     * THEN user sees no reserved tasks
     */
    @Test
    fun `reserve no queued tasks`(){
        //WHEN
        val reservationRequest = TaskStepReservationV7Request(step = TaskStepV7.CleanAndSync)
        val reservedTasks = orchestratorClient.goldenRecordTasks.reserveTasksForStep(reservationRequest)

        //THEN
        val expectedResult = TaskStepReservationV7Response(emptyList(), Instant.now())
        assertRepo.assertBusinessPartnerTaskReservationResponseEqual(reservedTasks, expectedResult, ignoreRecordId = false)
    }

    /**
     * GIVEN tasks queued in step
     * WHEN user requests reservation of new tasks of different step
     * THEN user sees no reserved tasks
     */
    @Test
    fun `reserve different step tasks`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)

        //WHEN
        val reservationRequest = TaskStepReservationV7Request(step = getDifferentStep(createdTask.processingState.step))
        val reservedTasks = orchestratorClient.goldenRecordTasks.reserveTasksForStep(reservationRequest)

        //THEN
        val expectedResult = TaskStepReservationV7Response(emptyList(), Instant.now())
        assertRepo.assertBusinessPartnerTaskReservationResponseEqual(reservedTasks, expectedResult, ignoreRecordId = false)
    }

    /**
     * GIVEN task reserved in step
     * WHEN user requests reservation of tasks for that step
     * THEN user sees no reserved tasks
     */
    @Test
    fun `reserve reserved step tasks`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)
        testDataClient.reserveBusinessPartnerTasks(createdTask.processingState.step)

        //WHEN
        val reservationRequest = TaskStepReservationV7Request(step = createdTask.processingState.step)
        val reservedTasks = orchestratorClient.goldenRecordTasks.reserveTasksForStep(reservationRequest)

        //THEN
        val expectedResult = TaskStepReservationV7Response(emptyList(), Instant.now())
        assertRepo.assertBusinessPartnerTaskReservationResponseEqual(reservedTasks, expectedResult, ignoreRecordId = false)
    }

    /**
     * WHEN reserving too many business partner tasks
     * THEN throw 400 BAD REQUEST
     */
    @Test
    fun `try requesting too many business partner cleaning tasks`() {
        //WHEN
        val request: () -> Unit = {
            orchestratorClient.goldenRecordTasks.reserveTasksForStep(TaskStepReservationV7Request(200, TaskStepV7.CleanAndSync))
        }

        //THEN
        Assertions.assertThatThrownBy(request).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    private fun getDifferentStep(step: TaskStepV7): TaskStepV7 {
        return TaskStepV7.entries.find { it != step }!!
    }
}