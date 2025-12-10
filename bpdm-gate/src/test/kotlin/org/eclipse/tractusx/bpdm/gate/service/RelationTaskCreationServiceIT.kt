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

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateErrorCode
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.gate.controller.SelfClientAsPartnerUploaderInitializer
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.PostalAddressDb
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.gate.util.PrincipalUtil
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.gate.GateInputFactory
import org.eclipse.tractusx.bpdm.test.testdata.gate.withAddressType
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.test.util.Timeframe
import org.eclipse.tractusx.orchestrator.api.ApiCommons
import org.eclipse.tractusx.orchestrator.api.model.*
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
import java.time.Instant
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [
    PostgreSQLContextInitializer::class,
    KeyCloakInitializer::class,
    SelfClientAsPartnerUploaderInitializer::class
])
@ActiveProfiles("test")
class RelationTaskCreationServiceIT @Autowired constructor(
    private val testHelpers: DbTestHelpers,
    private val inputFactory: GateInputFactory,
    private val gateClient: GateClient,
    private val sharingStateRepository: SharingStateRepository,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val principalUtil: PrincipalUtil,
    private val objectMapper: ObjectMapper,
    private val relationTaskCreationService: RelationTaskCreationService
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
    val anyTime: Instant = Instant.MIN

    var testName: String = ""

    @BeforeEach
    fun beforeEach(testInfo: TestInfo) {
        testName = testInfo.displayName
        testHelpers.truncateDbTables()
        orchestratorWireMockServer.resetAll()
    }

    @ParameterizedTest
    @EnumSource(value = RelationType::class, names = ["IsAlternativeHeadquarterFor", "IsManagedBy", "IsOwnedBy"])
    //@EnumSource(RelationType::class)
    fun createTaskForInitialLegalEntityRelation(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityBpnL1 = "$testName BPNL 1"
        val legalEntityBpnL2 = "$testName BPNL 2"
        val relationId = "$testName R 1"
        val taskId = "$testName T 1"
        val recordId = "$testName Record 1"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2)
        ))
        assignOutputBpnL(legalEntityId1, legalEntityBpnL1)
        assignOutputBpnL(legalEntityId2, legalEntityBpnL2)

        createRelation(relationId, relationType, legalEntityId1, legalEntityId2)

        val orchestratorMockResponse = TaskCreateRelationsResponse(listOf(
            TaskClientRelationsStateDto(
                taskId,
                recordId,
                BusinessPartnerRelations(
                    when(relationType) {
                        RelationType.IsAlternativeHeadquarterFor -> org.eclipse.tractusx.orchestrator.api.model.RelationType.IsAlternativeHeadquarterFor
                        RelationType.IsManagedBy -> org.eclipse.tractusx.orchestrator.api.model.RelationType.IsManagedBy
                        RelationType.IsOwnedBy -> org.eclipse.tractusx.orchestrator.api.model.RelationType.IsOwnedBy
                        RelationType.IsReplacedBy -> org.eclipse.tractusx.orchestrator.api.model.RelationType.IsReplacedBy
                    },
                    legalEntityBpnL1,
                    legalEntityBpnL2,
                    listOf(
                        RelationValidityPeriod(
                            validFrom = LocalDate.parse("1970-01-01"),
                            validTo = LocalDate.parse("9999-12-31")
                        )
                    )
                ),
                TaskProcessingRelationsStateDto(ResultState.Pending, TaskStep.CleanAndSync, StepState.Queued, emptyList(), anyTime, anyTime, anyTime))
        ))

        orchestratorWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(ApiCommons.BASE_PATH_V7_RELATIONS))
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(orchestratorMockResponse))
                )
        )

        relationTaskCreationService.sendTasks()

        val actual = gateClient.relationSharingState.get(externalIds = listOf(relationId)).content.single()

        Assertions.assertThat(actual.sharingStateType).isEqualTo(RelationSharingStateType.Pending)
    }

    @ParameterizedTest
    @EnumSource(value = RelationType::class, names = ["IsReplacedBy"])
    fun createTaskForInitialAddressRelation(relationType: RelationType) {
        val legalEntityId = "$testName LE"
        val additionalAddressId = "$testName AA"
        val sourceBpna = "$testName BPNA 1"
        val targetBpna = "$testName BPNA 2"
        val relationId = "$testName R"
        val taskId = "$testName T"
        val recordId = "$testName REC"

        // ---------- INPUT ----------
        gateClient.businessParters.upsertBusinessPartnersInput(
            listOf(
                inputFactory.createAllFieldsFilled(legalEntityId).request
                    .withAddressType(AddressType.LegalAddress)
                    .copy(isOwnCompanyData = true),

                inputFactory.createAllFieldsFilled(additionalAddressId).request
                    .withAddressType(AddressType.AdditionalAddress)
                    .copy(isOwnCompanyData = true)
            )
        )

        // ---------- OUTPUT ----------
        assignOutputBpna(
            externalId = legalEntityId,
            addressType = AddressType.LegalAddress,
            bpna = sourceBpna
        )

        assignOutputBpna(
            externalId = additionalAddressId,
            addressType = AddressType.AdditionalAddress,
            bpna = targetBpna
        )

        // ---------- RELATION ----------
        createRelation(
            externalId = relationId,
            relationType = relationType,
            source = legalEntityId,
            target = additionalAddressId
        )

        // ---------- ORCHESTRATOR MOCK ----------
        val orchestratorMockResponse = TaskCreateRelationsResponse(
            listOf(
                TaskClientRelationsStateDto(
                    taskId,
                    recordId,
                    BusinessPartnerRelations(
                        org.eclipse.tractusx.orchestrator.api.model.RelationType.IsReplacedBy,
                        sourceBpna,
                        targetBpna,
                        listOf(
                            RelationValidityPeriod(
                                LocalDate.parse("1970-01-01"),
                                LocalDate.parse("9999-12-31")
                            )
                        )
                    ),
                    TaskProcessingRelationsStateDto(ResultState.Pending, TaskStep.CleanAndSync, StepState.Queued, emptyList(), anyTime, anyTime, anyTime)
                )
            )
        )
        orchestratorWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(ApiCommons.BASE_PATH_V7_RELATIONS))
                .willReturn(WireMock.okJson(objectMapper.writeValueAsString(orchestratorMockResponse)))
        )

        relationTaskCreationService.sendTasks()

        // ---------- ASSERT ----------
        val actual = gateClient.relationSharingState.get(externalIds = listOf(relationId)).content.single()

        Assertions.assertThat(actual.sharingStateType).isEqualTo(RelationSharingStateType.Pending)
    }

    @ParameterizedTest
    @EnumSource(value = RelationType::class, names = ["IsOwnedBy", "IsManagedBy", "IsAlternativeHeadquarterFor"])
    fun unsupportedAddressRelation_shouldSetErrorAndUnstage(relationType: RelationType) {
        val legalEntityId = "$testName LE"
        val additionalAddressId = "$testName AA"
        val relationId = "$testName R"

        // ---------- INPUT ----------
        gateClient.businessParters.upsertBusinessPartnersInput(
            listOf(
                inputFactory.createAllFieldsFilled(legalEntityId).request
                    .withAddressType(AddressType.LegalAddress)
                    .copy(isOwnCompanyData = true),

                inputFactory.createAllFieldsFilled(additionalAddressId).request
                    .withAddressType(AddressType.AdditionalAddress)
                    .copy(isOwnCompanyData = true)
            )
        )

        // ---------- OUTPUT ----------
        assignOutputBpna(
            externalId = legalEntityId,
            addressType = AddressType.LegalAddress,
            bpna = "$testName BPNA-LEGAL"
        )
        assignOutputBpna(
            externalId = additionalAddressId,
            addressType = AddressType.AdditionalAddress,
            bpna = "$testName BPNA-ADD"
        )

        // ---------- RELATION (UNSUPPORTED) ----------
        createRelation(
            externalId = relationId,
            relationType = relationType,
            source = legalEntityId,
            target = additionalAddressId
        )

        // ---------- EXECUTE ----------
        relationTaskCreationService.sendTasks()

        // ---------- ASSERT: Sharing State ----------
        val sharingState = gateClient.relationSharingState.get(externalIds = listOf(relationId)).content.single()

        Assertions.assertThat(sharingState.sharingStateType).isEqualTo(RelationSharingStateType.Error)
        Assertions.assertThat(sharingState.sharingErrorCode).isEqualTo(RelationSharingStateErrorCode.SharingProcessError)

        // ---------- ASSERT: Orchestrator NOT called ----------
        orchestratorWireMockServer.verify(
            0,
            WireMock.postRequestedFor(
                WireMock.urlPathEqualTo(ApiCommons.BASE_PATH_V7_RELATIONS)
            )
        )
    }

    @ParameterizedTest
    @EnumSource(value = RelationType::class, names = ["IsReplacedBy"])
    fun unsupportedLegalEntityRelation_shouldErrorAndUnstage(relationType: RelationType) {

        val le1 = "$testName LE1"
        val le2 = "$testName LE2"
        val relationId = "$testName R"

        // ---------- INPUT ----------
        gateClient.businessParters.upsertBusinessPartnersInput(
            listOf(
                createLegalEntityRequest(le1),
                createLegalEntityRequest(le2)
            )
        )

        // ---------- OUTPUT ----------
        assignOutputBpnL(le1, "$testName BPNL1")
        assignOutputBpnL(le2, "$testName BPNL2")

        // ---------- RELATION (INVALID) ----------
        createRelation(
            externalId = relationId,
            relationType = relationType,
            source = le1,
            target = le2
        )

        // ---------- EXECUTE ----------
        relationTaskCreationService.sendTasks()

        // ---------- ASSERT ----------
        val state = gateClient.relationSharingState.get(externalIds = listOf(relationId)).content.single()

        Assertions.assertThat(state.sharingStateType).isEqualTo(RelationSharingStateType.Error)
        Assertions.assertThat(state.sharingErrorCode).isEqualTo(RelationSharingStateErrorCode.SharingProcessError)

        orchestratorWireMockServer.verify(
            0,
            WireMock.postRequestedFor(
                WireMock.urlPathEqualTo(ApiCommons.BASE_PATH_V7_RELATIONS)
            )
        )
    }

    private fun createLegalEntityRequest(externalId: String) =
        inputFactory.createAllFieldsFilled(externalId).request
            .withAddressType(AddressType.LegalAddress)
            .copy(isOwnCompanyData = true)

    private fun createRelation(externalId: String, relationType: RelationType, source: String, target: String): Timeframe{
        val beforeCreation = Instant.now()
        gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                listOf(
                    RelationPutEntry(
                        externalId = externalId,
                        relationType = relationType,
                        businessPartnerSourceExternalId = source,
                        businessPartnerTargetExternalId = target
                    )
                )
            )
        )
        val afterCreation = Instant.now()

        return Timeframe(beforeCreation, afterCreation)
    }

    private fun assignOutputBpnL(legalEntityExternalId: String, bpnL: String){
        val sharingState1 = sharingStateRepository.findByExternalIdAndTenantBpnl(legalEntityExternalId, principalUtil.resolveTenantBpnl().value).first()
        businessPartnerRepository.save(BusinessPartnerDb(sharingState1, stage = StageType.Output, bpnL = bpnL, postalAddress = PostalAddressDb(addressType = AddressType.LegalAddress), legalEntityConfidence = null, siteConfidence = null, addressConfidence = null))
    }

    private fun assignOutputBpna(externalId: String, addressType: AddressType, bpna: String){
        val sharingState = sharingStateRepository.findByExternalIdAndTenantBpnl(externalId, principalUtil.resolveTenantBpnl().value).first()
        businessPartnerRepository.save(BusinessPartnerDb(sharingState, stage = StageType.Output, bpnA = bpna, postalAddress = PostalAddressDb(addressType = addressType), legalEntityConfidence = null, siteConfidence = null, addressConfidence = null))
    }
}