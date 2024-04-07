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
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.admin.model.ServeEventQuery
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.cleaning.config.OrchestratorConfigProperties
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.*
import org.eclipse.tractusx.orchestrator.api.GoldenRecordTaskApi
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

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["${OrchestratorConfigProperties.PREFIX}.security-enabled=false"]
)
@ActiveProfiles("test")
class CleaningServiceApiCallsTest @Autowired constructor(
    val cleaningServiceDummy: CleaningServiceDummy,
    val jacksonObjectMapper: ObjectMapper
) {

    companion object {
        const val ORCHESTRATOR_RESERVE_TASKS_URL = "${GoldenRecordTaskApi.TASKS_PATH}/step-reservations"
        const val ORCHESTRATOR_RESOLVE_TASKS_URL = "${GoldenRecordTaskApi.TASKS_PATH}/step-results"

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

    val fixedTaskId = "1"
    val businessPartnerFactory = BusinessPartnerTestDataFactory()
    val defaultBpnRequest = BpnReference("IGNORED", null, BpnReferenceType.BpnRequestIdentifier)

    @BeforeEach
    fun beforeEach() {
        orchestratorMockApi.resetAll()
    }

    @Test
    fun `Additional Address with Site without BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithBpnReferences(BpnReference.empty)
            .copyWithLegalAddress(PostalAddress.empty)
            .copyWithSiteMainAddress(PostalAddress.empty)

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithBpnReferences(defaultBpnRequest)
                .copyWithLegalAddress(mockedBusinessPartner.uncategorized.address!!)
                .copyWithSiteMainAddress(mockedBusinessPartner.uncategorized.address!!)
                .copyWithConfidenceCriteria(cleaningServiceDummy.dummyConfidenceCriteria)
                .copyWithHasChanged(false, false, true)
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertIsEqualIgnoreReferenceValue(actualResponse, expectedResponse)
    }

    @Test
    fun `Additional Address with Site with BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithLegalAddress(PostalAddress.empty)
            .copyWithSiteMainAddress(PostalAddress.empty)

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithLegalAddress(mockedBusinessPartner.uncategorized.address!!)
                .copyWithSiteMainAddress(mockedBusinessPartner.uncategorized.address!!)
                .copyWithConfidenceCriteria(cleaningServiceDummy.dummyConfidenceCriteria)
                .copyWithHasChanged(false, false, true)
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse)
    }

    @Test
    fun `Additional Address without Site without BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithBpnReferences(BpnReference.empty)
            .copyWithLegalAddress(PostalAddress.empty)
            .copy(site = null)

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithBpnReferences(defaultBpnRequest)
                .copyWithLegalAddress(mockedBusinessPartner.uncategorized.address!!)
                .copyWithConfidenceCriteria(cleaningServiceDummy.dummyConfidenceCriteria)
                .copyWithHasChanged(false, false, true)
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertIsEqualIgnoreReferenceValue(actualResponse, expectedResponse)
    }

    @Test
    fun `Additional Address without Site with BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithLegalAddress(PostalAddress.empty)
            .copy(site = null)

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithLegalAddress(mockedBusinessPartner.uncategorized.address!!)
                .copyWithConfidenceCriteria(cleaningServiceDummy.dummyConfidenceCriteria)
                .copyWithHasChanged(false, false, true)
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse)
    }

    @Test
    fun `Site with Own Main Address and without BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithBpnReferences(BpnReference.empty)
            .copyWithLegalAddress(PostalAddress.empty)
            .copy(additionalAddress = null)

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithBpnReferences(defaultBpnRequest)
                .copyWithLegalAddress(mockedBusinessPartner.uncategorized.address!!)
                .copyWithConfidenceCriteria(cleaningServiceDummy.dummyConfidenceCriteria)
                .copyWithHasChanged(false, true, false)
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertIsEqualIgnoreReferenceValue(actualResponse, expectedResponse)
    }

    @Test
    fun `Site With Own Main Address and BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithLegalAddress(PostalAddress.empty)
            .copy(additionalAddress = null)

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithLegalAddress(mockedBusinessPartner.uncategorized.address!!)
                .copyWithConfidenceCriteria(cleaningServiceDummy.dummyConfidenceCriteria)
                .copyWithHasChanged(false, true, false)
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse)
    }

    @Test
    fun `Site With Legal Address As Main Address And Without BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithBpnReferences(BpnReference.empty)
            .copyWithSiteMainAddress(null)
            .copy(additionalAddress = null)

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithBpnReferences(defaultBpnRequest)
                .copyWithConfidenceCriteria(cleaningServiceDummy.dummyConfidenceCriteria)
                .copyWithHasChanged(false, true, false)
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertIsEqualIgnoreReferenceValue(actualResponse, expectedResponse)
    }

    @Test
    fun `Site With Legal Address As Main Address And With BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithSiteMainAddress(null)
            .copy(additionalAddress = null)

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithConfidenceCriteria(cleaningServiceDummy.dummyConfidenceCriteria)
                .copyWithHasChanged(false, true, false)
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse)
    }

    @Test
    fun `Legal Entity without BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithBpnReferences(BpnReference.empty)
            .copyWithLegalAddress(PostalAddress.empty)
            .copy(site = null, additionalAddress = null)

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithBpnReferences(defaultBpnRequest)
                .copyWithLegalAddress(mockedBusinessPartner.uncategorized.address!!)
                .copyWithConfidenceCriteria(cleaningServiceDummy.dummyConfidenceCriteria)
                .copyWithHasChanged(true, false, false)
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertIsEqualIgnoreReferenceValue(actualResponse, expectedResponse)
    }

    @Test
    fun `Legal Entity with BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithLegalAddress(PostalAddress.empty)
            .copy(site = null, additionalAddress = null)

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithLegalAddress(mockedBusinessPartner.uncategorized.address!!)
                .copyWithConfidenceCriteria(cleaningServiceDummy.dummyConfidenceCriteria)
                .copyWithHasChanged(true, false, false)
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse)
    }



    fun mockOrchestratorReserveApi(businessPartner: BusinessPartner): StubMapping {
        // Orchestrator reserve
        return orchestratorMockApi.stubFor(
            post(urlPathEqualTo(ORCHESTRATOR_RESERVE_TASKS_URL))
                .willReturn(
                    okJson(jacksonObjectMapper.writeValueAsString(createSampleTaskStepReservationResponse(businessPartner)))
                )
        )
    }

    fun mockOrchestratorResolveApi(): StubMapping {
        // Orchestrator resolve
        return orchestratorMockApi.stubFor(
            post(urlPathEqualTo(ORCHESTRATOR_RESOLVE_TASKS_URL))
                .willReturn(aResponse().withStatus(200))
        )
    }

    // Helper method to create a sample TaskStepReservationResponse
    private fun createSampleTaskStepReservationResponse(businessPartner: BusinessPartner): TaskStepReservationResponse {
        return TaskStepReservationResponse(listOf(TaskStepReservationEntryDto(fixedTaskId, businessPartner)), Instant.MIN)
    }


    private fun getResolveResult(stubMapping: StubMapping): TaskStepResultRequest{
        val serveEvents = orchestratorMockApi.getServeEvents(ServeEventQuery.forStubMapping(stubMapping)).requests
        assertEquals(serveEvents.size, 1)
        val actualRequest = serveEvents.first()!!.request
        return jacksonObjectMapper.readValue<TaskStepResultRequest>(actualRequest.body)
    }

    private fun assertIsEqualIgnoreReferenceValue(actual: TaskStepResultRequest, expected: TaskStepResultRequest){
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(
                ".*.${BpnReference::referenceValue.name}"
            )
            .isEqualTo(expected)
    }

    private fun createResultRequest(expectedBusinessPartner: BusinessPartner): TaskStepResultRequest {
        return TaskStepResultRequest(
            TaskStep.CleanAndSync, listOf(
                TaskStepResultEntryDto(
                    fixedTaskId,
                    expectedBusinessPartner,
                    emptyList()
                )
            )
        )
    }


}