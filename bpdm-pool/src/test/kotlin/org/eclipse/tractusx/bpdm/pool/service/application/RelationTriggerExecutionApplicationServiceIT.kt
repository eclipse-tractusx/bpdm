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

package org.eclipse.tractusx.bpdm.pool.service.application

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.v7.client.PoolV7Client
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressRelationVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.test.containers.OrchestratorMockConfiguration
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepResultEntryDto
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.containers.OrchestratorMockContextInitializer
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerRequestFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.ExpectedBusinessPartnerResultFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolDataHelper
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestDataEnvironment
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.test.util.PoolAssertHelper
import org.eclipse.tractusx.bpdm.test.util.Timeframe
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.BusinessPartnerRelations
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.RelationType
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.RelationValidityPeriod
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepReservationEntryDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.*

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class]
)
@ActiveProfiles("test-no-auth", "test-scheduling-disabled")
@Import(OrchestratorMockConfiguration::class)
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OrchestratorMockContextInitializer::class])
class RelationTriggerExecutionApplicationServiceIT @Autowired constructor(
    private val relationTaskResolutionService: GoldenRecordRelationTaskResolutionApplicationService,
    private val orchestratorMockDataFactory: OrchestratorMockDataFactory,
    private val relationTriggerExecutionService: RelationTriggerExecutionApplicationService,
    private val poolApiClient: PoolV7Client,
    private val legalEntityRepository: LegalEntityRepository,
    private val dataHelper: PoolDataHelper,
    private val dbTestHelpers: DbTestHelpers,
    private val poolAssertHelper: PoolAssertHelper
){

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
     * GIVEN legal address to be replaced by additional address at date X
     * WHEN date X has arrived
     * THEN user sees legal entity has former additional address as legal address
     */
    @Test
    fun `event trigger invokes headquarter relocation`(){
        //GIVEN
        val creationStartTime = Instant.now()
        val legalEntityRequest = requestFactory.createLegalEntityRequest(testName, true)
        val createdLegalEntity = poolApiClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        val addAddressRequest = requestFactory.buildAdditionalAddressCreateRequest("$testName 2", createdLegalEntity.legalEntity.header.bpnl).copy(scriptVariants = emptyList())
        val createdAddAddress = poolApiClient.addresses.createAddresses(listOf(addAddressRequest)).entities.single()

        val activeLater = listOf(RelationValidityPeriod(LocalDate.now().plusDays(1), null))
        val replacedByRelation = BusinessPartnerRelations(RelationType.IsReplacedBy, createdLegalEntity.legalEntity.legalAddress.bpna, createdAddAddress.address.bpna, activeLater, anyReasonCode())
        val taskToResolve = TaskRelationsStepReservationEntryDto("Any", "Any", replacedByRelation)
        resolveRelationTasks(listOf(taskToResolve))

        //WHEN
        val creationEndTime = Instant.now()
        lateinit var updateTimeframe: Timeframe
        executeAtDate(LocalDate.now().plusDays(1)) {
            val updateStartTime = Instant.now()
            relationTriggerExecutionService.executeDueTriggers()
            updateTimeframe = Timeframe(updateStartTime.minusSeconds(2), Instant.now().plusSeconds(2))
        }

        //THEN
        val actualLegalEntity = poolApiClient.legalEntities.getLegalEntity(createdLegalEntity.legalEntity.header.bpnl)
        val expectedLegalEntity = buildExpectedRelocatedHeadquarterLegalEntity(createdLegalEntity, createdAddAddress, replacedByRelation)

        poolAssertHelper.assertLegalEntityResponse(
            listOf(actualLegalEntity),
            listOf(expectedLegalEntity),
            Timeframe(creationStartTime.minusSeconds(2), creationEndTime.plusSeconds(2)),
            updateTimeframe
        )
    }


    /**
     * GIVEN legal address to be replaced by additional address at date X
     * WHEN date X not yet arrived
     * THEN user sees legal entity legal address has not changed
     */
    @Test
    fun `not yet ready event trigger is ignored`(){
        //GIVEN
        val legalEntityRequest = requestFactory.createLegalEntityRequest(testName, true)
        val createdLegalEntity = poolApiClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        val addAddressRequest = requestFactory.buildAdditionalAddressCreateRequest("$testName 2", createdLegalEntity.legalEntity.header.bpnl)
        val createdAddAddress = poolApiClient.addresses.createAddresses(listOf(addAddressRequest)).entities.single()

        val activeLater = listOf(RelationValidityPeriod(LocalDate.now().plusDays(1), null))
        val replacedByRelation = BusinessPartnerRelations(RelationType.IsReplacedBy, createdLegalEntity.legalEntity.legalAddress.bpna, createdAddAddress.address.bpna, activeLater, anyReasonCode())
        val taskToResolve = TaskRelationsStepReservationEntryDto("Any", "Any", replacedByRelation)
        resolveRelationTasks(listOf(taskToResolve))

        val originalLegalEntity = poolApiClient.legalEntities.getLegalEntity(createdLegalEntity.legalEntity.header.bpnl)

        //WHEN
        relationTriggerExecutionService.executeDueTriggers()

        //THEN
        val actualLegalEntity = poolApiClient.legalEntities.getLegalEntity(createdLegalEntity.legalEntity.header.bpnl)
        val expectedLegalEntity = originalLegalEntity

        poolAssertHelper.assertLegalEntityResponse(listOf(actualLegalEntity), listOf(expectedLegalEntity), Timeframe(Instant.now().minusSeconds(2), Instant.now()))
    }

    @Test
    fun `alternative headquarter validity trigger recalculates ultimate owner on start date`() {
        val alternative = poolApiClient.legalEntities.createBusinessPartners(listOf(requestFactory.createLegalEntityRequest("$testName Alt", true))).entities.single()
        val main = poolApiClient.legalEntities.createBusinessPartners(listOf(requestFactory.createLegalEntityRequest("$testName Main", true))).entities.single()

        val mainDb = legalEntityRepository.findByBpnIgnoreCase(main.legalEntity.header.bpnl)!!
        mainDb.ownershipUltimate = true
        legalEntityRepository.save(mainDb)

        val activeLater = listOf(RelationValidityPeriod(LocalDate.now().plusDays(1), null))
        val alternativeRelation = BusinessPartnerRelations(
            RelationType.IsAlternativeHeadquarterFor,
            alternative.legalEntity.header.bpnl,
            main.legalEntity.header.bpnl,
            activeLater,
            anyReasonCode()
        )

        val taskToResolve = TaskRelationsStepReservationEntryDto("Any", "Any", alternativeRelation)
        resolveRelationTasks(listOf(taskToResolve))

        val alternativeBefore = poolApiClient.legalEntities.getLegalEntity(alternative.legalEntity.header.bpnl)
        assertThat(alternativeBefore.header.ultimateOwnerBpnl).isNull()

        executeAtDate(LocalDate.now().plusDays(1)) { relationTriggerExecutionService.executeDueTriggers() }

        val alternativeAfter = poolApiClient.legalEntities.getLegalEntity(alternative.legalEntity.header.bpnl)
        assertThat(alternativeAfter.header.ultimateOwnerBpnl).isEqualTo(main.legalEntity.header.bpnl)
    }

    private fun executeAtDate(executionDate: LocalDate, methodToExecute: () -> Unit){
        val offset = Duration.between(Instant.now(), executionDate.atStartOfDay(ZoneOffset.UTC).toInstant())
        val offsetClock = Clock.offset(Clock.systemUTC(), offset)

        mockkStatic(Instant::class)
        mockkStatic(Clock::class)
        every { Instant.now() } returns offsetClock.instant()
        every { Clock.systemUTC() } returns offsetClock
        every { Clock.systemDefaultZone() } returns offsetClock

        methodToExecute()
        unmockkStatic(Instant::class)
        unmockkStatic(Clock::class)
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

    private fun anyReasonCode(): String{
        return poolApiClient.metadata.getReasonCodes(PaginationRequest()).content.first().technicalKey
    }

    private fun resolveRelationTasks(tasks: List<TaskRelationsStepReservationEntryDto>): List<TaskRelationsStepResultEntryDto> {
        orchestratorMockDataFactory.mockReservedRelationTasks(tasks)
        relationTaskResolutionService.resolveTasks()
        return orchestratorMockDataFactory.getRelationStepResults()
    }
}
