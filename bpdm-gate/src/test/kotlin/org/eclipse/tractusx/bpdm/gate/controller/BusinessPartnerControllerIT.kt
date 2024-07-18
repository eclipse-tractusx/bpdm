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

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.service.TaskCreationChunkService
import org.eclipse.tractusx.bpdm.gate.service.TaskResolutionChunkService
import org.eclipse.tractusx.bpdm.gate.util.MockAndAssertUtils
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.gate.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.gate.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.test.util.AssertHelpers
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.orchestrator.api.model.ResultState
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

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["bpdm.api.upsert-limit=3"]
)
@ActiveProfiles("test-no-auth")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class BusinessPartnerControllerIT @Autowired constructor(
    val testHelpers: DbTestHelpers,
    val assertHelpers: AssertHelpers,
    val gateClient: GateClient,
    val taskCreationService: TaskCreationChunkService,
    val taskResolutionService: TaskResolutionChunkService,
    val mockAndAssertUtils: MockAndAssertUtils
) {

    companion object {

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
        this.mockAndAssertUtils.mockOrchestratorApi(gateWireMockServer)
    }

    @Test
    fun `insert minimal business partner`() {

        val upsertRequests = listOf(BusinessPartnerNonVerboseValues.bpInputRequestMinimal)
        val upsertResponses = gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests).body!!
        this.mockAndAssertUtils.assertUpsertResponsesMatchRequests(upsertResponses, upsertRequests)

        this.mockAndAssertUtils.assertBusinessPartnersUpsertedCorrectly(upsertResponses)
    }

    @Test
    fun `insert three business partners`() {
        val upsertRequests = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestFull,
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal,
            BusinessPartnerNonVerboseValues.bpInputRequestChina
        )
        val upsertResponses = gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests).body!!

        this.mockAndAssertUtils.assertBusinessPartnersUpsertedCorrectly(upsertResponses)
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
                externalId = externalId1,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                sharingProcessStarted = null,
                taskId = "0"
            ),
            SharingStateDto(
                externalId = externalId2,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                sharingProcessStarted = null,
                taskId = "1"
            ),
            SharingStateDto(
                externalId = externalId3,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                sharingProcessStarted = null,
                taskId = "2"
            )
        )

        val upsertSharingStateResponses = this.mockAndAssertUtils.readSharingStates(externalIds)


        assertHelpers.assertRecursively(upsertSharingStateResponses).isEqualTo(upsertSharingStatesRequests)

    }


    @Test
    fun `insert and then update business partner`() {

        val insertRequests = listOf(BusinessPartnerNonVerboseValues.bpInputRequestMinimal)
        val externalId = insertRequests.first().externalId
        val insertResponses = gateClient.businessParters.upsertBusinessPartnersInput(insertRequests).body!!
        this.mockAndAssertUtils.assertUpsertResponsesMatchRequests(insertResponses, insertRequests)

        val searchResponse1Page = gateClient.businessParters.getBusinessPartnersInput(null)
        assertHelpers.assertRecursively(searchResponse1Page.content).isEqualTo(insertResponses)

        val updateRequests = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestFull.copy(externalId = externalId)
        )
        val updateResponses = gateClient.businessParters.upsertBusinessPartnersInput(updateRequests).body!!
        this.mockAndAssertUtils.assertUpsertResponsesMatchRequests(updateResponses, updateRequests)

        val searchResponse2Page = gateClient.businessParters.getBusinessPartnersInput(null)
        assertHelpers.assertRecursively(searchResponse2Page.content).isEqualTo(updateResponses)
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
        this.mockAndAssertUtils.assertUpsertResponsesMatchRequests(searchResponsePage.content, listOf(upsertRequests[0], upsertRequests[2]))
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
        this.mockAndAssertUtils.assertUpsertResponsesMatchRequests(searchResponsePage.content, listOf(upsertRequests[1]))
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
        this.mockAndAssertUtils.assertUpsertResponsesMatchRequests(searchResponsePage0.content, listOf(upsertRequests[0], upsertRequests[1]))

        val searchResponsePage1 = gateClient.businessParters.getBusinessPartnersInput(null, PaginationRequest(1, 2))
        assertEquals(3, searchResponsePage1.totalElements)
        assertEquals(2, searchResponsePage1.totalPages)
        this.mockAndAssertUtils.assertUpsertResponsesMatchRequests(searchResponsePage1.content, listOf(upsertRequests[2]))

        val searchResponsePage2 = gateClient.businessParters.getBusinessPartnersInput(null, PaginationRequest(2, 2))
        assertEquals(3, searchResponsePage2.totalElements)
        assertEquals(2, searchResponsePage2.totalPages)
        assertEquals(0, searchResponsePage2.content.size)
    }






    @Test
    fun `insert one business partners and finalize cleaning task without error`() {
        this.mockAndAssertUtils.mockOrchestratorApiCleaned(gateWireMockServer)

        // Expect outputBusinessPartner without identifiers and states as there are no Address identifier and states provided.
        val outputBusinessPartners = listOf(
            BusinessPartnerVerboseValues.bpOutputDtoCleaned.copy(identifiers = emptyList(), states = emptyList())
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
                externalId = externalId4,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                sharingProcessStarted = null,
                taskId = "0"
            ),
            SharingStateDto(
                externalId = externalId5,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                sharingProcessStarted = null,
                taskId = "1"
            )
        )

        //Firstly verifies if the Sharing States was created for new Business Partners
        val externalIds = listOf(externalId4, externalId5)
        val upsertSharingStateResponses = this.mockAndAssertUtils.readSharingStates(externalIds)
        assertHelpers
            .assertRecursively(upsertSharingStateResponses)
            .ignoringFieldsMatchingRegexes(".*${SharingStateDto::sharingProcessStarted.name}")
            .isEqualTo(createdSharingState)

        // Call Finish Cleaning Method
        taskResolutionService.resolveTasks()

        val cleanedSharingState = listOf(
            SharingStateDto(
                externalId = externalId4,
                sharingStateType = SharingStateType.Success,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                sharingProcessStarted = null,
                taskId = "0"
            ),
            SharingStateDto(
                externalId = externalId5,
                sharingStateType = SharingStateType.Error,
                sharingErrorCode = BusinessPartnerSharingError.SharingTimeout,
                sharingErrorMessage = "Major Error // Minor Error",
                sharingProcessStarted = null,
                taskId = "1"
            )
        )

        //Check for both Sharing State changes (Error and Success)
        val readCleanedSharingState = this.mockAndAssertUtils.readSharingStates(externalIds)
        assertHelpers.assertRecursively(readCleanedSharingState)
            .ignoringFieldsMatchingRegexes(".*${SharingStateDto::sharingProcessStarted.name}")
            .isEqualTo(cleanedSharingState)

        //Assert that Cleaned Golden Record is persisted in the Output correctly
        val searchResponsePage = gateClient.businessParters.getBusinessPartnersOutput(listOf(externalId4))
        this.mockAndAssertUtils.assertUpsertOutputResponsesMatchRequests(searchResponsePage.content, outputBusinessPartners)

    }

    @Test
    fun `insert one business partners but task is missing in orchestrator`() {
        this.mockAndAssertUtils.mockOrchestratorApiCleaned(gateWireMockServer)
        this.mockAndAssertUtils.mockOrchestratorApiResultStates(gateWireMockServer, listOf(ResultState.Pending, ResultState.Pending, null))
        val upsertRequests = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestCleaned,
            BusinessPartnerNonVerboseValues.bpInputRequestError,
            BusinessPartnerNonVerboseValues.bpInputRequestChina,
        )
        upsertBusinessPartnersAndShare(upsertRequests)

        val externalId3 = BusinessPartnerNonVerboseValues.bpInputRequestChina.externalId

        val createdSharingState = listOf(
            SharingStateDto(
                externalId = externalId3,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                sharingProcessStarted = null,
                taskId = "2"
            )
        )

        //Firstly verifies if the Sharing States was created for new Business Partner
        val externalIds = listOf(externalId3)
        val upsertSharingStateResponses = this.mockAndAssertUtils.readSharingStates(externalIds)
        assertHelpers
            .assertRecursively(upsertSharingStateResponses)
            .ignoringFieldsMatchingRegexes(".*${SharingStateDto::sharingProcessStarted.name}")
            .isEqualTo(createdSharingState)


        // Call Health Check
        taskResolutionService.healthCheck(0)

        val cleanedSharingState = listOf(
            SharingStateDto(
                externalId = externalId3,
                sharingStateType = SharingStateType.Error,
                sharingErrorCode = BusinessPartnerSharingError.MissingTaskID,
                sharingErrorMessage = "Missing Task in Orchestrator",
                sharingProcessStarted = null,
                taskId = "2"
            )
        )

        //Check for Sharing State
        val readCleanedSharingState = this.mockAndAssertUtils.readSharingStates(externalIds)
        assertHelpers.assertRecursively(readCleanedSharingState)
            .ignoringFieldsMatchingRegexes(".*${SharingStateDto::sharingProcessStarted.name}")
            .isEqualTo(cleanedSharingState)

    }

    private fun upsertBusinessPartnersAndShare(partners: List<BusinessPartnerInputRequest>) {
        gateClient.businessParters.upsertBusinessPartnersInput(partners)
        taskCreationService.createTasksForReadyBusinessPartners()
    }
    private fun BusinessPartnerInputRequest.fastCopy(externalId: String, shortName: String) =
        copy(externalId = externalId, legalEntity = legalEntity.copy(shortName = shortName))

}
