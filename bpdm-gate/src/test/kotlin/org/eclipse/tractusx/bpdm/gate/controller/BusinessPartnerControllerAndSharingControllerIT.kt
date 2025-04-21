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
import org.eclipse.tractusx.bpdm.gate.service.TaskCreationChunkService
import org.eclipse.tractusx.bpdm.gate.service.TaskResolutionChunkService
import org.eclipse.tractusx.bpdm.gate.util.MockAndAssertUtils
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.gate.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.test.util.AssertHelpers
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.orchestrator.api.model.ResultState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.OffsetDateTime
import java.time.ZoneOffset


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
    val taskCreationService: TaskCreationChunkService,
    val taskResolutionService: TaskResolutionChunkService,
    val mockAndAssertUtils: MockAndAssertUtils
) {
    private val anyTime = OffsetDateTime.of(2025, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC).toInstant()

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
                taskId = "0",
                updatedAt = anyTime
            ),
            SharingStateDto(
                externalId = externalId2,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                sharingProcessStarted = null,
                taskId = "1",
                updatedAt = anyTime
            ),
            SharingStateDto(
                externalId = externalId3,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                sharingProcessStarted = null,
                taskId = "2",
                updatedAt = anyTime
            )
        )

        val upsertSharingStateResponses = this.mockAndAssertUtils.readSharingStates(externalIds)


        assertHelpers.assertRecursively(upsertSharingStateResponses).isEqualTo(upsertSharingStatesRequests)

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
                taskId = "2",
                updatedAt = anyTime
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
        taskResolutionService.healthCheck(0)

        val cleanedSharingState = listOf(
            SharingStateDto(
                externalId = externalId3,
                sharingStateType = SharingStateType.Error,
                sharingErrorCode = BusinessPartnerSharingError.MissingTaskID,
                sharingErrorMessage = "Missing Task in Orchestrator",
                sharingProcessStarted = null,
                taskId = "2",
                updatedAt = anyTime
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
