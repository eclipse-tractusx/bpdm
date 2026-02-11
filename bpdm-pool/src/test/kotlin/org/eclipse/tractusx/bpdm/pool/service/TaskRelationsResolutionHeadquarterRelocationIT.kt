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

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.AddressRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.AddressRelationVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import java.time.LocalDate

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class]
)
@ActiveProfiles("test-no-auth", "test-scheduling-disabled")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class TaskRelationsResolutionHeadquarterRelocationIT @Autowired constructor(
    private val taskRelationsResolutionService: TaskRelationsResolutionService,
    private val poolApiClient: PoolApiClient,
    private val dataHelper: PoolDataHelper,
    private val dbTestHelpers: DbTestHelpers,
    private val poolAssertHelper: PoolAssertHelper
) {

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

        val addAddressRequest = requestFactory.buildAdditionalAddressCreateRequest("$testName 2", createdLegalEntity.legalEntity.bpnl)
        val createdAddAddress = poolApiClient.addresses.createAddresses(listOf(addAddressRequest)).entities.single()

        //WHEN
        val activeNow = listOf(RelationValidityPeriod(LocalDate.now(), null))
        val replacedByRelation = BusinessPartnerRelations(RelationType.IsReplacedBy, createdLegalEntity.legalAddress.bpna, createdAddAddress.address.bpna, activeNow, anyReasonCode())
        val taskToResolve = TaskRelationsStepReservationEntryDto("Any", "Any", replacedByRelation)
        taskRelationsResolutionService.upsertRelationsGoldenRecordIntoPool(listOf(taskToResolve))

        //THEN
        val actualLegalEntity = poolApiClient.legalEntities.getLegalEntity(createdLegalEntity.legalEntity.bpnl)
        val expectedLegalEntity = buildExpectedRelocatedHeadquarterLegalEntity(createdLegalEntity, createdAddAddress, replacedByRelation)

        poolAssertHelper.assertLegalEntityResponse(listOf(actualLegalEntity), listOf(expectedLegalEntity), Timeframe(Instant.now().minusSeconds(2), Instant.now()))
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

        val addAddressRequest = requestFactory.buildAdditionalAddressCreateRequest("$testName 2", createdLegalEntity.legalEntity.bpnl)
        val createdAddAddress = poolApiClient.addresses.createAddresses(listOf(addAddressRequest)).entities.single()

        //WHEN
        val activeLater = listOf(RelationValidityPeriod(LocalDate.now().plusDays(1), null))
        val replacedByRelation = BusinessPartnerRelations(RelationType.IsReplacedBy, createdLegalEntity.legalAddress.bpna, createdAddAddress.address.bpna, activeLater, anyReasonCode())
        val taskToResolve = TaskRelationsStepReservationEntryDto("Any", "Any", replacedByRelation)
        taskRelationsResolutionService.upsertRelationsGoldenRecordIntoPool(listOf(taskToResolve))

        //THEN
        val actualLegalEntity = poolApiClient.legalEntities.getLegalEntity(createdLegalEntity.legalEntity.bpnl)
        val expectedLegalEntity = LegalEntityWithLegalAddressVerboseDto(
            createdLegalEntity.legalEntity,
            createdLegalEntity.legalAddress.copy(relations = createdLegalEntity.legalAddress.relations.plus(buildAddressReplacedByRelation(replacedByRelation)))
        )

        poolAssertHelper.assertLegalEntityResponse(listOf(actualLegalEntity), listOf(expectedLegalEntity), Timeframe(Instant.now().minusSeconds(2), Instant.now()))
    }

    private fun anyReasonCode(): String{
        return poolApiClient.metadata.getReasonCodes(PaginationRequest()).content.first().technicalKey
    }

    private fun buildExpectedRelocatedHeadquarterLegalEntity(
        legalEntity: LegalEntityPartnerCreateVerboseDto,
        newLegalAddress: AddressPartnerCreateVerboseDto,
        replacedByRelation: BusinessPartnerRelations
        ): LegalEntityWithLegalAddressVerboseDto{
        val newReplacedRelation = buildAddressReplacedByRelation(replacedByRelation)

        return LegalEntityWithLegalAddressVerboseDto(
            legalEntity = legalEntity.legalEntity,
            legalAddress = newLegalAddress.address.copy(addressType = AddressType.LegalAddress, relations = newLegalAddress.address.relations.plus(newReplacedRelation))
        )
    }

    private fun buildAddressReplacedByRelation(replacedByRelation: BusinessPartnerRelations): AddressRelationVerboseDto{
        return AddressRelationVerboseDto(
            type = AddressRelationType.IsReplacedBy,
            businessPartnerSourceBpna = replacedByRelation.businessPartnerSourceBpn,
            businessPartnerTargetBpna = replacedByRelation.businessPartnerTargetBpn,
            validityPeriods = replacedByRelation.validityPeriods.map { org.eclipse.tractusx.bpdm.pool.api.model.RelationValidityPeriod(it.validFrom, it.validTo) },
            reasonCode = replacedByRelation.reasonCode
        )
    }
}