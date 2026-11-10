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

package org.eclipse.tractusx.bpdm.test.testdata.orchestrator

import com.github.tomakehurst.wiremock.client.WireMock
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.test.containers.OrchestratorMockContextInitializer
import org.eclipse.tractusx.bpdm.orchestrator.api.ApiCommons.BASE_PATH_V7_BUSINESS_PARTNERS
import org.eclipse.tractusx.bpdm.orchestrator.api.ApiCommons.BASE_PATH_V7_RELATIONS
import org.eclipse.tractusx.bpdm.orchestrator.api.ApiCommons.BASE_PATH_V7_SHARING_MEMBER_RECORDS
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.sharingmember.SharingMemberRecordV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartnerV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.BusinessPartnerRelationsV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.FinishedTaskEventsV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.ResultStateV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.sharingmember.SharingMemberRecordUpdateV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.StepStateV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskClientRelationsStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskClientStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskCreateRelationsV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskCreateV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskCreateV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskErrorV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskErrorTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskModeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskProcessingRelationsStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskProcessingStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsErrorV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsErrorTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStateV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepReservationEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepReservationV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepResultEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepResultV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskResultStateSearchV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStateV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepResultEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepResultV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.UncategorizedScriptVariantV7Dto
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.time.Instant
import java.util.*

