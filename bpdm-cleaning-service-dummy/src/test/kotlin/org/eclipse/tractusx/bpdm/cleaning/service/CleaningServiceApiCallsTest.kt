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

package org.eclipse.tractusx.bpdm.cleaning.service


import com.github.tomakehurst.wiremock.admin.model.ServeEventQuery
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.stubbing.Scenario
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.cleaning.config.CleaningServiceConfigProperties
import org.eclipse.tractusx.bpdm.cleaning.config.OrchestratorConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.api.ApiCommons
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceV7Dto
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.*
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AdditionalSiteV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartnerV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.SiteV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepResultEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepResultV7Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.time.Instant
import java.util.*

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["${OrchestratorConfigProperties.PREFIX}.security-enabled=false"]
)
@ActiveProfiles("test")
class CleaningServiceApiCallsTest @Autowired constructor(
    private val cleaningServiceDummy: CleaningServiceDummy,
    private val jsonMapper: JsonMapper,
    private val cleaningServiceConfigProperties: CleaningServiceConfigProperties
) {

    companion object {
        const val ORCHESTRATOR_RESERVE_TASKS_URL = "${ApiCommons.BASE_PATH_V7_BUSINESS_PARTNERS}/step-reservations"
        const val ORCHESTRATOR_RESOLVE_TASKS_URL = "${ApiCommons.BASE_PATH_V7_BUSINESS_PARTNERS}/step-results"

        const val RESERVATION_SCENARIO = "Reservation"
        const val RESERVED_STATE = "Reserved"

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
    val businessPartnerFactory = BusinessPartnerTestDataFactory(OrchestratorRequestFactoryCommon(countries = listOf("DE")))
    val defaultBpnRequest = BpnReferenceV7Dto("IGNORED", null, BpnReferenceTypeV7.BpnRequestIdentifier)
    val expectedConfidenceCriteria = cleaningServiceDummy.dummyConfidenceCriteria.copy(sharedByOwner = true)

    @BeforeEach
    fun beforeEach() {
        orchestratorMockApi.resetAll()

        orchestratorMockApi.stubFor(
            post(urlPathEqualTo(ORCHESTRATOR_RESERVE_TASKS_URL))
                .inScenario(RESERVATION_SCENARIO)
                .whenScenarioStateIs(RESERVED_STATE)
                .willReturn(okJson(jsonMapper.writeValueAsString(TaskStepReservationV7Response(emptyList(), Instant.now()))))
        )
    }

    @Test
    fun `Additional Address with SiteV7Dto without BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithBpnReferences(BpnReferenceV7Dto.empty)
            .copyWithLegalAddress(PostalAddressV7Dto.empty)
            .copyWithSiteMainAddress(PostalAddressV7Dto.empty)
            .copy(
                uncategorized = businessPartnerFactory.createFullBusinessPartner("test").uncategorized.copy(
                    identifiers = emptyList() // Assign empty list to uncategorized identifiers
                )
            )

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithBpnReferences(defaultBpnRequest)
                .copyWithLegalAddress(mockedBusinessPartner.uncategorized.address!!)
                .copyWithSiteMainAddress(mockedBusinessPartner.uncategorized.address!!.postalProperties)
                .copyWithConfidenceCriteria(expectedConfidenceCriteria)
                .copyWithHasChanged(false, false, true)
                .copyAsCxMemberData()
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertIsEqualIgnoreReferenceValue(actualResponse, expectedResponse)
    }

    @Test
    fun `Additional Address with SiteV7Dto with BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithLegalAddress(PostalAddressV7Dto.empty)
            .copyWithSiteMainAddress(PostalAddressV7Dto.empty)
            .copy(
                uncategorized = businessPartnerFactory.createFullBusinessPartner("test").uncategorized.copy(
                    identifiers = emptyList() // Assign empty list to uncategorized identifiers
                )
            )

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithLegalAddress(mockedBusinessPartner.uncategorized.address!!.postalProperties)
                .copyWithSiteMainAddress(mockedBusinessPartner.uncategorized.address!!.postalProperties)
                .copyWithConfidenceCriteria(expectedConfidenceCriteria)
                .copyWithHasChanged(false, false, true)
                .copyAsCxMemberData()
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse)
    }

    @Test
    fun `Additional Address without SiteV7Dto without BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithBpnReferences(BpnReferenceV7Dto.empty)
            .copyWithLegalAddress(PostalAddressV7Dto.empty)
            .copy(site = null)
            .copy(
                uncategorized = businessPartnerFactory.createFullBusinessPartner("test").uncategorized.copy(
                    identifiers = emptyList() // Assign empty list to uncategorized identifiers
                )
            )

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithBpnReferences(defaultBpnRequest)
                .copyWithLegalAddress(mockedBusinessPartner.uncategorized.address!!.postalProperties)
                .copyWithConfidenceCriteria(expectedConfidenceCriteria)
                .copyWithHasChanged(false, false, true)
                .copyAsCxMemberData()
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertIsEqualIgnoreReferenceValue(actualResponse, expectedResponse)
    }

    @Test
    fun `Additional Address without SiteV7Dto with BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithLegalAddress(PostalAddressV7Dto.empty)
            .copy(site = null)
            .copy(
                uncategorized = businessPartnerFactory.createFullBusinessPartner("test").uncategorized.copy(
                    identifiers = emptyList() // Assign empty list to uncategorized identifiers
                )
            )

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithLegalAddress(mockedBusinessPartner.uncategorized.address!!.postalProperties)
                .copyWithConfidenceCriteria(expectedConfidenceCriteria)
                .copyWithHasChanged(false, false, true)
                .copyAsCxMemberData()
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse)
    }

    @Test
    fun `SiteV7Dto with Own Main Address and without BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithBpnReferences(BpnReferenceV7Dto.empty)
            .copyWithLegalAddress(PostalAddressV7Dto.empty)
            .copy(additionalAddress = null)
            .copy(
                uncategorized = businessPartnerFactory.createFullBusinessPartner("test").uncategorized.copy(
                    identifiers = emptyList()
                )
            )

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithBpnReferences(defaultBpnRequest)
                .copyWithLegalAddress(mockedBusinessPartner.uncategorized.address!!)
                .copyWithConfidenceCriteria(expectedConfidenceCriteria)
                .copyWithHasChanged(false, true, false)
                .copyAsCxMemberData()
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertIsEqualIgnoreReferenceValue(actualResponse, expectedResponse)
    }

    @Test
    fun `SiteV7Dto With Own Main Address and BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithLegalAddress(PostalAddressV7Dto.empty)
            .copy(additionalAddress = null)
            .copy(
                uncategorized = businessPartnerFactory.createFullBusinessPartner("test").uncategorized.copy(
                    identifiers = emptyList() // Assign empty list to uncategorized identifiers
                )
            )

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithLegalAddress(mockedBusinessPartner.uncategorized.address!!)
                .copyWithConfidenceCriteria(expectedConfidenceCriteria)
                .copyWithHasChanged(false, true, false)
                .copyAsCxMemberData()
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse)
    }

    @Test
    fun `SiteV7Dto With Legal Address As Main Address And Without BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithBpnReferences(BpnReferenceV7Dto.empty)
            .copyWithSiteMainAddress(null)
            .copy(additionalAddress = null)
            .copy(
                uncategorized = businessPartnerFactory.createFullBusinessPartner("test").uncategorized.copy(
                    identifiers = emptyList() // Assign empty list to uncategorized identifiers
                )
            )

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithBpnReferences(defaultBpnRequest)
                .copyWithConfidenceCriteria(expectedConfidenceCriteria)
                .copyWithHasChanged(true, true, false)
                .copyAsCxMemberData()
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertIsEqualIgnoreReferenceValue(actualResponse, expectedResponse)
    }

    @Test
    fun `SiteV7Dto With Legal Address As Main Address And With BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithSiteMainAddress(null)
            .copy(additionalAddress = null)
            .copy(
                uncategorized = businessPartnerFactory.createFullBusinessPartner("test").uncategorized.copy(
                    identifiers = emptyList() // Assign empty list to uncategorized identifiers
                )
            )

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithConfidenceCriteria(expectedConfidenceCriteria)
                .copyWithHasChanged(true, true, false)
                .copyAsCxMemberData()
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse)
    }

    @Test
    fun `Additional Sites With BPNs`() {
        //Mock Orchestrator responses
        val additionalSites = listOf(
            AdditionalSiteV7Dto(BpnReferenceV7Dto("BPNS0000000001XY", null, BpnReferenceTypeV7.Bpn), "Additional Site 1 test"),
            AdditionalSiteV7Dto(BpnReferenceV7Dto("BPNS0000000002XY", null, BpnReferenceTypeV7.Bpn), "Additional Site 2 test")
        )
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithLegalAddress(PostalAddressV7Dto.empty)
            .copy(
                additionalSites = additionalSites,
                uncategorized = businessPartnerFactory.createFullBusinessPartner("test").uncategorized.copy(
                    identifiers = emptyList() // Assign empty list to uncategorized identifiers
                )
            )

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithLegalAddress(mockedBusinessPartner.uncategorized.address!!.postalProperties)
                .copyWithConfidenceCriteria(expectedConfidenceCriteria)
                .copyWithHasChanged(false, false, true)
                .copyAsCxMemberData()
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse)
    }

    @Test
    fun `Additional Sites Without BPNs`() {
        //Mock Orchestrator responses
        val additionalSites = listOf(
            AdditionalSiteV7Dto(BpnReferenceV7Dto.empty, "Additional Site 1 test"),
            AdditionalSiteV7Dto(BpnReferenceV7Dto.empty, "Additional Site 2 test")
        )
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithBpnReferences(BpnReferenceV7Dto.empty)
            .copy(additionalSites = additionalSites)

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val cleanedBusinessPartner = getResolveResult(resolveMapping).results.single().businessPartner

        //Every additional site is requested as its own site, distinct from the site the record itself is about
        val additionalSiteReferences = cleanedBusinessPartner.additionalSites.map { it.bpnReference }
        assertThat(additionalSiteReferences).allMatch { it.referenceType == BpnReferenceTypeV7.BpnRequestIdentifier }
        assertThat(additionalSiteReferences.map { it.referenceValue }.distinct()).hasSameSizeAs(additionalSites)
        assertThat(additionalSiteReferences).doesNotContain(cleanedBusinessPartner.site!!.bpnReference)
    }

    @Test
    fun `Legal Entity without BPNs`() {
        //Mock Orchestrator responses
        val mockedBusinessPartner = businessPartnerFactory.createFullBusinessPartner("test")
            .copyWithBpnReferences(BpnReferenceV7Dto.empty)
            .copyWithLegalAddress(PostalAddressV7Dto.empty)
            .copy(site = null, additionalAddress = null)
            .copy(
                uncategorized = businessPartnerFactory.createFullBusinessPartner("test").uncategorized.copy(
                    identifiers = emptyList() // Assign empty list to uncategorized identifiers
                )
            )

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithBpnReferences(defaultBpnRequest)
                .copyWithLegalAddress(mockedBusinessPartner.uncategorized.address!!)
                .copyWithConfidenceCriteria(expectedConfidenceCriteria)
                .copyWithHasChanged(true, false, false)
                .copyAsCxMemberData()
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
            .copyWithLegalAddress(PostalAddressV7Dto.empty)
            .copy(site = null, additionalAddress = null)
            .copy(
                uncategorized = businessPartnerFactory.createFullBusinessPartner("test").uncategorized.copy(
                    identifiers = emptyList() // Assign empty list to uncategorized identifiers
                )
            )

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedBusinessPartner)

        //Create expectations
        val expectedResponse = createResultRequest(
            mockedBusinessPartner
                .copyWithLegalAddress(mockedBusinessPartner.uncategorized.address!!)
                .copyWithConfidenceCriteria(expectedConfidenceCriteria)
                .copyWithHasChanged(true, false, false)
                .copyAsCxMemberData()
        )

        // Call the method under test
        cleaningServiceDummy.pollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse)
    }



    fun mockOrchestratorReserveApi(businessPartner: BusinessPartnerV7Dto): StubMapping {
        // Orchestrator reserve
        return orchestratorMockApi.stubFor(
            post(urlPathEqualTo(ORCHESTRATOR_RESERVE_TASKS_URL))
                .inScenario(RESERVATION_SCENARIO)
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo(RESERVED_STATE)
                .willReturn(
                    okJson(jsonMapper.writeValueAsString(createSampleTaskStepReservationResponse(businessPartner)))
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

    // Helper method to create a sample TaskStepReservationV7Response
    private fun createSampleTaskStepReservationResponse(businessPartner: BusinessPartnerV7Dto): TaskStepReservationV7Response {
        return TaskStepReservationV7Response(listOf(TaskStepReservationEntryV7Dto(fixedTaskId, UUID.randomUUID().toString(), businessPartner)), Instant.MIN)
    }


    private fun getResolveResult(stubMapping: StubMapping): TaskStepResultV7Request{
        val serveEvents = orchestratorMockApi.getServeEvents(ServeEventQuery.forStubMapping(stubMapping)).requests
        assertEquals(serveEvents.size, 1)
        val actualRequest = serveEvents.first()!!.request
        return jsonMapper.readValue<TaskStepResultV7Request>(actualRequest.body)
    }

    private fun assertIsEqualIgnoreReferenceValue(actual: TaskStepResultV7Request, expected: TaskStepResultV7Request){
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(
                ".*.${BpnReferenceV7Dto::referenceValue.name}"
            )
            .isEqualTo(expected)
    }

    private fun createResultRequest(expectedBusinessPartner: BusinessPartnerV7Dto): TaskStepResultV7Request {
        return TaskStepResultV7Request(
            cleaningServiceConfigProperties.step, listOf(
                TaskStepResultEntryV7Dto(
                    fixedTaskId,
                    expectedBusinessPartner,
                    emptyList()
                )
            )
        )
    }


}