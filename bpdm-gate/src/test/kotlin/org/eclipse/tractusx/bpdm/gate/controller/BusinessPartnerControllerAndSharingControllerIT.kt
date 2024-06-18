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
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.PostSharingStateReadyRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.service.TaskCreationService
import org.eclipse.tractusx.bpdm.gate.service.TaskResolutionService
import org.eclipse.tractusx.bpdm.gate.util.MockAndAssertUtils
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.gate.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.gate.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.test.util.AssertHelpers
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["bpdm.api.upsert-limit=3","bpdm.tasks.creation.fromSharingMember.starts-as-ready=false"]
)
@ActiveProfiles("test-no-auth")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class BusinessPartnerControllerAndSharingControllerIT @Autowired constructor(
    val testHelpers: DbTestHelpers,
    val assertHelpers: AssertHelpers,
    val gateClient: GateClient,
    val taskCreationService: TaskCreationService,
    val taskResolutionService: TaskResolutionService,
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
        gateWireMockServer.resetAll();
        poolWireMockServer.resetAll()
        this.mockAndAssertUtils.mockOrchestratorApi(gateWireMockServer)
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
    fun `insert one business partners and finalize cleaning task without error`() {
        this.mockAndAssertUtils.mockOrchestratorApiCleaned(gateWireMockServer)

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
                sharingErrorCode = BusinessPartnerSharingError.SharingProcessError,
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

        // Call Finish Cleaning Method
        taskResolutionService.resolveTasks()

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

    fun upsertBusinessPartnersAndShare(partners: List<BusinessPartnerInputRequest>) {
        gateClient.businessParters.upsertBusinessPartnersInput(partners)
        gateClient.sharingState.postSharingStateReady(PostSharingStateReadyRequest(partners.map { it.externalId }))
        taskCreationService.createTasksForReadyBusinessPartners()
    }



}