class OrchestratorMockDataFactory(
    private val refinementTestDataFactory: RefinementTestDataFactory,
    private val requestFactory: OrchestratorRequestFactoryV7,
    private val resultFactory: OrchestratorExpectedResultFactoryV7,
    private val jsonMapper: JsonMapper
) {

    private val orchestratorMockServer = OrchestratorMockContextInitializer.wiremockServer

    fun mockRefineToSuccess(
        seed: String,
        businessPartnerData: BusinessPartnerV7Dto
    ): TaskClientStateV7Dto{
        configureWireMock()

        val mockedCreatedTask = mockCreateTask(seed)
        val mockedRefinedTask = mockTaskRefinedSuccessfully(mockedCreatedTask.taskId, businessPartnerData, seed)

        return mockedRefinedTask
    }

    fun mockRefineToLegalEntity(
        seed: String,
        legalEntityGoldenRecord: LegalEntityWithLegalAddressVerboseV7Dto,
        owningCompany: String?,
        nameParts: List<String>,
        scriptVariants: List<UncategorizedScriptVariantV7Dto> = emptyList()
    ): TaskClientStateV7Dto{
        configureWireMock()

        val mockedCreatedTask = mockCreateTask(seed)

        val refinementTaskData = refinementTestDataFactory.buildLegalEntityBusinessPartner(legalEntityGoldenRecord, owningCompany, nameParts, scriptVariants)
        val mockedRefinedTask = mockTaskRefinedSuccessfully(mockedCreatedTask.taskId, refinementTaskData, seed)

        return mockedRefinedTask
    }

    fun mockRefineToLegalEntityOnSite(
        seed: String,
        legalEntityGoldenRecord: LegalEntityWithLegalAddressVerboseV7Dto,
        siteGoldenRecord: SiteVerboseV7Dto,
        owningCompany: String?,
        nameParts: List<String>,
        scriptVariants: List<UncategorizedScriptVariantV7Dto> = emptyList()
    ): TaskClientStateV7Dto{
        configureWireMock()

        val mockedCreatedTask = mockCreateTask(seed)

        val refinementTaskData = refinementTestDataFactory.buildLegalEntityOnSiteBusinessPartner(
            legalEntityGoldenRecord,
            siteGoldenRecord,
            owningCompany,
            nameParts,
            scriptVariants
        )
        val mockedRefinedTask = mockTaskRefinedSuccessfully(mockedCreatedTask.taskId, refinementTaskData, seed)

        return mockedRefinedTask
    }

    fun mockRefineToSite(
        seed: String,
        legalEntityGoldenRecord: LegalEntityWithLegalAddressVerboseV7Dto,
        siteGoldenRecord: SiteWithMainAddressVerboseV7Dto,
        owningCompany: String?,
        nameParts: List<String>,
        scriptVariants: List<UncategorizedScriptVariantV7Dto> = emptyList()
    ): TaskClientStateV7Dto{
        configureWireMock()

        val mockedCreatedTask = mockCreateTask(seed)

        val refinementTaskData = refinementTestDataFactory.buildSiteBusinessPartner(legalEntityGoldenRecord, siteGoldenRecord, owningCompany, nameParts, scriptVariants)
        val mockedRefinedTask = mockTaskRefinedSuccessfully(mockedCreatedTask.taskId, refinementTaskData, seed)

        return mockedRefinedTask
    }

    fun mockRefineToAdditionalAddressOfSite(
        seed: String,
        legalEntityGoldenRecord: LegalEntityWithLegalAddressVerboseV7Dto,
        siteGoldenRecord: SiteWithMainAddressVerboseV7Dto,
        addressGoldenRecord: LogisticAddressVerboseV7Dto,
        owningCompany: String?,
        nameParts: List<String>,
        scriptVariants: List<UncategorizedScriptVariantV7Dto> = emptyList()
    ): TaskClientStateV7Dto{
        configureWireMock()

        val mockedCreatedTask = mockCreateTask(seed)

        val refinementTaskData = refinementTestDataFactory.buildAdditionSiteAddressBusinessPartner(legalEntityGoldenRecord, siteGoldenRecord, addressGoldenRecord, owningCompany, nameParts, scriptVariants)
        val mockedRefinedTask = mockTaskRefinedSuccessfully(mockedCreatedTask.taskId, refinementTaskData, seed)

        return mockedRefinedTask
    }

    fun mockRefineRelation(seed: String): TaskClientRelationsStateV7Dto{
        configureWireMock()

        val mockedCreatedTask = mockCreateRelationTask(seed)

        val inputRelation = requestFactory.buildRelation(seed)
        val refinementTaskData = resultFactory.buildCreatedRelationTaskClientState(inputRelation, TaskModeV7.UpdateFromSharingMember)

        val mockedRefinedTask = mockRelationTaskRefinedSuccessfully(mockedCreatedTask.taskId, refinementTaskData.businessPartnerRelationsResult, seed)

        return mockedRefinedTask
    }

    fun mockCreateTask(seed: String): TaskClientStateV7Dto{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        val mockedCreatedTaskResponse = buildInitialTaskCreationResponse(seed)
        val mockedCreatedTask = mockedCreatedTaskResponse.createdTasks.single()
        WireMock.stubFor(
            WireMock
                .post(BASE_PATH_V7_BUSINESS_PARTNERS)
                .willReturn(WireMock.okJson(jsonMapper.writeValueAsString(mockedCreatedTaskResponse)))
        )

        return mockedCreatedTask
    }

    fun resetRecordedRequests(){
        WireMock.configureFor("localhost", orchestratorMockServer.port())
        WireMock.resetAllRequests()
    }

    fun getCreatedTaskBusinessPartners(): List<BusinessPartnerV7Dto>{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        val loggedRequests = WireMock.findAll(WireMock.postRequestedFor(WireMock.urlPathEqualTo(BASE_PATH_V7_BUSINESS_PARTNERS)))

        return loggedRequests
            .map { jsonMapper.readValue(it.body, TaskCreateV7Request::class.java) }
            .flatMap { createRequest -> createRequest.requests.map { it.businessPartner } }
    }

    fun mockSharingMemberRecordUpdates(){
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        WireMock.stubFor(
            WireMock
                .put(WireMock.urlPathEqualTo(BASE_PATH_V7_SHARING_MEMBER_RECORDS))
                .willReturn(WireMock.okJson(jsonMapper.writeValueAsString(SharingMemberRecordV7Dto("", null, Instant.now(), Instant.now()))))
        )
    }

    fun mockSharingMemberRecords(records: List<SharingMemberRecordV7Dto>){
        configureWireMock()

        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo(BASE_PATH_V7_SHARING_MEMBER_RECORDS))
                .willReturn(WireMock.okJson(jsonMapper.writeValueAsString(PageDto(records.size.toLong(), 1, 0, records.size, records))))
        )
    }

    fun getSharingMemberRecordQueryTimestamps(): List<Instant>{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        return WireMock.findAll(WireMock.getRequestedFor(WireMock.urlPathEqualTo(BASE_PATH_V7_SHARING_MEMBER_RECORDS)))
            .map { Instant.parse(it.queryParameter("timestampAfter").firstValue()) }
    }

    fun getSharingMemberRecordUpdates(): List<SharingMemberRecordUpdateV7Request>{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        return WireMock.findAll(WireMock.putRequestedFor(WireMock.urlPathEqualTo(BASE_PATH_V7_SHARING_MEMBER_RECORDS)))
            .map { jsonMapper.readValue(it.body, SharingMemberRecordUpdateV7Request::class.java) }
    }

    fun mockTaskRefinedSuccessfully(taskId: String, refinementTaskData: BusinessPartnerV7Dto, seed: String): TaskClientStateV7Dto{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/finished-events")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(
                FinishedTaskEventsV7Response(1, 1, 0, 1, listOf(
                    FinishedTaskEventsV7Response.Event(Instant.now(), ResultStateV7.Success, taskId)
                ))
            )
        )))

        val mockedRefinedTasks = buildSuccessTaskState(seed, refinementTaskData)
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/state/search")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(mockedRefinedTasks)
        )))

        return mockedRefinedTasks.tasks.single()
    }

    fun mockRelationTaskRefinedSuccessfully(taskId: String, refinementTaskData: BusinessPartnerRelationsV7Dto, seed: String): TaskClientRelationsStateV7Dto{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("${BASE_PATH_V7_RELATIONS}/finished-events")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(
                FinishedTaskEventsV7Response(1, 1, 0, 1, listOf(
                    FinishedTaskEventsV7Response.Event(Instant.now(), ResultStateV7.Success, taskId)
                ))
            )
        )))

        val mockedRefinedTasks = buildSuccessRelationTaskState(seed, refinementTaskData)
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("${BASE_PATH_V7_RELATIONS}/state/search")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(mockedRefinedTasks)
        )))

        return mockedRefinedTasks.tasks.single()
    }

    fun mockSharingError(seed: String, errorType: TaskErrorTypeV7): TaskClientStateV7Dto{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        val mockedCreatedTask = mockCreateTask(seed)

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/finished-events")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(
                FinishedTaskEventsV7Response(1, 1, 0, 1, listOf(
                    FinishedTaskEventsV7Response.Event(Instant.now(), ResultStateV7.Error, mockedCreatedTask.taskId)
                ))
            )
        )))

        val mockedErrorTasks = buildErrorTaskState(seed, errorType)
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/state/search")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(mockedErrorTasks)
        )))

        return mockedErrorTasks.tasks.single()
    }

    fun mockMissingTask(seed: String): TaskClientStateV7Dto{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        val mockedCreatedTask = mockCreateTask(seed)

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/finished-events")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(
                FinishedTaskEventsV7Response(1, 1, 0, 1, listOf(
                    FinishedTaskEventsV7Response.Event(Instant.now(), ResultStateV7.Success, mockedCreatedTask.taskId)
                ))
            )
        )))

        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/state/search")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(TaskStateV7Response(emptyList()))
        )))

        return mockedCreatedTask
    }

    fun mockTasksLost(){
        mockTaskResultStates(null)
    }

    fun mockTasksPending(){
        mockTaskResultStates(ResultStateV7.Pending)
    }

    private fun mockTaskResultStates(resultState: ResultStateV7?){
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/result-state/search")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(TaskResultStateSearchV7Response(listOf(resultState)))
        )))
    }

    fun mockRelationSharingError(seed: String, errorType: TaskRelationsErrorTypeV7): TaskClientRelationsStateV7Dto{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        val mockedCreatedTask = mockCreateRelationTask(seed)

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("${BASE_PATH_V7_RELATIONS}/finished-events")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(
                FinishedTaskEventsV7Response(1, 1, 0, 1, listOf(
                    FinishedTaskEventsV7Response.Event(Instant.now(), ResultStateV7.Error, mockedCreatedTask.taskId)
                ))
            )
        )))

        val mockedErrorTasks = buildErrorRelationTaskState(seed, errorType)
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("${BASE_PATH_V7_RELATIONS}/state/search")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(mockedErrorTasks)
        )))

        return mockedErrorTasks.tasks.single()
    }

    fun mockCreateRelationTask(seed: String): TaskClientRelationsStateV7Dto{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        val mockedCreatedTaskResponse = buildInitialRelationTaskResponse(seed)
        val mockedCreatedTask = mockedCreatedTaskResponse.createdTasks.single()
        WireMock.stubFor(
            WireMock
                .post(BASE_PATH_V7_RELATIONS)
                .willReturn(WireMock.okJson(jsonMapper.writeValueAsString(mockedCreatedTaskResponse)))
        )

        return mockedCreatedTask
    }

    fun mockReservedBusinessPartner(seed: String, businessPartner: BusinessPartnerV7Dto): TaskStepReservationEntryV7Dto{
        return mockReservedTasks(buildReservedTasks(seed, businessPartner).reservedTasks).single()
    }

    fun mockReservedTasks(reservedTasks: List<TaskStepReservationEntryV7Dto>): List<TaskStepReservationEntryV7Dto>{
        configureWireMock()

        mockStepReservations(BASE_PATH_V7_BUSINESS_PARTNERS, TaskStepReservationV7Response(reservedTasks, reservationTimeout()), TaskStepReservationV7Response(emptyList(), reservationTimeout()))
        mockStepResultsResponse(200)
        mockStepReservations(BASE_PATH_V7_RELATIONS, TaskRelationsStepReservationV7Response(emptyList(), reservationTimeout()), TaskRelationsStepReservationV7Response(emptyList(), reservationTimeout()))

        return reservedTasks
    }

    fun mockReservedRelationTasks(reservedTasks: List<TaskRelationsStepReservationEntryV7Dto>): List<TaskRelationsStepReservationEntryV7Dto>{
        configureWireMock()

        mockStepReservations(BASE_PATH_V7_RELATIONS, TaskRelationsStepReservationV7Response(reservedTasks, reservationTimeout()), TaskRelationsStepReservationV7Response(emptyList(), reservationTimeout()))
        mockRelationStepResultsResponse(200)
        mockStepReservations(BASE_PATH_V7_BUSINESS_PARTNERS, TaskStepReservationV7Response(emptyList(), reservationTimeout()), TaskStepReservationV7Response(emptyList(), reservationTimeout()))

        return reservedTasks
    }

    private fun mockStepReservations(basePath: String, reservation: Any, emptyReservation: Any){
        val scenario = "$basePath/step-reservations"

        WireMock.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("$basePath/step-reservations"))
                .inScenario(scenario)
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(WireMock.okJson(jsonMapper.writeValueAsString(reservation)))
                .willSetStateTo("empty")
        )
        WireMock.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("$basePath/step-reservations"))
                .inScenario(scenario)
                .whenScenarioStateIs("empty")
                .willReturn(WireMock.okJson(jsonMapper.writeValueAsString(emptyReservation)))
        )
    }

    private fun reservationTimeout() = Instant.now().plus(Duration.ofSeconds(60))

    fun mockStepReservationsResponse(status: Int){
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/step-reservations"))
            .willReturn(WireMock.status(status)))
    }

    fun mockStepResultsResponse(status: Int){
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/step-results"))
            .willReturn(WireMock.status(status)))
    }

    fun mockRelationStepResultsResponse(status: Int){
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("${BASE_PATH_V7_RELATIONS}/step-results"))
            .willReturn(WireMock.status(status)))
    }

    fun getRelationStepResults(): List<TaskRelationsStepResultEntryV7Dto>{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        return WireMock.findAll(WireMock.postRequestedFor(WireMock.urlPathEqualTo("${BASE_PATH_V7_RELATIONS}/step-results")))
            .flatMap { jsonMapper.readValue(it.body, TaskRelationsStepResultV7Request::class.java).results }
    }

    fun getStepResults(): List<TaskStepResultEntryV7Dto>{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        return WireMock.findAll(WireMock.postRequestedFor(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/step-results")))
            .flatMap { jsonMapper.readValue(it.body, TaskStepResultV7Request::class.java).results }
    }

    fun getBusinessPartnerResolution(): BusinessPartnerV7Dto{
        return getBusinessPartnerResolutionEntry().businessPartner
    }

    fun getBusinessPartnerResolutionErrors(): List<TaskErrorV7Dto>{
        return getBusinessPartnerResolutionEntry().errors
    }

    private fun getBusinessPartnerResolutionEntry(): TaskStepResultEntryV7Dto{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        val loggedRequests = WireMock.findAll(WireMock.postRequestedFor(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/step-results")))
        val postedResult =  jsonMapper.readValue(loggedRequests.first().body, TaskStepResultV7Request::class.java)

        return postedResult.results.first()
    }

    fun buildInitialTaskCreationResponse(seed: String): TaskCreateV7Response{
        return TaskCreateV7Response(
            listOf(
                TaskClientStateV7Dto(
                    UUID.nameUUIDFromBytes("TaskID_$seed".encodeToByteArray()).toString(),
                    UUID.nameUUIDFromBytes("RecordID_$seed".encodeToByteArray()).toString(),
                    BusinessPartnerV7Dto.empty,
                    TaskProcessingStateV7Dto(
                        ResultStateV7.Pending,
                        TaskStepV7.CleanAndSync,
                        StepStateV7.Queued,
                        emptyList(),
                        Instant.now(),
                        Instant.now(),
                        Instant.now()
                    )
                )
            )
        )
    }

    fun buildInitialRelationTaskResponse(seed: String): TaskCreateRelationsV7Response{
        return TaskCreateRelationsV7Response(
            listOf(
                TaskClientRelationsStateV7Dto(
                    UUID.nameUUIDFromBytes("TaskID_$seed".encodeToByteArray()).toString(),
                    UUID.nameUUIDFromBytes("RecordID_$seed".encodeToByteArray()).toString(),
                    BusinessPartnerRelationsV7Dto.empty,
                    TaskProcessingRelationsStateV7Dto(
                        ResultStateV7.Pending,
                        TaskStepV7.CleanAndSync,
                        StepStateV7.Queued,
                        emptyList(),
                        Instant.now(),
                        Instant.now(),
                        Instant.now()
                    )
                )
            )
        )
    }

    fun buildSuccessTaskState(seed: String, finishedBusinessPartner: BusinessPartnerV7Dto): TaskStateV7Response{
        return TaskStateV7Response(
            listOf(
                TaskClientStateV7Dto(
                    UUID.nameUUIDFromBytes("TaskID_$seed".encodeToByteArray()).toString(),
                    UUID.nameUUIDFromBytes("RecordID_$seed".encodeToByteArray()).toString(),
                    finishedBusinessPartner,
                    TaskProcessingStateV7Dto(
                        ResultStateV7.Success,
                        TaskStepV7.PoolSync,
                        StepStateV7.Success,
                        emptyList(),
                        Instant.now(),
                        Instant.now(),
                        Instant.now()
                    )
                )
            )
        )
    }

    fun buildSuccessRelationTaskState(seed: String, finishedRelation: BusinessPartnerRelationsV7Dto): TaskRelationsStateV7Response {
        return TaskRelationsStateV7Response(
            listOf(
                TaskClientRelationsStateV7Dto(
                    UUID.nameUUIDFromBytes("TaskID_$seed".encodeToByteArray()).toString(),
                    UUID.nameUUIDFromBytes("RecordID_$seed".encodeToByteArray()).toString(),
                    finishedRelation,
                    TaskProcessingRelationsStateV7Dto(
                        ResultStateV7.Success,
                        TaskStepV7.PoolSync,
                        StepStateV7.Success,
                        emptyList(),
                        Instant.now(),
                        Instant.now(),
                        Instant.now()
                    )
                )
            )
        )
    }

    private fun buildErrorTaskState(seed: String, errorType: TaskErrorTypeV7): TaskStateV7Response{
        return TaskStateV7Response(
            listOf(
                TaskClientStateV7Dto(
                    UUID.nameUUIDFromBytes("TaskID_$seed".encodeToByteArray()).toString(),
                    UUID.nameUUIDFromBytes("RecordID_$seed".encodeToByteArray()).toString(),
                    BusinessPartnerV7Dto.empty,
                    TaskProcessingStateV7Dto(
                        ResultStateV7.Error,
                        TaskStepV7.PoolSync,
                        StepStateV7.Error,
                        listOf(TaskErrorV7Dto(errorType, "$seed Description")),
                        Instant.now(),
                        Instant.now(),
                        Instant.now()
                    )
                )
            )
        )
    }

    private fun buildErrorRelationTaskState(seed: String, errorType: TaskRelationsErrorTypeV7): TaskRelationsStateV7Response{
        return TaskRelationsStateV7Response(
            listOf(
                TaskClientRelationsStateV7Dto(
                    UUID.nameUUIDFromBytes("TaskID_$seed".encodeToByteArray()).toString(),
                    UUID.nameUUIDFromBytes("RecordID_$seed".encodeToByteArray()).toString(),
                    BusinessPartnerRelationsV7Dto.empty,
                    TaskProcessingRelationsStateV7Dto(
                        ResultStateV7.Error,
                        TaskStepV7.PoolSync,
                        StepStateV7.Error,
                        listOf(TaskRelationsErrorV7Dto(errorType, "$seed Description")),
                        Instant.now(),
                        Instant.now(),
                        Instant.now()
                    )
                )
            )
        )
    }

    private fun buildReservedTasks(seed: String, businessPartner: BusinessPartnerV7Dto): TaskStepReservationV7Response{
        return TaskStepReservationV7Response(
            listOf(
                TaskStepReservationEntryV7Dto(
                    UUID.nameUUIDFromBytes("TaskID_$seed".encodeToByteArray()).toString(),
                    UUID.nameUUIDFromBytes("RecordID_$seed".encodeToByteArray()).toString(),
                    businessPartner
                )
            ),
            Instant.now().plus(Duration.ofSeconds(60)),
        )
    }

    private fun configureWireMock(){
        WireMock.configureFor("localhost", orchestratorMockServer.port())
        WireMock.reset()
    }

}