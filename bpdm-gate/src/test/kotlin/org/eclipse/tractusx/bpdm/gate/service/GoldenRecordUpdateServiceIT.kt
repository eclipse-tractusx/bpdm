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

package org.eclipse.tractusx.bpdm.gate.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.stubbing.Scenario
import com.neovisionaries.i18n.CountryCode
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.SharableRelationType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputRelationDto
import org.eclipse.tractusx.bpdm.gate.controller.SelfClientAsPartnerUploaderInitializer
import org.eclipse.tractusx.bpdm.gate.entity.PhysicalPostalAddressDb
import org.eclipse.tractusx.bpdm.gate.entity.RelationOutputDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.ConfidenceCriteriaDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.PostalAddressDb
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.gate.util.PrincipalUtil
import org.eclipse.tractusx.bpdm.pool.api.PoolAddressApi
import org.eclipse.tractusx.bpdm.pool.api.PoolChangelogApi
import org.eclipse.tractusx.bpdm.pool.api.PoolLegalEntityApi
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.RelationType
import org.eclipse.tractusx.bpdm.pool.api.model.RelationVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.ChangelogEntryVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.gate.GateInputFactory
import org.eclipse.tractusx.bpdm.test.testdata.gate.withAddressType
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.BusinessPartnerTestDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.orchestrator.api.FinishedTaskEventApi
import org.eclipse.tractusx.orchestrator.api.GoldenRecordTaskApi
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [
    PostgreSQLContextInitializer::class,
    KeyCloakInitializer::class,
    SelfClientAsPartnerUploaderInitializer::class
])
@ActiveProfiles("test")
class GoldenRecordUpdateServiceIT @Autowired constructor(
    private val testHelpers: DbTestHelpers,
    private val gateClient: GateClient,
    private val inputFactory: GateInputFactory,
    private val sharingStateRepository: SharingStateRepository,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val principalUtil: PrincipalUtil,
    private val objectMapper: ObjectMapper,
    private val goldenRecordUpdateBatchService: GoldenRecordUpdateBatchService,
    private val taskResolutionBatchService: TaskResolutionBatchService,
    private val orchestratorTestDataFactory: BusinessPartnerTestDataFactory
) {
    companion object {
        @JvmField
        @RegisterExtension
        val poolWireMockServer: WireMockExtension = WireMockExtension.newInstance().options(WireMockConfiguration.wireMockConfig().dynamicPort()).build()

        @JvmField
        @RegisterExtension
        val orchestratorWireMockServer: WireMockExtension = WireMockExtension.newInstance().options(WireMockConfiguration.wireMockConfig().dynamicPort()).build()


        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.client.pool.base-url") { poolWireMockServer.baseUrl() }
            registry.add("bpdm.client.orchestrator.base-url") { orchestratorWireMockServer.baseUrl() }
        }
    }

    /**
     * Represents a time that does not matter as it should be ignored by equality comparisons
     */
    val anyTime: Instant = OffsetDateTime.of(2025, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC).toInstant()
    val anyConfidence: ConfidenceCriteriaDb = ConfidenceCriteriaDb(true, true, 0, LocalDateTime.ofInstant(anyTime, ZoneId.systemDefault()),  LocalDateTime.ofInstant(anyTime, ZoneId.systemDefault()), 0)

    var testName: String = ""

    @BeforeEach
    fun beforeEach(testInfo: TestInfo) {
        testName = testInfo.displayName
        testHelpers.truncateDbTables()
        poolWireMockServer.resetAll()
        orchestratorWireMockServer.resetAll()
    }

    @Test
    fun createOutputRelations(){
        val legalEntityExternalId = "$testName LE"
        val sourceBpnL = "$testName Source BPNL"
        val targetBpnL = "$testName Target BPNL"

        createLegalEntityOutput(legalEntityExternalId, sourceBpnL, SharingStateType.Success, emptyList())

        val mockedChangelogResponse = PageDto<ChangelogEntryVerboseDto>(1, 1, 0, 1, listOf(
            ChangelogEntryVerboseDto(sourceBpnL, BusinessPartnerType.LEGAL_ENTITY, Instant.now(), ChangelogType.UPDATE)
        ))
        val mockedEmptyChangelogResponse = PageDto<ChangelogEntryVerboseDto>(0, 0, 0, 0, emptyList())

        val mockedLegalEntityResponse = PageDto<LegalEntityWithLegalAddressVerboseDto>(1, 1, 0, 1, listOf(
            with(BusinessPartnerVerboseValues.legalEntity1){ copy(legalEntity = legalEntity.copy(bpnl = sourceBpnL, relations = listOf(RelationVerboseDto(
                RelationType.IsAlternativeHeadquarterFor, businessPartnerSourceBpnl = sourceBpnL, businessPartnerTargetBpnl = targetBpnL, true)))) }
        ))

        poolWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(PoolChangelogApi.CHANGELOG_PATH + PoolChangelogApi.SUBPATH_SEARCH))
                .inScenario("Changelog")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("Requested")
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(mockedChangelogResponse))
                )
        )
        poolWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(PoolChangelogApi.CHANGELOG_PATH + PoolChangelogApi.SUBPATH_SEARCH))
                .inScenario("Changelog")
                .whenScenarioStateIs("Requested")
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(mockedEmptyChangelogResponse))
                )
        )

        poolWireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo(PoolLegalEntityApi.LEGAL_ENTITY_PATH))
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(mockedLegalEntityResponse))
                )
        )

        goldenRecordUpdateBatchService.updateOutputOnGoldenRecordChange()

        val expectedRelations = listOf(BusinessPartnerOutputRelationDto(SharableRelationType.IsAlternativeHeadquarterFor, sourceBpnL, targetBpnL))
        val actualRelations = gateClient.businessParters.getBusinessPartnersOutput(listOf(legalEntityExternalId)).content.flatMap { it.legalEntity.relations }

        Assertions.assertThat(actualRelations).isEqualTo(expectedRelations)
    }

    @Test
    fun updateOutputRelations(){
        val legalEntityExternalId = "$testName LE"
        val bpnL = "$testName BPNL"

        val sourceBpnL = "$testName Source BPNL"
        val targetBpnL = "$testName Target BPNL"

        val newSourceBpnL =  "$testName New Source BPNL"
        val newTargetBpnL =  "$testName New Target BPNL"

        createLegalEntityOutput(legalEntityExternalId, bpnL, SharingStateType.Success, listOf(RelationOutputDb(SharableRelationType.IsAlternativeHeadquarterFor, sourceBpnL, targetBpnL, anyTime)))

        val mockedChangelogResponse = PageDto<ChangelogEntryVerboseDto>(1, 1, 0, 1, listOf(
            ChangelogEntryVerboseDto(bpnL, BusinessPartnerType.LEGAL_ENTITY, Instant.now(), ChangelogType.UPDATE)
        ))
        val mockedEmptyChangelogResponse = PageDto<ChangelogEntryVerboseDto>(0, 0, 0, 0, emptyList())

        val mockedLegalEntityResponse = PageDto<LegalEntityWithLegalAddressVerboseDto>(1, 1, 0, 1, listOf(
            with(BusinessPartnerVerboseValues.legalEntity1){ copy(legalEntity = legalEntity.copy(bpnl = bpnL, relations = listOf(RelationVerboseDto(
                RelationType.IsAlternativeHeadquarterFor, businessPartnerSourceBpnl = newSourceBpnL, businessPartnerTargetBpnl = newTargetBpnL, true)))) }
        ))

        poolWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(PoolChangelogApi.CHANGELOG_PATH + PoolChangelogApi.SUBPATH_SEARCH))
                .inScenario("Changelog")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("Requested")
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(mockedChangelogResponse))
                )
        )
        poolWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(PoolChangelogApi.CHANGELOG_PATH + PoolChangelogApi.SUBPATH_SEARCH))
                .inScenario("Changelog")
                .whenScenarioStateIs("Requested")
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(mockedEmptyChangelogResponse))
                )
        )

        poolWireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo(PoolLegalEntityApi.LEGAL_ENTITY_PATH))
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(mockedLegalEntityResponse))
                )
        )

        goldenRecordUpdateBatchService.updateOutputOnGoldenRecordChange()

        val expectedRelations = listOf(BusinessPartnerOutputRelationDto(SharableRelationType.IsAlternativeHeadquarterFor, newSourceBpnL, newTargetBpnL))
        val actualRelations = gateClient.businessParters.getBusinessPartnersOutput(listOf(legalEntityExternalId)).content.flatMap { it.legalEntity.relations }

        Assertions.assertThat(actualRelations).isEqualTo(expectedRelations)
    }

    @Test
    fun businessPartnerUpdateDoesNotOverwriteRelation(){
        val legalEntityExternalId = "$testName LE"
        val bpnL =  "$testName BPNL"

        val sourceBpnL = "$testName Source BPNL"
        val targetBpnL = "$testName Target BPNL"

        val taskId = "$testName Source BPNL"
        val recordId = java.util.UUID.randomUUID()

        createLegalEntityOutput(legalEntityExternalId, bpnL, SharingStateType.Pending, listOf(RelationOutputDb(SharableRelationType.IsAlternativeHeadquarterFor, sourceBpnL, targetBpnL, anyTime)))
        setPendingTask(legalEntityExternalId, taskId, recordId)

        val orchestratorMockEventResponse = FinishedTaskEventsResponse(1, 1, 0, 1, listOf(
            FinishedTaskEventsResponse.Event(Instant.now(), ResultState.Success, taskId),
        ))

        val orchestratorMockResultResponse = TaskStateResponse(listOf(
            TaskClientStateDto(taskId, recordId.toString(), orchestratorTestDataFactory.createLegalEntityBusinessPartner(), TaskProcessingStateDto(ResultState.Success,
                TaskStep.PoolSync, StepState.Success, emptyList(), anyTime, anyTime, anyTime)),
        ))

        orchestratorWireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo(FinishedTaskEventApi.FINISHED_TASK_EVENT_PATH))
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(orchestratorMockEventResponse))
                )
        )

        orchestratorWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("${GoldenRecordTaskApi.TASKS_PATH}/state/search"))
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(orchestratorMockResultResponse))
                )
        )

        val mockEmptyResponse = PageDto<Any>(0, 0, 0, 0, emptyList<Any>())
        poolWireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo(PoolLegalEntityApi.LEGAL_ENTITY_PATH))
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(mockEmptyResponse))
                )
        )
        poolWireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo(PoolAddressApi.ADDRESS_PATH))
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(mockEmptyResponse))
                )
        )

        taskResolutionBatchService.resolveTasks()

        val expectedRelations = listOf(BusinessPartnerOutputRelationDto(SharableRelationType.IsAlternativeHeadquarterFor, sourceBpnL, targetBpnL))
        val actualRelations = gateClient.businessParters.getBusinessPartnersOutput(listOf(legalEntityExternalId)).content.flatMap { it.legalEntity.relations }

        Assertions.assertThat(actualRelations).isEqualTo(expectedRelations)
    }

    private fun createLegalEntityOutput(externalId: String, bpnL: String, sharingStateType: SharingStateType, relations: List<RelationOutputDb>){
        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(externalId)
        ))

        val sharingState = sharingStateRepository.findByExternalIdAndTenantBpnl(externalId, principalUtil.resolveTenantBpnl().value).first()
        sharingState.sharingStateType = sharingStateType

        val output = BusinessPartnerDb(sharingState, stage = StageType.Output, bpnL = bpnL, bpnA = "any", postalAddress = PostalAddressDb(addressType = AddressType.LegalAddress, physicalPostalAddress = PhysicalPostalAddressDb(null,
            CountryCode.DE, null, null, null, null, "any", null, null, null, null, null, null, null, null)), legalEntityConfidence = anyConfidence, siteConfidence = anyConfidence, addressConfidence = anyConfidence)
        output.relations.addAll(relations)

        sharingStateRepository.save(sharingState)
        businessPartnerRepository.save(output)
    }

    private fun setPendingTask(externalId: String, taskId: String, recordId: java.util.UUID){
        val sharingState = sharingStateRepository.findByExternalIdAndTenantBpnl(externalId, principalUtil.resolveTenantBpnl().value).first()
        sharingState.sharingStateType = SharingStateType.Pending
        sharingState.taskId = taskId
        sharingState.orchestratorRecordId = recordId

        sharingStateRepository.save(sharingState)
    }

    private fun createLegalEntityRequest(externalId: String) =
        inputFactory.createAllFieldsFilled(externalId).request
            .withAddressType(AddressType.LegalAddress)
            .copy(isOwnCompanyData = true)

}