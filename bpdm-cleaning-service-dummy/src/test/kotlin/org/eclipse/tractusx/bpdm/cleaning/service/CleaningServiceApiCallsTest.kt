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

package org.eclipse.tractusx.bpdm.cleaning.service


import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.eclipse.tractusx.bpdm.cleaning.testdata.CommonValues.businessPartnerWithBpnA
import org.eclipse.tractusx.bpdm.cleaning.testdata.CommonValues.fixedTaskId
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CleaningServiceApiCallsTest @Autowired constructor(
    val cleaningServiceDummy: CleaningServiceDummy,
    val jacksonObjectMapper: ObjectMapper,
    val orchestrationApiClient: OrchestrationApiClient
) {

    companion object {
        const val ORCHESTRATOR_RESERVE_TASKS_URL = "/api/golden-record-tasks/step-reservations"
        const val ORCHESTRATOR_RESOLVE_TASKS_URL = "/api/golden-record-tasks/step-results"

        @JvmField
        @RegisterExtension
        val orchestratorMockApi: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.client.orchestrator.base-url") { orchestratorMockApi.baseUrl() }

        }
    }

    @BeforeEach
    fun beforeEach() {
        orchestratorMockApi.resetAll()
        this.mockOrchestratorResolveApi()
        this.mockOrchestratorReserveApi()
    }


    @Test
    fun `pollForCleaningTasks should reserve and resolve tasks from orchestrator`() {
        // Call the method under test
        cleaningServiceDummy.pollForCleaningTasks()

        // Verify that the reserve API was called once (using WireMock)
        orchestratorMockApi.verify(postRequestedFor(urlEqualTo(ORCHESTRATOR_RESERVE_TASKS_URL)).withRequestBody(matchingJsonPath("$.amount", equalTo("10"))))

        // Verify that the resolve API was called once (using WireMock)
        orchestratorMockApi.verify(postRequestedFor(urlEqualTo(ORCHESTRATOR_RESOLVE_TASKS_URL)))
    }

    @Test
    fun `reserveTasksForStep should return expected response`() {
        val expectedResponse = jacksonObjectMapper.writeValueAsString(createSampleTaskStepReservationResponse(businessPartnerWithBpnA))

        val result = orchestrationApiClient.goldenRecordTasks.reserveTasksForStep(
            TaskStepReservationRequest(amount = 10, TaskStep.Clean)
        )

        // Assert the expected result
        val expectedResult = jacksonObjectMapper.readValue(expectedResponse, result::class.java) // Convert the expected JSON response to your DTO
        assertEquals(expectedResult, result)

        orchestrationApiClient.goldenRecordTasks.resolveStepResults(
            TaskStepResultRequest(TaskStep.Clean, emptyList())
        )
    }


    fun mockOrchestratorReserveApi() {
        // Orchestrator reserve
        orchestratorMockApi.stubFor(
            post(urlPathEqualTo(ORCHESTRATOR_RESERVE_TASKS_URL))
                .willReturn(
                    okJson(jacksonObjectMapper.writeValueAsString(createSampleTaskStepReservationResponse(businessPartnerWithBpnA)))
                )
        )
    }

    fun mockOrchestratorResolveApi() {
        // Orchestrator resolve
        orchestratorMockApi.stubFor(
            post(urlPathEqualTo(ORCHESTRATOR_RESOLVE_TASKS_URL))
                .willReturn(aResponse().withStatus(200))
        )
    }

    // Helper method to create a sample TaskStepReservationResponse
    private fun createSampleTaskStepReservationResponse(businessPartnerGenericDto: BusinessPartnerGenericDto): TaskStepReservationResponse {
        val fullDto = BusinessPartnerFullDto(businessPartnerGenericDto)
        return TaskStepReservationResponse(listOf(TaskStepReservationEntryDto(fixedTaskId, fullDto)), Instant.MIN)
    }

}