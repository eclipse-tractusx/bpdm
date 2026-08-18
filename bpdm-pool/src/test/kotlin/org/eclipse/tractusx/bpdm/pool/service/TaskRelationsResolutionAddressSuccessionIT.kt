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
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.AddressRelationType
import org.eclipse.tractusx.bpdm.test.containers.OrchestratorMockConfiguration
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerRequestFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolDataHelper
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestDataEnvironment
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
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
import java.time.LocalDate

/**
 * A succession between addresses relocates the headquarters only where it starts at the legal address, which
 * TaskRelationsResolutionHeadquarterRelocationIT covers. This class covers the successions that leave the involved
 * business partners as they are.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class]
)
@ActiveProfiles("test-no-auth", "test-scheduling-disabled")
@Import(OrchestratorMockConfiguration::class)
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class TaskRelationsResolutionAddressSuccessionIT @Autowired constructor(
    private val taskRelationsResolutionService: TaskRelationsResolutionService,
    private val poolApiClient: PoolApiClient,
    private val dataHelper: PoolDataHelper,
    private val dbTestHelpers: DbTestHelpers
) {

    private lateinit var testDataEnvironment: TestDataEnvironment
    private lateinit var testName: String
    private lateinit var requestFactory: BusinessPartnerRequestFactory

    @BeforeEach
    fun beforeEach(testInfo: TestInfo) {
        dbTestHelpers.truncateDbTables()
        testDataEnvironment = dataHelper.createTestDataEnvironment()
        requestFactory = testDataEnvironment.requestFactory
        testName = testInfo.displayName
    }

    /**
     * GIVEN a legal entity with two additional addresses
     * WHEN one additional address replaces the other effective immediately
     * THEN user sees the legal entity keeps its legal address and both addresses keep their address type
     */
    @Test
    fun `succession between two additional addresses keeps headquarter and address types`() {
        //GIVEN
        val legalEntityRequest = requestFactory.createLegalEntityRequest(testName, true)
        val createdLegalEntity = poolApiClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()
        val legalEntityBpnl = createdLegalEntity.legalEntity.header.bpnl

        val predecessorRequest = requestFactory.buildAdditionalAddressCreateRequest("$testName A", legalEntityBpnl).copy(scriptVariants = emptyList())
        val successorRequest = requestFactory.buildAdditionalAddressCreateRequest("$testName B", legalEntityBpnl).copy(scriptVariants = emptyList())
        val createdAddresses = poolApiClient.addresses.createAddresses(listOf(predecessorRequest, successorRequest)).entities.toList()
        val predecessor = createdAddresses[0]
        val successor = createdAddresses[1]

        //WHEN
        resolveSuccession(predecessor.address.bpna, successor.address.bpna)

        //THEN
        val actualLegalEntity = poolApiClient.legalEntities.getLegalEntity(legalEntityBpnl)
        assertThat(actualLegalEntity.legalAddress.bpna).isEqualTo(createdLegalEntity.legalEntity.legalAddress.bpna)

        assertThat(addressTypeOf(predecessor.address.bpna)).isEqualTo(AddressType.AdditionalAddress)
        assertThat(addressTypeOf(successor.address.bpna)).isEqualTo(AddressType.AdditionalAddress)
        assertThat(successionsOf(predecessor.address.bpna)).hasSize(1)
    }

    /**
     * GIVEN a legal entity and an additional address
     * WHEN the legal address replaces the additional address effective immediately
     * THEN user sees the legal entity keeps its legal address and both addresses keep their address type
     */
    @Test
    fun `succession from additional address to legal address keeps headquarter and address types`() {
        //GIVEN
        val legalEntityRequest = requestFactory.createLegalEntityRequest(testName, true)
        val createdLegalEntity = poolApiClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()
        val legalAddressBpna = createdLegalEntity.legalEntity.legalAddress.bpna

        val predecessorRequest = requestFactory
            .buildAdditionalAddressCreateRequest("$testName A", createdLegalEntity.legalEntity.header.bpnl)
            .copy(scriptVariants = emptyList())
        val predecessor = poolApiClient.addresses.createAddresses(listOf(predecessorRequest)).entities.single()

        //WHEN
        resolveSuccession(predecessor.address.bpna, legalAddressBpna)

        //THEN
        val actualLegalEntity = poolApiClient.legalEntities.getLegalEntity(createdLegalEntity.legalEntity.header.bpnl)
        assertThat(actualLegalEntity.legalAddress.bpna).isEqualTo(legalAddressBpna)

        assertThat(addressTypeOf(predecessor.address.bpna)).isEqualTo(AddressType.AdditionalAddress)
        assertThat(addressTypeOf(legalAddressBpna)).isEqualTo(AddressType.LegalAddress)
        assertThat(successionsOf(predecessor.address.bpna)).hasSize(1)
    }

    private fun resolveSuccession(predecessorBpna: String, successorBpna: String) {
        val activeNow = listOf(RelationValidityPeriod(LocalDate.now(), null))
        val succession = BusinessPartnerRelations(RelationType.IsReplacedBy, predecessorBpna, successorBpna, activeNow, anyReasonCode())
        val taskToResolve = TaskRelationsStepReservationEntryDto("Any", "Any", succession)

        val results = taskRelationsResolutionService.upsertRelationsGoldenRecordIntoPool(listOf(taskToResolve))

        assertThat(results.single().errors).isEmpty()
    }

    private fun addressTypeOf(bpna: String) = poolApiClient.addresses.getAddress(bpna).address.addressType

    private fun successionsOf(bpna: String) =
        poolApiClient.addresses.getAddress(bpna).address.relations.filter { it.type == AddressRelationType.IsReplacedBy }

    private fun anyReasonCode(): String {
        return poolApiClient.metadata.getReasonCodes(PaginationRequest()).content.first().technicalKey
    }
}
