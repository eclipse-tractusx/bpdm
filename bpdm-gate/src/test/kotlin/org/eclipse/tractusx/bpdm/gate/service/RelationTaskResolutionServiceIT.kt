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

package org.eclipse.tractusx.bpdm.gate.service

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationOutputSearchRequest
import org.eclipse.tractusx.bpdm.gate.controller.SelfClientAsPartnerUploaderInitializer
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.entity.RelationSharingStateDb
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.eclipse.tractusx.bpdm.gate.util.GateTestValues
import org.eclipse.tractusx.bpdm.gate.util.PrincipalUtil
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.orchestrator.api.ApiCommons
import org.eclipse.tractusx.orchestrator.api.model.*
import org.eclipse.tractusx.orchestrator.api.model.RelationType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [
    PostgreSQLContextInitializer::class,
    KeyCloakInitializer::class,
    SelfClientAsPartnerUploaderInitializer::class
])
@ActiveProfiles("test")
class RelationTaskResolutionServiceIT @Autowired constructor(
    private val testHelpers: DbTestHelpers,
    private val relationRepository: RelationRepository,
    private val principalUtil: PrincipalUtil,
    private val jsonMapper: JsonMapper,
    private val relationTaskResolutionService: RelationTaskResolutionService,
    private val gateClient: GateClient
) {

    companion object {

        @JvmField
        @RegisterExtension
        val orchestratorWireMockServer: WireMockExtension = WireMockExtension.newInstance().options(WireMockConfiguration.wireMockConfig().dynamicPort()).build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.client.orchestrator.base-url") { orchestratorWireMockServer.baseUrl() }
        }
    }

    /**
     * Represents a time that does not matter as it should be ignored by equality comparisons
     */
    val anyTime: Instant = OffsetDateTime.of(2025, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC).toInstant()

    var testName: String = ""

    @BeforeEach
    fun beforeEach(testInfo: TestInfo) {
        testName = testInfo.displayName
        testHelpers.truncateDbTables()
        orchestratorWireMockServer.resetAll()
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun resolvePendingTasks(relationType: RelationType){
        val bpnL1 = "$testName BPNL1"
        val bpnL2 = "$testName BPNL2"
        val relationId1 = "$testName R 1"
        val relationId2 = "$testName R 2"
        val taskId1 = "$testName T 1"
        val taskId2 = "$testName T 2"
        val recordId1 = "$testName Record 1"
        val recordId2 = "$testName Record 2"
        val errorType = TaskRelationsErrorType.Unspecified
        val errorDescription = "$testName error description"

        val pendingRelations =  listOf(
            RelationDb(relationId1, principalUtil.resolveTenantBpnl().value, RelationSharingStateDb(RelationSharingStateType.Pending, null, null, anyTime, recordId1, taskId1, false), null),
            RelationDb(relationId2, principalUtil.resolveTenantBpnl().value, RelationSharingStateDb(RelationSharingStateType.Pending, null, null, anyTime, recordId2, taskId2, false), null)
        )
        relationRepository.saveAll(pendingRelations)

        val orchestratorMockEventResponse = FinishedTaskEventsResponse(2, 1, 0, 2, listOf(
            FinishedTaskEventsResponse.Event(Instant.now(), ResultState.Success, taskId1),
            FinishedTaskEventsResponse.Event(Instant.now(), ResultState.Success, taskId2),
        ))

        val mockedRelationValidityPeriods = GateTestValues.alwaysActiveRelationValidity.map { RelationValidityPeriod(it.validFrom, it.validTo) }
        val orchestratorMockResultResponse = TaskRelationsStateResponse(listOf(
            TaskClientRelationsStateDto(taskId1, recordId1, BusinessPartnerRelations(relationType, bpnL1,bpnL2, mockedRelationValidityPeriods),
                TaskProcessingRelationsStateDto(ResultState.Success, TaskStep.PoolSync, StepState.Success, emptyList(), anyTime, anyTime, anyTime )),
            TaskClientRelationsStateDto(taskId2, recordId2, BusinessPartnerRelations(relationType, bpnL1,bpnL2, mockedRelationValidityPeriods),
                TaskProcessingRelationsStateDto(ResultState.Error, TaskStep.CleanAndSync, StepState.Error, listOf(TaskRelationsErrorDto(errorType, errorDescription)), anyTime, anyTime, anyTime )),
        ))

        orchestratorWireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("${ApiCommons.BASE_PATH_V7_RELATIONS}/finished-events"))
                .willReturn(
                    WireMock.okJson(jsonMapper.writeValueAsString(orchestratorMockEventResponse))
                )
        )

        orchestratorWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("${ApiCommons.BASE_PATH_V7_RELATIONS}/state/search"))
                .willReturn(
                    WireMock.okJson(jsonMapper.writeValueAsString(orchestratorMockResultResponse))
                )
        )

        relationTaskResolutionService.checkResolveTasks()

        val expectedOutput = PageDto(1, 1, 0, 1, listOf(
            RelationOutputDto(
                relationId1,
                when(relationType) {
                  RelationType.IsAlternativeHeadquarterFor -> SharableRelationType.IsAlternativeHeadquarterFor
                  RelationType.IsManagedBy -> SharableRelationType.IsManagedBy
                    RelationType.IsOwnedBy -> SharableRelationType.IsOwnedBy
                },
                bpnL1,
                bpnL2,
                GateTestValues.alwaysActiveRelationValidity,
                anyTime)
        ))
        val expectedSharingStates = PageDto(2, 1, 0, 2, listOf(
            RelationSharingStateDto(relationId1, RelationSharingStateType.Success, null, null, taskId1, anyTime),
            RelationSharingStateDto(relationId2, RelationSharingStateType.Error, RelationSharingStateErrorCode.SharingProcessError, errorDescription, taskId2, anyTime)
        ))

        val actualOutput = gateClient.relationOutput.postSearch(RelationOutputSearchRequest(externalIds = listOf(relationId1, relationId1)))
        val actualSharingStates = gateClient.relationSharingState.get(externalIds = listOf(relationId1, relationId2))

        Assertions.assertThat(actualOutput)
            .usingRecursiveComparison()
            .ignoringFieldsOfTypes(Instant::class.java)
            .isEqualTo(expectedOutput)

        Assertions.assertThat(actualSharingStates)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFieldsOfTypes(Instant::class.java)
            .isEqualTo(expectedSharingStates)
    }

}