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

package org.eclipse.tractusx.bpdm.gate.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerClassificationDto
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerIdentifierDto
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.PostSharingStateReadyRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.service.GoldenRecordTaskService
import org.eclipse.tractusx.bpdm.gate.util.*
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.response.ChangelogEntryResponse
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["bpdm.api.upsert-limit=3"]
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class BusinessPartnerControllerIT @Autowired constructor(
    val testHelpers: DbTestHelpers,
    val gateClient: GateClient,
    val objectMapper: ObjectMapper,
    val goldenRecordTaskService: GoldenRecordTaskService
) {
    companion object {
        const val ORCHESTRATOR_CREATE_TASKS_URL = "/api/golden-record-tasks"
        const val ORCHESTRATOR_SEARCH_TASK_STATES_URL = "/api/golden-record-tasks/state/search"
        const val POOL_API_SEARCH_CHANGE_LOG_URL = "/api/catena/business-partners/changelog/search"

        @JvmField
        @RegisterExtension
        val gateWireMockServer: WireMockExtension = WireMockExtension.newInstance().options(WireMockConfiguration.wireMockConfig().dynamicPort()).build()

        @JvmField
        @RegisterExtension
        val poolWireMockServer: WireMockExtension = WireMockExtension.newInstance().options(WireMockConfiguration.wireMockConfig().dynamicPort()).build()


        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.client.pool.base-url") { poolWireMockServer.baseUrl() }
            registry.add("bpdm.client.orchestrator.base-url") { gateWireMockServer.baseUrl() }
        }
    }

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        gateWireMockServer.resetAll()
        poolWireMockServer.resetAll()
        this.mockOrchestratorApi()
    }

    @Test
    fun `insert minimal business partner`() {

        val upsertRequests = listOf(BusinessPartnerNonVerboseValues.bpInputRequestMinimal)
        val upsertResponses = gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests).body!!
        assertUpsertResponsesMatchRequests(upsertResponses, upsertRequests)

        assertBusinessPartnersUpsertedCorrectly(upsertResponses)
    }

    @Test
    fun `insert three business partners`() {
        val upsertRequests = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestFull,
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal,
            BusinessPartnerNonVerboseValues.bpInputRequestChina
        )
        val upsertResponses = gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests).body!!

        assertBusinessPartnersUpsertedCorrectly(upsertResponses)
    }

    @Test
    fun `insert three business partners and check sharing state is pending and has taskid`() {
        val upsertRequests = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestFull,
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal,
            BusinessPartnerNonVerboseValues.bpInputRequestChina
        )
        upsertBusinessPartnersAndShare(upsertRequests)

        val externalId1 = BusinessPartnerNonVerboseValues.bpInputRequestFull.externalId
        val externalId2 = BusinessPartnerNonVerboseValues.bpInputRequestMinimal.externalId
        val externalId3 = BusinessPartnerNonVerboseValues.bpInputRequestChina.externalId

        val externalIds = listOf(externalId1, externalId2, externalId3)

        val upsertSharingStatesRequests = listOf(
            SharingStateDto(
                businessPartnerType = BusinessPartnerType.GENERIC,
                externalId = externalId1,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                bpn = null,
                sharingProcessStarted = null,
                taskId = "0"
            ),
            SharingStateDto(
                businessPartnerType = BusinessPartnerType.GENERIC,
                externalId = externalId2,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                bpn = null,
                sharingProcessStarted = null,
                taskId = "1"
            ),
            SharingStateDto(
                businessPartnerType = BusinessPartnerType.GENERIC,
                externalId = externalId3,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                bpn = null,
                sharingProcessStarted = null,
                taskId = "2"
            )
        )

        val upsertSharingStateResponses = readSharingStates(BusinessPartnerType.GENERIC, externalIds)


        testHelpers.assertRecursively(upsertSharingStateResponses).isEqualTo(upsertSharingStatesRequests)

    }


    @Test
    fun `insert and then update business partner`() {

        val insertRequests = listOf(BusinessPartnerNonVerboseValues.bpInputRequestMinimal)
        val externalId = insertRequests.first().externalId
        val insertResponses = gateClient.businessParters.upsertBusinessPartnersInput(insertRequests).body!!
        assertUpsertResponsesMatchRequests(insertResponses, insertRequests)

        val searchResponse1Page = gateClient.businessParters.getBusinessPartnersInput(null)
        testHelpers.assertRecursively(searchResponse1Page.content).isEqualTo(insertResponses)

        val updateRequests = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestFull.copy(externalId = externalId)
        )
        val updateResponses = gateClient.businessParters.upsertBusinessPartnersInput(updateRequests).body!!
        assertUpsertResponsesMatchRequests(updateResponses, updateRequests)

        val searchResponse2Page = gateClient.businessParters.getBusinessPartnersInput(null)
        testHelpers.assertRecursively(searchResponse2Page.content).isEqualTo(updateResponses)
    }

    @Test
    fun `insert too many business partners`() {
        // limit is 3
        val upsertRequests = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestFull,
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal,
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal.copy(externalId = BusinessPartnerVerboseValues.externalId3),
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal.copy(externalId = BusinessPartnerVerboseValues.externalId4)
        )
        try {
            gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }
    }

    @Test
    fun `insert duplicate business partners`() {
        val upsertRequests = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestFull.copy(externalId = BusinessPartnerVerboseValues.externalId3),
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal.copy(externalId = BusinessPartnerVerboseValues.externalId3)
        )
        try {
            gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }
    }

    @Test
    fun `query business partners by externalId`() {
        val upsertRequests = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestFull.fastCopy(externalId = BusinessPartnerVerboseValues.externalId1, shortName = "1"),
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal.fastCopy(externalId = BusinessPartnerVerboseValues.externalId2, shortName = "2"),
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal.fastCopy(externalId = BusinessPartnerVerboseValues.externalId3, shortName = "3")
        )
        gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests)

        val searchResponsePage =
            gateClient.businessParters.getBusinessPartnersInput(listOf(BusinessPartnerVerboseValues.externalId1, BusinessPartnerVerboseValues.externalId3))
        assertUpsertResponsesMatchRequests(searchResponsePage.content, listOf(upsertRequests[0], upsertRequests[2]))
    }

    @Test
    fun `query business partners by missing externalId`() {
        val upsertRequests = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestFull.fastCopy(externalId = BusinessPartnerVerboseValues.externalId1, shortName = "1"),
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal.fastCopy(externalId = BusinessPartnerVerboseValues.externalId2, shortName = "2"),
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal.fastCopy(externalId = BusinessPartnerVerboseValues.externalId3, shortName = "3")
        )
        gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests)

        // missing externalIds are just ignored in the response
        val searchResponsePage =
            gateClient.businessParters.getBusinessPartnersInput(listOf(BusinessPartnerVerboseValues.externalId2, BusinessPartnerVerboseValues.externalId4))
        assertUpsertResponsesMatchRequests(searchResponsePage.content, listOf(upsertRequests[1]))
    }

    @Test
    fun `query business partners using paging`() {
        val upsertRequests = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestFull.fastCopy(
                externalId = BusinessPartnerNonVerboseValues.bpInputRequestFull.externalId,
                shortName = "1"
            ),
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal.fastCopy(externalId = BusinessPartnerVerboseValues.externalId2, shortName = "2"),
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal.fastCopy(externalId = BusinessPartnerVerboseValues.externalId3, shortName = "3")
        )
        gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests)

        // missing externalIds are just ignored in the response
        val searchResponsePage0 = gateClient.businessParters.getBusinessPartnersInput(null, PaginationRequest(0, 2))
        assertEquals(3, searchResponsePage0.totalElements)
        assertEquals(2, searchResponsePage0.totalPages)
        assertUpsertResponsesMatchRequests(searchResponsePage0.content, listOf(upsertRequests[0], upsertRequests[1]))

        val searchResponsePage1 = gateClient.businessParters.getBusinessPartnersInput(null, PaginationRequest(1, 2))
        assertEquals(3, searchResponsePage1.totalElements)
        assertEquals(2, searchResponsePage1.totalPages)
        assertUpsertResponsesMatchRequests(searchResponsePage1.content, listOf(upsertRequests[2]))

        val searchResponsePage2 = gateClient.businessParters.getBusinessPartnersInput(null, PaginationRequest(2, 2))
        assertEquals(3, searchResponsePage2.totalElements)
        assertEquals(2, searchResponsePage2.totalPages)
        assertEquals(0, searchResponsePage2.content.size)
    }

    private fun assertUpsertResponsesMatchRequests(responses: Collection<BusinessPartnerInputDto>, requests: List<BusinessPartnerInputRequest>) {
        Assertions.assertThat(responses)
            .usingRecursiveComparison()
            .ignoringFieldsOfTypes(Instant::class.java)
            .isEqualTo(requests.map(::toExpectedResponse))
    }

    private fun toExpectedResponse(request: BusinessPartnerInputRequest): BusinessPartnerInputDto {
        // same sorting order as defined for entity
        return BusinessPartnerInputDto(
            externalId = request.externalId,
            nameParts = request.nameParts,
            identifiers = request.identifiers.toSortedSet(identifierDtoComparator),
            states = request.states.toSortedSet(stateDtoComparator),
            roles = request.roles.toSortedSet(),
            isOwnCompanyData = request.isOwnCompanyData,
            legalEntity = request.legalEntity.copy(classifications = request.legalEntity.classifications.toSortedSet(classificationDtoComparator)),
            site = request.site,
            address = request.address,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    val identifierDtoComparator = compareBy(
        BusinessPartnerIdentifierDto::type,
        BusinessPartnerIdentifierDto::value,
        BusinessPartnerIdentifierDto::issuingBody
    )

    val stateDtoComparator = compareBy(nullsFirst(), BusinessPartnerStateDto::validFrom)       // here null means MIN
        .thenBy(nullsLast(), BusinessPartnerStateDto::validTo)        // here null means MAX
        .thenBy(BusinessPartnerStateDto::type)

    val classificationDtoComparator = compareBy(
        BusinessPartnerClassificationDto::type,
        BusinessPartnerClassificationDto::code,
        BusinessPartnerClassificationDto::value
    )

    private fun mockOrchestratorApi() {
        val taskCreateResponse =
            TaskCreateResponse(
                listOf(
                    TaskClientStateDto(
                        taskId = "0",
                        businessPartnerResult = null,
                        processingState = TaskProcessingStateDto(
                            resultState = ResultState.Pending,
                            step = TaskStep.CleanAndSync,
                            stepState = StepState.Queued,
                            errors = emptyList(),
                            createdAt = Instant.now(),
                            modifiedAt = Instant.now(),
                            timeout = Instant.now()
                        )
                    ),
                    TaskClientStateDto(
                        taskId = "1",
                        businessPartnerResult = null,
                        processingState = TaskProcessingStateDto(
                            resultState = ResultState.Pending,
                            step = TaskStep.CleanAndSync,
                            stepState = StepState.Queued,
                            errors = emptyList(),
                            createdAt = Instant.now(),
                            modifiedAt = Instant.now(),
                            timeout = Instant.now()
                        )
                    ),
                    TaskClientStateDto(
                        taskId = "2",
                        businessPartnerResult = null,
                        processingState = TaskProcessingStateDto(
                            resultState = ResultState.Pending,
                            step = TaskStep.CleanAndSync,
                            stepState = StepState.Queued,
                            errors = emptyList(),
                            createdAt = Instant.now(),
                            modifiedAt = Instant.now(),
                            timeout = Instant.now()
                        )
                    ),
                    TaskClientStateDto(
                        taskId = "3",
                        businessPartnerResult = null,
                        processingState = TaskProcessingStateDto(
                            resultState = ResultState.Pending,
                            step = TaskStep.CleanAndSync,
                            stepState = StepState.Queued,
                            errors = emptyList(),
                            createdAt = Instant.now(),
                            modifiedAt = Instant.now(),
                            timeout = Instant.now()
                        )
                    )
                )
            )

        // Orchestrator request new cleaning endpoint
        gateWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(ORCHESTRATOR_CREATE_TASKS_URL))
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(taskCreateResponse))
                )
        )
    }

    private fun mockOrchestratorApiCleaned() {
        val taskStateResponse =
            TaskStateResponse(
                listOf(
                    TaskClientStateDto(
                        taskId = "0",
                        businessPartnerResult = BusinessPartnerGenericValues.businessPartner1,
                        processingState = TaskProcessingStateDto(
                            resultState = ResultState.Success,
                            step = TaskStep.CleanAndSync,
                            stepState = StepState.Queued,
                            errors = emptyList(),
                            createdAt = Instant.now(),
                            modifiedAt = Instant.now(),
                            timeout = Instant.now()
                        )
                    ),
                    TaskClientStateDto(
                        taskId = "1",
                        businessPartnerResult = null,
                        processingState = TaskProcessingStateDto(
                            resultState = ResultState.Error,
                            step = TaskStep.CleanAndSync,
                            stepState = StepState.Queued,
                            errors = listOf(
                                TaskErrorDto(TaskErrorType.Timeout, "Major Error"),
                                TaskErrorDto(TaskErrorType.Unspecified, "Minor Error")
                            ),
                            createdAt = Instant.now(),
                            modifiedAt = Instant.now(),
                            timeout = Instant.now()
                        )
                    )
                )
            )

        gateWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(ORCHESTRATOR_SEARCH_TASK_STATES_URL)).willReturn(
                WireMock.okJson(objectMapper.writeValueAsString(taskStateResponse))
            )
        )
    }
    private fun mockOrchestratorApiCleanedResponseSizeOne() {
        val taskStateResponse = TaskStateResponse(
            listOf(
                TaskClientStateDto(
                    taskId = "0", businessPartnerResult = BusinessPartnerGenericValues.businessPartner1, processingState = TaskProcessingStateDto(
                        resultState = ResultState.Success,
                        step = TaskStep.CleanAndSync,
                        stepState = StepState.Queued,
                        errors = emptyList(),
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now(),
                        timeout = Instant.now()
                    )
                )
            )
        )
        gateWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(ORCHESTRATOR_SEARCH_TASK_STATES_URL))
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(taskStateResponse))
                )
        )
    }
    private fun mockPoolApiGetChangeLogs() {

        val poolChangelogEntries = PageDto(
            totalElements = 3,
            totalPages = 1,
            page = 0,
            contentSize = 3,
            content = listOf(
                ChangelogEntryResponse(
                    bpn = BusinessPartnerVerboseValues.bpOutputDtoCleaned.legalEntity.legalEntityBpn,
                    businessPartnerType = BusinessPartnerType.LEGAL_ENTITY,
                    timestamp = Instant.now(),
                    changelogType = ChangelogType.UPDATE
                ),
                ChangelogEntryResponse(
                    bpn = BusinessPartnerVerboseValues.bpOutputDtoCleaned.address.addressBpn,
                    businessPartnerType = BusinessPartnerType.ADDRESS,
                    timestamp = Instant.now(),
                    changelogType = ChangelogType.UPDATE
                ),
                ChangelogEntryResponse(
                    bpn = BusinessPartnerVerboseValues.bpOutputDtoCleaned.site!!.siteBpn!!,
                    businessPartnerType = BusinessPartnerType.SITE,
                    timestamp = Instant.now(),
                    changelogType = ChangelogType.UPDATE
                )
            )
        )
        // Pool APi get changelogs endpoint
        poolWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(POOL_API_SEARCH_CHANGE_LOG_URL))
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(poolChangelogEntries))
                )
        )
    }


    fun readSharingStates(businessPartnerType: BusinessPartnerType?, externalIds: Collection<String>?): Collection<SharingStateDto> {

        return gateClient.sharingState.getSharingStates(PaginationRequest(), businessPartnerType, externalIds).content
    }

    @Test
    fun `insert one business partners and finalize cleaning task without error`() {
        this.mockOrchestratorApiCleaned()

        val outputBusinessPartners = listOf(
            BusinessPartnerVerboseValues.bpOutputDtoCleaned
        )

        val upsertRequests = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestCleaned,
            BusinessPartnerNonVerboseValues.bpInputRequestError
        )
        upsertBusinessPartnersAndShare(upsertRequests)

        val externalId4 = BusinessPartnerNonVerboseValues.bpInputRequestCleaned.externalId
        val externalId5 = BusinessPartnerNonVerboseValues.bpInputRequestError.externalId

        val createdSharingState = listOf(
            SharingStateDto(
                businessPartnerType = BusinessPartnerType.GENERIC,
                externalId = externalId4,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                bpn = null,
                sharingProcessStarted = null,
                taskId = "0"
            ),
            SharingStateDto(
                businessPartnerType = BusinessPartnerType.GENERIC,
                externalId = externalId5,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                bpn = null,
                sharingProcessStarted = null,
                taskId = "1"
            )
        )

        //Firstly verifies if the Sharing States was created for new Business Partners
        val externalIds = listOf(externalId4, externalId5)
        val upsertSharingStateResponses = readSharingStates(BusinessPartnerType.GENERIC, externalIds)
        testHelpers
            .assertRecursively(upsertSharingStateResponses)
            .ignoringFieldsMatchingRegexes(".*${SharingStateDto::sharingProcessStarted.name}")
            .isEqualTo(createdSharingState)

        // Call Finish Cleaning Method
        goldenRecordTaskService.resolvePendingTasks()

        val cleanedSharingState = listOf(
            SharingStateDto(
                businessPartnerType = BusinessPartnerType.GENERIC,
                externalId = externalId4,
                sharingStateType = SharingStateType.Success,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                bpn = BusinessPartnerGenericValues.businessPartner1.address.addressBpn,
                sharingProcessStarted = null,
                taskId = "0"
            ),
            SharingStateDto(
                businessPartnerType = BusinessPartnerType.GENERIC,
                externalId = externalId5,
                sharingStateType = SharingStateType.Error,
                sharingErrorCode = BusinessPartnerSharingError.SharingProcessError,
                sharingErrorMessage = "Major Error // Minor Error",
                bpn = null,
                sharingProcessStarted = null,
                taskId = "1"
            )
        )

        //Check for both Sharing State changes (Error and Success)
        val readCleanedSharingState = readSharingStates(BusinessPartnerType.GENERIC, externalIds)
        testHelpers.assertRecursively(readCleanedSharingState)
            .ignoringFieldsMatchingRegexes(".*${SharingStateDto::sharingProcessStarted.name}")
            .isEqualTo(cleanedSharingState)

        //Assert that Cleaned Golden Record is persisted in the Output correctly
        val searchResponsePage = gateClient.businessParters.getBusinessPartnersOutput(listOf(externalId4))
        assertUpsertOutputResponsesMatchRequests(searchResponsePage.content, outputBusinessPartners)

    }

    @Test
    fun `insert one business partners but task is missing in orchestrator`() {
        this.mockOrchestratorApiCleaned()
        val upsertRequests = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestCleaned,
            BusinessPartnerNonVerboseValues.bpInputRequestError,
            BusinessPartnerNonVerboseValues.bpInputRequestChina,
        )
        upsertBusinessPartnersAndShare(upsertRequests)

        val externalId3 = BusinessPartnerNonVerboseValues.bpInputRequestChina.externalId

        val createdSharingState = listOf(
            SharingStateDto(
                businessPartnerType = BusinessPartnerType.GENERIC,
                externalId = externalId3,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                bpn = null,
                sharingProcessStarted = null,
                taskId = "2"
            )
        )

        //Firstly verifies if the Sharing States was created for new Business Partner
        val externalIds = listOf(externalId3)
        val upsertSharingStateResponses = readSharingStates(BusinessPartnerType.GENERIC, externalIds)
        testHelpers
            .assertRecursively(upsertSharingStateResponses)
            .ignoringFieldsMatchingRegexes(".*${SharingStateDto::sharingProcessStarted.name}")
            .isEqualTo(createdSharingState)

        // Call Finish Cleaning Method
        goldenRecordTaskService.resolvePendingTasks()

        val cleanedSharingState = listOf(
            SharingStateDto(
                businessPartnerType = BusinessPartnerType.GENERIC,
                externalId = externalId3,
                sharingStateType = SharingStateType.Error,
                sharingErrorCode = BusinessPartnerSharingError.MissingTaskID,
                sharingErrorMessage = "Missing Task in Orchestrator",
                bpn = null,
                sharingProcessStarted = null,
                taskId = "2"
            )
        )

        //Check for Sharing State
        val readCleanedSharingState = readSharingStates(BusinessPartnerType.GENERIC, externalIds)
        testHelpers.assertRecursively(readCleanedSharingState)
            .ignoringFieldsMatchingRegexes(".*${SharingStateDto::sharingProcessStarted.name}")
            .isEqualTo(cleanedSharingState)

    }

    @Test
    fun `insert one business partners and request cleaning task for bpn update`() {

        this.mockPoolApiGetChangeLogs()
        this.mockOrchestratorApiCleanedResponseSizeOne()

        val outputBusinessPartners = listOf(
            BusinessPartnerVerboseValues.bpOutputDtoCleaned
        )
        val upsertRequests = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestCleaned
        )
        upsertBusinessPartnersAndShare(upsertRequests)

        val externalId4 = BusinessPartnerNonVerboseValues.bpInputRequestCleaned.externalId

        goldenRecordTaskService.resolvePendingTasks()

        goldenRecordTaskService.createTasksForGoldenRecordUpdates()

        val upsertSharingStatesRequests = listOf(
            SharingStateDto(
                businessPartnerType = BusinessPartnerType.GENERIC,
                externalId = externalId4,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                bpn = "000000123CCC333",
                sharingProcessStarted = null,
                taskId = "0"
            )
        )
        val externalIds = listOf(externalId4)
        val upsertSharingStateResponses = readSharingStates(BusinessPartnerType.GENERIC, externalIds)

        //Assert that Cleaned Golden Record is persisted in the Output correctly
        val searchResponsePage = gateClient.businessParters.getBusinessPartnersOutput(listOf(externalId4))
        assertUpsertOutputResponsesMatchRequests(searchResponsePage.content, outputBusinessPartners)

        //Assert that sharing state are created
        testHelpers.assertRecursively(upsertSharingStateResponses).ignoringFieldsMatchingRegexes(".*${SharingStateDto::sharingProcessStarted.name}")
            .isEqualTo(upsertSharingStatesRequests)
    }

    private fun assertUpsertOutputResponsesMatchRequests(responses: Collection<BusinessPartnerOutputDto>, requests: List<BusinessPartnerOutputDto>) {
        Assertions.assertThat(responses)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFieldsOfTypes(Instant::class.java)
            .isEqualTo(requests)
    }

    private fun BusinessPartnerInputRequest.fastCopy(externalId: String, shortName: String) =
        copy(externalId = externalId, legalEntity = legalEntity.copy(shortName = shortName))

    private fun upsertBusinessPartnersAndShare(partners: List<BusinessPartnerInputRequest>) {
        gateClient.businessParters.upsertBusinessPartnersInput(partners)
        gateClient.sharingState.postSharingStateReady(PostSharingStateReadyRequest(partners.map { it.externalId }))
        goldenRecordTaskService.createTasksForReadyBusinessPartners()
    }

    private fun assertBusinessPartnersUpsertedCorrectly(upsertedBusinessPartners: Collection<BusinessPartnerInputDto>) {
        val searchResponsePage = gateClient.businessParters.getBusinessPartnersInput(null)
        assertEquals(upsertedBusinessPartners.size.toLong(), searchResponsePage.totalElements)
        testHelpers.assertRecursively(searchResponsePage.content).isEqualTo(upsertedBusinessPartners)

        val sharingStateResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), businessPartnerType = null, externalIds = null)
        assertEquals(upsertedBusinessPartners.size.toLong(), sharingStateResponse.totalElements)
        Assertions.assertThat(sharingStateResponse.content).isEqualTo(
            upsertedBusinessPartners.map {
                SharingStateDto(
                    businessPartnerType = BusinessPartnerType.GENERIC,
                    externalId = it.externalId,
                    sharingStateType = SharingStateType.Initial
                )
            }
        )
    }
}
