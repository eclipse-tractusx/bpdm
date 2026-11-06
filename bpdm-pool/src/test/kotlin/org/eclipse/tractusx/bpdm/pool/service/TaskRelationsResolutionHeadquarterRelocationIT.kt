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

package org.eclipse.tractusx.bpdm.pool.service

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.v7.client.PoolV7Client
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressRelationVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.changelog.ChangelogEntryVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.changelog.ChangelogSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.changelog.ChangelogTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.test.containers.OrchestratorMockConfiguration
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.containers.OrchestratorMockContextInitializer
import org.eclipse.tractusx.bpdm.pool.service.application.GoldenRecordRelationTaskResolutionApplicationService
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerRequestFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.ExpectedBusinessPartnerResultFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolDataHelper
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestDataEnvironment
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.test.util.PoolAssertHelper
import org.eclipse.tractusx.bpdm.test.util.Timeframe
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.RelationType
import org.eclipse.tractusx.orchestrator.api.model.RelationValidityPeriod
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepReservationEntryDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import java.time.LocalDate

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class]
)
@ActiveProfiles("test-no-auth", "test-scheduling-disabled")
@Import(OrchestratorMockConfiguration::class)
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OrchestratorMockContextInitializer::class])
class TaskRelationsResolutionHeadquarterRelocationIT @Autowired constructor(
    private val relationTaskResolutionService: GoldenRecordRelationTaskResolutionApplicationService,
    private val orchestratorMockDataFactory: OrchestratorMockDataFactory,
    private val poolApiClient: PoolV7Client,
    private val dataHelper: PoolDataHelper,
    private val dbTestHelpers: DbTestHelpers,
    private val poolAssertHelper: PoolAssertHelper
) {

    private val anyTime = Instant.MIN

    private lateinit var testDataEnvironment: TestDataEnvironment
    private lateinit var testName: String
    private lateinit var requestFactory: BusinessPartnerRequestFactory
    private lateinit var resultFactory: ExpectedBusinessPartnerResultFactory

    @BeforeEach
    fun beforeEach(testInfo: TestInfo) {
        dbTestHelpers.truncateDbTables()
        testDataEnvironment = dataHelper.createTestDataEnvironment()
        requestFactory = testDataEnvironment.requestFactory
        resultFactory = testDataEnvironment.expectFactory
        testName = testInfo.displayName
    }


    /**
     * GIVEN a legal entity and an additional address
     * WHEN additional address replaces legal address effective immediately
     * THEN user sees legal entity has the former additional address as new legal address
     */
    @Test
    fun `replaced legal address changes headquarter`(){
        //GIVEN
        val legalEntityRequest = requestFactory.createLegalEntityRequest(testName, true)
        val createdLegalEntity = poolApiClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        val addAddressRequest = requestFactory.buildAdditionalAddressCreateRequest("$testName 2", createdLegalEntity.legalEntity.header.bpnl).copy(scriptVariants = emptyList())
        val createdAddAddress = poolApiClient.addresses.createAddresses(listOf(addAddressRequest)).entities.single()

        //WHEN
        val updateStartTime = Instant.now()
        val activeNow = listOf(RelationValidityPeriod(LocalDate.now(), null))
        val replacedByRelation = BusinessPartnerRelations(RelationType.IsReplacedBy, createdLegalEntity.legalEntity.legalAddress.bpna, createdAddAddress.address.bpna, activeNow, anyReasonCode())
        val taskToResolve = TaskRelationsStepReservationEntryDto("Any", "Any", replacedByRelation)
        resolveRelationTasks(listOf(taskToResolve))
        val updateEndTime = Instant.now()

        //THEN
        val actualLegalEntity = poolApiClient.legalEntities.getLegalEntity(createdLegalEntity.legalEntity.header.bpnl)
        val expectedLegalEntity = buildExpectedRelocatedHeadquarterLegalEntity(createdLegalEntity, createdAddAddress, replacedByRelation)

        poolAssertHelper.assertLegalEntityResponse(listOf(actualLegalEntity), listOf(expectedLegalEntity), Timeframe(updateStartTime.minusSeconds(5), updateEndTime.plusSeconds(5)))
    }

    /**
     * GIVEN a legal entity and a site whose main address is its own
     * WHEN the site main address replaces the legal address effective immediately
     * THEN user sees the site main address as new legal address, now serving both roles
     */
    @Test
    fun `legal address replaced by site main address changes headquarter`(){
        //GIVEN
        val legalEntityRequest = requestFactory.createLegalEntityRequest(testName, true)
        val createdLegalEntity = poolApiClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        val siteRequest = requestFactory.buildSiteCreateRequest("$testName 2", createdLegalEntity.legalEntity.header.bpnl)
        val createdSite = poolApiClient.sites.createSite(listOf(siteRequest)).entities.single()

        //WHEN
        val activeNow = listOf(RelationValidityPeriod(LocalDate.now(), null))
        val replacedByRelation = BusinessPartnerRelations(
            RelationType.IsReplacedBy, createdLegalEntity.legalEntity.legalAddress.bpna, createdSite.mainAddress.bpna, activeNow, anyReasonCode()
        )
        val taskToResolve = TaskRelationsStepReservationEntryDto("Any", "Any", replacedByRelation)
        val results = resolveRelationTasks(listOf(taskToResolve))

        //THEN
        assertThat(results.single().errors).isEmpty()

        val actualLegalEntity = poolApiClient.legalEntities.getLegalEntity(createdLegalEntity.legalEntity.header.bpnl)
        assertThat(actualLegalEntity.legalAddress.bpna).isEqualTo(createdSite.mainAddress.bpna)
        assertThat(actualLegalEntity.legalAddress.addressType).isEqualTo(AddressType.LegalAndSiteMainAddress)
    }

    /**
     * GIVEN a legal entity and an additional address
     * WHEN additional address replaces legal address effective some point in the future
     * THEN user sees legal address of legal entity did not change
     */
    @Test
    fun `replacing legal address in the future does not change headquarter`(){
        //GIVEN
        val legalEntityRequest = requestFactory.createLegalEntityRequest(testName, true)
        val createdLegalEntity = poolApiClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        val addAddressRequest = requestFactory.buildAdditionalAddressCreateRequest("$testName 2", createdLegalEntity.legalEntity.header.bpnl).copy(scriptVariants = emptyList())
        val createdAddAddress = poolApiClient.addresses.createAddresses(listOf(addAddressRequest)).entities.single()

        //WHEN
        val updateStartTime = Instant.now()
        val activeLater = listOf(RelationValidityPeriod(LocalDate.now().plusDays(1), null))
        val replacedByRelation = BusinessPartnerRelations(RelationType.IsReplacedBy, createdLegalEntity.legalEntity.legalAddress.bpna, createdAddAddress.address.bpna, activeLater, anyReasonCode())
        val taskToResolve = TaskRelationsStepReservationEntryDto("Any", "Any", replacedByRelation)
        resolveRelationTasks(listOf(taskToResolve))
        val updateEndTime = Instant.now()

        //THEN
        val actualLegalEntity = poolApiClient.legalEntities.getLegalEntity(createdLegalEntity.legalEntity.header.bpnl)
        val expectedLegalEntity = LegalEntityWithLegalAddressVerboseV7Dto(
            header = createdLegalEntity.legalEntity.header,
            legalAddress = createdLegalEntity.legalEntity.legalAddress.copy(relations = createdLegalEntity.legalEntity.legalAddress.relations.plus(buildAddressReplacedByRelation(replacedByRelation))),
            scriptVariants = createdLegalEntity.legalEntity.scriptVariants
        )

        poolAssertHelper.assertLegalEntityResponse(listOf(actualLegalEntity), listOf(expectedLegalEntity), Timeframe(updateStartTime.minusSeconds(5), updateEndTime.plusSeconds(5)))
    }

    /**
     * GIVEN a legal entity and an additional address
     * WHEN additional address replaces legal address effective immediately
     * THEN sharing member sees one changelog entry per affected business partner, the legal entity before its addresses
     */
    @Test
    fun `replaced legal address logs each affected business partner once`(){
        //GIVEN
        val legalEntityRequest = requestFactory.createLegalEntityRequest(testName, true)
        val createdLegalEntity = poolApiClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        val addAddressRequest = requestFactory.buildAdditionalAddressCreateRequest("$testName 2", createdLegalEntity.legalEntity.header.bpnl).copy(scriptVariants = emptyList())
        val createdAddAddress = poolApiClient.addresses.createAddresses(listOf(addAddressRequest)).entities.single()

        val beforeRelocation = Instant.now()

        //WHEN
        val activeNow = listOf(RelationValidityPeriod(LocalDate.now(), null))
        val replacedByRelation = BusinessPartnerRelations(RelationType.IsReplacedBy, createdLegalEntity.legalEntity.legalAddress.bpna, createdAddAddress.address.bpna, activeNow, anyReasonCode())
        val taskToResolve = TaskRelationsStepReservationEntryDto("Any", "Any", replacedByRelation)
        resolveRelationTasks(listOf(taskToResolve))

        //THEN
        val searchResponse = poolApiClient.changelogs.getChangelogEntries(ChangelogSearchV7Request(timestampAfter = beforeRelocation), PaginationRequest(0, 20))

        val expectedEntries = listOf(
            ChangelogEntryVerboseV7Dto(createdLegalEntity.legalEntity.header.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogTypeV7.UPDATE),
            ChangelogEntryVerboseV7Dto(createdLegalEntity.legalEntity.legalAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV7.UPDATE),
            ChangelogEntryVerboseV7Dto(createdAddAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV7.UPDATE)
        )
        val expectedResponse = PageDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries)

        assertThat(searchResponse)
            .usingRecursiveComparison()
            .ignoringFields("${PageDto<*>::content.name}.${ChangelogEntryVerboseV7Dto::timestamp.name}")
            .isEqualTo(expectedResponse)
    }

    private fun anyReasonCode(): String{
        return poolApiClient.metadata.getReasonCodes(PaginationRequest()).content.first().technicalKey
    }

    private fun buildExpectedRelocatedHeadquarterLegalEntity(
        legalEntity: LegalEntityPartnerCreateVerboseV7Dto,
        newLegalAddress: AddressPartnerCreateVerboseV7Dto,
        replacedByRelation: BusinessPartnerRelations
        ): LegalEntityWithLegalAddressVerboseV7Dto{
        val newReplacedRelation = buildAddressReplacedByRelation(replacedByRelation)

        return LegalEntityWithLegalAddressVerboseV7Dto(
            header = legalEntity.legalEntity.header,
            legalAddress = newLegalAddress.address.copy(addressType = AddressType.LegalAddress, relations = newLegalAddress.address.relations.plus(newReplacedRelation)),
            // The new legal address covers none of the old script codes, so the legal entity keeps no script variant.
            scriptVariants = emptyList()
        )
    }

    private fun buildAddressReplacedByRelation(replacedByRelation: BusinessPartnerRelations): AddressRelationVerboseV7Dto{
        return AddressRelationVerboseV7Dto(
            type = AddressRelationTypeV7.IsReplacedBy,
            businessPartnerSourceBpna = replacedByRelation.businessPartnerSourceBpn,
            businessPartnerTargetBpna = replacedByRelation.businessPartnerTargetBpn,
            validityPeriods = replacedByRelation.validityPeriods.map { org.eclipse.tractusx.bpdm.pool.api.v7.model.relation.RelationValidityPeriodV7Dto(it.validFrom, it.validTo) },
            reasonCode = replacedByRelation.reasonCode
        )
    }

    private fun resolveRelationTasks(tasks: List<TaskRelationsStepReservationEntryDto>): List<TaskRelationsStepResultEntryDto> {
        orchestratorMockDataFactory.mockReservedRelationTasks(tasks)
        relationTaskResolutionService.resolveTasks()
        return orchestratorMockDataFactory.getRelationStepResults()
    }
}
