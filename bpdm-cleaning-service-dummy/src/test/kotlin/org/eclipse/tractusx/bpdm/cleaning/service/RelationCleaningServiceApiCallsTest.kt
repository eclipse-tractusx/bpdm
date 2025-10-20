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

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.admin.model.ServeEventQuery
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.stubbing.Scenario
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.cleaning.config.CleaningServiceConfigProperties
import org.eclipse.tractusx.bpdm.cleaning.config.OrchestratorConfigProperties
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.orchestrator.api.ApiCommons
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant
import java.time.LocalDate
import java.util.*

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["${OrchestratorConfigProperties.PREFIX}.security-enabled=false"]
)
@ActiveProfiles("test")
class RelationCleaningServiceApiCallsTest @Autowired constructor(
    private val relationCleaningServiceDummy: RelationCleaningServiceDummy,
    private val jacksonObjectMapper: ObjectMapper,
    private val cleaningServiceConfigProperties: CleaningServiceConfigProperties
) {

    companion object {
        const val ORCHESTRATOR_RESERVE_TASKS_URL = "${ApiCommons.BASE_PATH_V7_RELATIONS}/step-reservations"
        const val ORCHESTRATOR_RESOLVE_TASKS_URL = "${ApiCommons.BASE_PATH_V7_RELATIONS}/step-results"

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

    @BeforeEach
    fun beforeEach() {
        orchestratorMockApi.resetAll()

        orchestratorMockApi.stubFor(
            post(urlPathEqualTo(ORCHESTRATOR_RESERVE_TASKS_URL))
                .inScenario(RESERVATION_SCENARIO)
                .whenScenarioStateIs(RESERVED_STATE)
                .willReturn(okJson(jacksonObjectMapper.writeValueAsString(TaskRelationsStepReservationResponse(emptyList(), Instant.now()))))
        )
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `Relation Cleaning Test for all RelationTypes`(relationType: RelationType) {
        val mockedRelation = createRelation("rel1", relationType)

        val resolveMapping = mockOrchestratorResolveApi()
        mockOrchestratorReserveApi(mockedRelation)

        val expectedResponse = TaskRelationsStepResultRequest(
            cleaningServiceConfigProperties.step,
            listOf(TaskRelationsStepResultEntryDto(fixedTaskId, mockedRelation))
        )

        relationCleaningServiceDummy.relationPollForCleanAndSyncTasks()
        val actualResponse = getResolveResult(resolveMapping)

        assertThat(actualResponse).isEqualTo(expectedResponse)
    }

    fun mockOrchestratorReserveApi(relation: BusinessPartnerRelations): StubMapping {
        return orchestratorMockApi.stubFor(
            post(urlPathEqualTo(ORCHESTRATOR_RESERVE_TASKS_URL))
                .inScenario(RESERVATION_SCENARIO)
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo(RESERVED_STATE)
                .willReturn(
                    okJson(
                        jacksonObjectMapper.writeValueAsString(
                            TaskRelationsStepReservationResponse(
                                listOf(TaskRelationsStepReservationEntryDto(fixedTaskId, UUID.randomUUID().toString(), relation)),
                                Instant.now()
                            )
                        )
                    )
                )
        )
    }

    fun mockOrchestratorResolveApi(): StubMapping {
        return orchestratorMockApi.stubFor(
            post(urlPathEqualTo(ORCHESTRATOR_RESOLVE_TASKS_URL))
                .willReturn(aResponse().withStatus(200))
        )
    }

    private fun getResolveResult(stubMapping: StubMapping): TaskRelationsStepResultRequest {
        val serveEvents = orchestratorMockApi.getServeEvents(ServeEventQuery.forStubMapping(stubMapping)).requests
        assertEquals(1, serveEvents.size)
        val actualRequest = serveEvents.first().request
        return jacksonObjectMapper.readValue(actualRequest.body, TaskRelationsStepResultRequest::class.java)
    }

    private fun createRelation(idSuffix: String, relationType: RelationType): BusinessPartnerRelations {
        return BusinessPartnerRelations(
            relationType = relationType,
            businessPartnerSourceBpnl = "BPNL_SOURCE_$idSuffix",
            businessPartnerTargetBpnl = "BPNL_TARGET_$idSuffix",
            validityPeriods = listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.parse("2020-01-01"),
                    validTo = LocalDate.parse("2030-01-01")
                )
            ),
        )
    }
}


