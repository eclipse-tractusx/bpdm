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
import org.eclipse.tractusx.bpdm.orchestrator.api.SharingMemberRecord
import org.eclipse.tractusx.bpdm.orchestrator.api.model.*
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
        businessPartnerData: BusinessPartner
    ): TaskClientStateDto{
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
        scriptVariants: List<UncategorizedScriptVariant> = emptyList()
    ): TaskClientStateDto{
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
        scriptVariants: List<UncategorizedScriptVariant> = emptyList()
    ): TaskClientStateDto{
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
        scriptVariants: List<UncategorizedScriptVariant> = emptyList()
    ): TaskClientStateDto{
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
        scriptVariants: List<UncategorizedScriptVariant> = emptyList()
    ): TaskClientStateDto{
        configureWireMock()

        val mockedCreatedTask = mockCreateTask(seed)

        val refinementTaskData = refinementTestDataFactory.buildAdditionSiteAddressBusinessPartner(legalEntityGoldenRecord, siteGoldenRecord, addressGoldenRecord, owningCompany, nameParts, scriptVariants)
        val mockedRefinedTask = mockTaskRefinedSuccessfully(mockedCreatedTask.taskId, refinementTaskData, seed)

        return mockedRefinedTask
    }

    fun mockRefineRelation(seed: String): TaskClientRelationsStateDto{
        configureWireMock()

        val mockedCreatedTask = mockCreateRelationTask(seed)

        val inputRelation = requestFactory.buildRelation(seed)
        val refinementTaskData = resultFactory.buildCreatedRelationTaskClientState(inputRelation, TaskMode.UpdateFromSharingMember)

        val mockedRefinedTask = mockRelationTaskRefinedSuccessfully(mockedCreatedTask.taskId, refinementTaskData.businessPartnerRelationsResult, seed)

        return mockedRefinedTask
    }

    fun mockCreateTask(seed: String): TaskClientStateDto{
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

    fun getCreatedTaskBusinessPartners(): List<BusinessPartner>{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        val loggedRequests = WireMock.findAll(WireMock.postRequestedFor(WireMock.urlPathEqualTo(BASE_PATH_V7_BUSINESS_PARTNERS)))

        return loggedRequests
            .map { jsonMapper.readValue(it.body, TaskCreateRequest::class.java) }
            .flatMap { createRequest -> createRequest.requests.map { it.businessPartner } }
    }

    fun mockSharingMemberRecordUpdates(){
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        WireMock.stubFor(
            WireMock
                .put(WireMock.urlPathEqualTo(BASE_PATH_V7_SHARING_MEMBER_RECORDS))
                .willReturn(WireMock.okJson(jsonMapper.writeValueAsString(SharingMemberRecord("", null, Instant.now(), Instant.now()))))
        )
    }

    fun mockSharingMemberRecords(records: List<SharingMemberRecord>){
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

    fun getSharingMemberRecordUpdates(): List<SharingMemberRecordUpdateRequest>{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        return WireMock.findAll(WireMock.putRequestedFor(WireMock.urlPathEqualTo(BASE_PATH_V7_SHARING_MEMBER_RECORDS)))
            .map { jsonMapper.readValue(it.body, SharingMemberRecordUpdateRequest::class.java) }
    }

    fun mockTaskRefinedSuccessfully(taskId: String, refinementTaskData: BusinessPartner, seed: String): TaskClientStateDto{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/finished-events")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(
                FinishedTaskEventsResponse(1, 1, 0, 1, listOf(
                    FinishedTaskEventsResponse.Event(Instant.now(), ResultState.Success, taskId)
                ))
            )
        )))

        val mockedRefinedTasks = buildSuccessTaskState(seed, refinementTaskData)
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/state/search")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(mockedRefinedTasks)
        )))

        return mockedRefinedTasks.tasks.single()
    }

    fun mockRelationTaskRefinedSuccessfully(taskId: String, refinementTaskData: BusinessPartnerRelations, seed: String): TaskClientRelationsStateDto{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("${BASE_PATH_V7_RELATIONS}/finished-events")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(
                FinishedTaskEventsResponse(1, 1, 0, 1, listOf(
                    FinishedTaskEventsResponse.Event(Instant.now(), ResultState.Success, taskId)
                ))
            )
        )))

        val mockedRefinedTasks = buildSuccessRelationTaskState(seed, refinementTaskData)
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("${BASE_PATH_V7_RELATIONS}/state/search")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(mockedRefinedTasks)
        )))

        return mockedRefinedTasks.tasks.single()
    }

    fun mockSharingError(seed: String, errorType: TaskErrorType): TaskClientStateDto{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        val mockedCreatedTask = mockCreateTask(seed)

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/finished-events")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(
                FinishedTaskEventsResponse(1, 1, 0, 1, listOf(
                    FinishedTaskEventsResponse.Event(Instant.now(), ResultState.Error, mockedCreatedTask.taskId)
                ))
            )
        )))

        val mockedErrorTasks = buildErrorTaskState(seed, errorType)
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/state/search")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(mockedErrorTasks)
        )))

        return mockedErrorTasks.tasks.single()
    }

    fun mockMissingTask(seed: String): TaskClientStateDto{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        val mockedCreatedTask = mockCreateTask(seed)

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/finished-events")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(
                FinishedTaskEventsResponse(1, 1, 0, 1, listOf(
                    FinishedTaskEventsResponse.Event(Instant.now(), ResultState.Success, mockedCreatedTask.taskId)
                ))
            )
        )))

        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/state/search")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(TaskStateResponse(emptyList()))
        )))

        return mockedCreatedTask
    }

    fun mockTasksLost(){
        mockTaskResultStates(null)
    }

    fun mockTasksPending(){
        mockTaskResultStates(ResultState.Pending)
    }

    private fun mockTaskResultStates(resultState: ResultState?){
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/result-state/search")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(TaskResultStateSearchResponse(listOf(resultState)))
        )))
    }

    fun mockRelationSharingError(seed: String, errorType: TaskRelationsErrorType): TaskClientRelationsStateDto{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        val mockedCreatedTask = mockCreateRelationTask(seed)

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("${BASE_PATH_V7_RELATIONS}/finished-events")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(
                FinishedTaskEventsResponse(1, 1, 0, 1, listOf(
                    FinishedTaskEventsResponse.Event(Instant.now(), ResultState.Error, mockedCreatedTask.taskId)
                ))
            )
        )))

        val mockedErrorTasks = buildErrorRelationTaskState(seed, errorType)
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("${BASE_PATH_V7_RELATIONS}/state/search")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(mockedErrorTasks)
        )))

        return mockedErrorTasks.tasks.single()
    }

    fun mockCreateRelationTask(seed: String): TaskClientRelationsStateDto{
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

    fun mockReservedBusinessPartner(seed: String, businessPartner: BusinessPartner): TaskStepReservationEntryDto{
        return mockReservedTasks(buildReservedTasks(seed, businessPartner).reservedTasks).single()
    }

    fun mockReservedTasks(reservedTasks: List<TaskStepReservationEntryDto>): List<TaskStepReservationEntryDto>{
        configureWireMock()

        mockStepReservations(BASE_PATH_V7_BUSINESS_PARTNERS, TaskStepReservationResponse(reservedTasks, reservationTimeout()), TaskStepReservationResponse(emptyList(), reservationTimeout()))
        mockStepResultsResponse(200)
        mockStepReservations(BASE_PATH_V7_RELATIONS, TaskRelationsStepReservationResponse(emptyList(), reservationTimeout()), TaskRelationsStepReservationResponse(emptyList(), reservationTimeout()))

        return reservedTasks
    }

    fun mockReservedRelationTasks(reservedTasks: List<TaskRelationsStepReservationEntryDto>): List<TaskRelationsStepReservationEntryDto>{
        configureWireMock()

        mockStepReservations(BASE_PATH_V7_RELATIONS, TaskRelationsStepReservationResponse(reservedTasks, reservationTimeout()), TaskRelationsStepReservationResponse(emptyList(), reservationTimeout()))
        mockRelationStepResultsResponse(200)
        mockStepReservations(BASE_PATH_V7_BUSINESS_PARTNERS, TaskStepReservationResponse(emptyList(), reservationTimeout()), TaskStepReservationResponse(emptyList(), reservationTimeout()))

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

    fun getRelationStepResults(): List<TaskRelationsStepResultEntryDto>{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        return WireMock.findAll(WireMock.postRequestedFor(WireMock.urlPathEqualTo("${BASE_PATH_V7_RELATIONS}/step-results")))
            .flatMap { jsonMapper.readValue(it.body, TaskRelationsStepResultRequest::class.java).results }
    }

    fun getStepResults(): List<TaskStepResultEntryDto>{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        return WireMock.findAll(WireMock.postRequestedFor(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/step-results")))
            .flatMap { jsonMapper.readValue(it.body, TaskStepResultRequest::class.java).results }
    }

    fun getBusinessPartnerResolution(): BusinessPartner{
        return getBusinessPartnerResolutionEntry().businessPartner
    }

    fun getBusinessPartnerResolutionErrors(): List<TaskErrorDto>{
        return getBusinessPartnerResolutionEntry().errors
    }

    private fun getBusinessPartnerResolutionEntry(): TaskStepResultEntryDto{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        val loggedRequests = WireMock.findAll(WireMock.postRequestedFor(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/step-results")))
        val postedResult =  jsonMapper.readValue(loggedRequests.first().body, TaskStepResultRequest::class.java)

        return postedResult.results.first()
    }

    fun buildInitialTaskCreationResponse(seed: String): TaskCreateResponse{
        return TaskCreateResponse(
            listOf(
                TaskClientStateDto(
                    UUID.nameUUIDFromBytes("TaskID_$seed".encodeToByteArray()).toString(),
                    UUID.nameUUIDFromBytes("RecordID_$seed".encodeToByteArray()).toString(),
                    BusinessPartner.empty,
                    TaskProcessingStateDto(
                        ResultState.Pending,
                        TaskStep.CleanAndSync,
                        StepState.Queued,
                        emptyList(),
                        Instant.now(),
                        Instant.now(),
                        Instant.now()
                    )
                )
            )
        )
    }

    fun buildInitialRelationTaskResponse(seed: String): TaskCreateRelationsResponse{
        return TaskCreateRelationsResponse(
            listOf(
                TaskClientRelationsStateDto(
                    UUID.nameUUIDFromBytes("TaskID_$seed".encodeToByteArray()).toString(),
                    UUID.nameUUIDFromBytes("RecordID_$seed".encodeToByteArray()).toString(),
                    BusinessPartnerRelations.empty,
                    TaskProcessingRelationsStateDto(
                        ResultState.Pending,
                        TaskStep.CleanAndSync,
                        StepState.Queued,
                        emptyList(),
                        Instant.now(),
                        Instant.now(),
                        Instant.now()
                    )
                )
            )
        )
    }

    fun buildSuccessTaskState(seed: String, finishedBusinessPartner: BusinessPartner): TaskStateResponse{
        return TaskStateResponse(
            listOf(
                TaskClientStateDto(
                    UUID.nameUUIDFromBytes("TaskID_$seed".encodeToByteArray()).toString(),
                    UUID.nameUUIDFromBytes("RecordID_$seed".encodeToByteArray()).toString(),
                    finishedBusinessPartner,
                    TaskProcessingStateDto(
                        ResultState.Success,
                        TaskStep.PoolSync,
                        StepState.Success,
                        emptyList(),
                        Instant.now(),
                        Instant.now(),
                        Instant.now()
                    )
                )
            )
        )
    }

    fun buildSuccessRelationTaskState(seed: String, finishedRelation: BusinessPartnerRelations): TaskRelationsStateResponse {
        return TaskRelationsStateResponse(
            listOf(
                TaskClientRelationsStateDto(
                    UUID.nameUUIDFromBytes("TaskID_$seed".encodeToByteArray()).toString(),
                    UUID.nameUUIDFromBytes("RecordID_$seed".encodeToByteArray()).toString(),
                    finishedRelation,
                    TaskProcessingRelationsStateDto(
                        ResultState.Success,
                        TaskStep.PoolSync,
                        StepState.Success,
                        emptyList(),
                        Instant.now(),
                        Instant.now(),
                        Instant.now()
                    )
                )
            )
        )
    }

    private fun buildErrorTaskState(seed: String, errorType: TaskErrorType): TaskStateResponse{
        return TaskStateResponse(
            listOf(
                TaskClientStateDto(
                    UUID.nameUUIDFromBytes("TaskID_$seed".encodeToByteArray()).toString(),
                    UUID.nameUUIDFromBytes("RecordID_$seed".encodeToByteArray()).toString(),
                    BusinessPartner.empty,
                    TaskProcessingStateDto(
                        ResultState.Error,
                        TaskStep.PoolSync,
                        StepState.Error,
                        listOf(TaskErrorDto(errorType, "$seed Description")),
                        Instant.now(),
                        Instant.now(),
                        Instant.now()
                    )
                )
            )
        )
    }

    private fun buildErrorRelationTaskState(seed: String, errorType: TaskRelationsErrorType): TaskRelationsStateResponse{
        return TaskRelationsStateResponse(
            listOf(
                TaskClientRelationsStateDto(
                    UUID.nameUUIDFromBytes("TaskID_$seed".encodeToByteArray()).toString(),
                    UUID.nameUUIDFromBytes("RecordID_$seed".encodeToByteArray()).toString(),
                    BusinessPartnerRelations.empty,
                    TaskProcessingRelationsStateDto(
                        ResultState.Error,
                        TaskStep.PoolSync,
                        StepState.Error,
                        listOf(TaskRelationsErrorDto(errorType, "$seed Description")),
                        Instant.now(),
                        Instant.now(),
                        Instant.now()
                    )
                )
            )
        )
    }

    private fun buildReservedTasks(seed: String, businessPartner: BusinessPartner): TaskStepReservationResponse{
        return TaskStepReservationResponse(
            listOf(
                TaskStepReservationEntryDto(
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