/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerIdentifierDto
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerStateDto
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.ClassificationDto
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.util.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.gate.util.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.gate.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.gate.util.PostgreSQLContextInitializer
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
    val objectMapper: ObjectMapper
) {
    companion object {
        const val ORCHESTRATOR_CREATE_TASKS_URL = "/api/golden-record-tasks"

        @JvmField
        @RegisterExtension
        val gateWireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.orchestrator.base-url") { gateWireMockServer.baseUrl() }
        }
    }

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        gateWireMockServer.resetAll()
        this.mockOrchestratorApi()
    }

    @Test
    fun `insert minimal business partner`() {


        val upsertRequests = listOf(BusinessPartnerNonVerboseValues.bpInputRequestMinimal)
        val upsertResponses = gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests).body!!
        assertUpsertResponsesMatchRequests(upsertResponses, upsertRequests)

        val searchResponsePage = gateClient.businessParters.getBusinessPartnersInput(null)
        assertEquals(1, searchResponsePage.totalElements)
        testHelpers.assertRecursively(searchResponsePage.content).isEqualTo(upsertResponses)
    }

    @Test
    fun `insert three business partners`() {
        val upsertRequests = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestFull,
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal,
            BusinessPartnerNonVerboseValues.bpInputRequestChina
        )
        val upsertResponses = gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests).body!!
        assertUpsertResponsesMatchRequests(upsertResponses, upsertRequests)

        val searchResponsePage = gateClient.businessParters.getBusinessPartnersInput(null)
        assertEquals(3, searchResponsePage.totalElements)
        testHelpers.assertRecursively(searchResponsePage.content).isEqualTo(upsertResponses)
    }

    @Test
    fun `insert three business partners and check sharing state is pending and has taskid`() {
        val upsertRequests = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestFull,
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal,
            BusinessPartnerNonVerboseValues.bpInputRequestChina
        )
        gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests).body!!

        val externalId1 = BusinessPartnerNonVerboseValues.bpInputRequestFull.externalId
        val externalId2 = BusinessPartnerNonVerboseValues.bpInputRequestMinimal.externalId
        val externalId3 = BusinessPartnerNonVerboseValues.bpInputRequestChina.externalId

        val externalIds = listOf(externalId1, externalId2, externalId3)

        val upsertSharingStatesRequests = listOf(
            SharingStateDto(
                businessPartnerType = BusinessPartnerType.ADDRESS,
                externalId = externalId1,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                bpn = null,
                sharingProcessStarted = null,
                taskId = "0"
            ),
            SharingStateDto(
                businessPartnerType = BusinessPartnerType.ADDRESS,
                externalId = externalId2,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                bpn = null,
                sharingProcessStarted = null,
                taskId = "1"
            ),
            SharingStateDto(
                businessPartnerType = BusinessPartnerType.ADDRESS,
                externalId = externalId3,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                bpn = null,
                sharingProcessStarted = null,
                taskId = "2"
            )
        )

        val upsertSharingStateResponses = readSharingStates(BusinessPartnerType.ADDRESS, externalIds)


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
            BusinessPartnerNonVerboseValues.bpInputRequestFull.copy(externalId = BusinessPartnerVerboseValues.externalId1, shortName = "1"),
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal.copy(externalId = BusinessPartnerVerboseValues.externalId2, shortName = "2"),
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal.copy(externalId = BusinessPartnerVerboseValues.externalId3, shortName = "3")
        )
        gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests)

        val searchResponsePage =
            gateClient.businessParters.getBusinessPartnersInput(listOf(BusinessPartnerVerboseValues.externalId1, BusinessPartnerVerboseValues.externalId3))
        assertUpsertResponsesMatchRequests(searchResponsePage.content, listOf(upsertRequests[0], upsertRequests[2]))
    }

    @Test
    fun `query business partners by missing externalId`() {
        val upsertRequests = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestFull.copy(externalId = BusinessPartnerVerboseValues.externalId1, shortName = "1"),
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal.copy(externalId = BusinessPartnerVerboseValues.externalId2, shortName = "2"),
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal.copy(externalId = BusinessPartnerVerboseValues.externalId3, shortName = "3")
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
            BusinessPartnerNonVerboseValues.bpInputRequestFull.copy(
                externalId = BusinessPartnerNonVerboseValues.bpInputRequestFull.externalId,
                shortName = "1"
            ),
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal.copy(externalId = BusinessPartnerVerboseValues.externalId2, shortName = "2"),
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal.copy(externalId = BusinessPartnerVerboseValues.externalId3, shortName = "3")
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
            shortName = request.shortName,
            identifiers = request.identifiers.toSortedSet(identifierDtoComparator),
            legalForm = request.legalForm,
            states = request.states.toSortedSet(stateDtoComparator),
            classifications = request.classifications.toSortedSet(classificationDtoComparator),
            roles = request.roles.toSortedSet(),
            postalAddress = request.postalAddress,
            isOwnCompanyData = request.isOwnCompanyData,
            bpnL = request.bpnL,
            bpnS = request.bpnS,
            bpnA = request.bpnA,
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
        .thenBy(BusinessPartnerStateDto::description)
    val classificationDtoComparator = compareBy(
        ClassificationDto::type,
        ClassificationDto::code,
        ClassificationDto::value
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

    fun readSharingStates(businessPartnerType: BusinessPartnerType?, externalIds: Collection<String>?): Collection<SharingStateDto> {

        return gateClient.sharingState.getSharingStates(PaginationRequest(), businessPartnerType, externalIds).content
    }

}
