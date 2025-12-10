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
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.AddressRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolDataHelper
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestDataEnvironment
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.util.*

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class]
)
@ActiveProfiles("test-no-auth")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class TaskRelationsStepBuildDispatcherServiceIT @Autowired constructor(
    val taskRelationsStepBuildDispatcherService: TaskRelationsStepBuildDispatcherService,
    val poolClient: PoolApiClient,
    private val dataHelper: PoolDataHelper,
    private val dbTestHelpers: DbTestHelpers
) {

    private lateinit var testDataEnvironment: TestDataEnvironment
    private lateinit var testName: String

    @BeforeEach
    fun beforeEach(testInfo: TestInfo) {
        dbTestHelpers.truncateDbTables()
        testDataEnvironment = dataHelper.createTestDataEnvironment()
        testName = testInfo.displayName
    }

    @ParameterizedTest
    @EnumSource(LegalEntityRelationType::class)
    fun `route legal entity relation to legal entity relation service`(relationType: LegalEntityRelationType) {
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        val createLegalEntityRelationsRequest = BusinessPartnerRelations(
            relationType = relationType.toTaskDto(),
            businessPartnerSourceBpn = legalEntity1.legalEntity.bpnl,
            businessPartnerTargetBpn = legalEntity2.legalEntity.bpnl,
            validityPeriods = listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.parse("1970-01-01"),
                    validTo = LocalDate.parse("9999-12-31")
                )
            )
        )

        val result = upsertBusinessPartnerRelations(taskId = "TASK_1", businessPartnerRelations = createLegalEntityRelationsRequest)
        assertThat(result.taskId).isEqualTo("TASK_1")
        assertThat(result.businessPartnerRelations.businessPartnerSourceBpn).contains("BPNL")
        assertThat(result.errors.size).isEqualTo(0)
    }

    @ParameterizedTest
    @EnumSource(AddressRelationType::class)
    fun `route address relation to address relation service`(relationType: AddressRelationType) {
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)

        val createAddressRelationsRequest = BusinessPartnerRelations(
            relationType = relationType.toTaskDto(),
            businessPartnerSourceBpn = legalEntity1.legalAddress.bpna,
            businessPartnerTargetBpn = additionalAddress1.address.bpna,
            validityPeriods = listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.parse("1970-01-01"),
                    validTo = LocalDate.parse("9999-12-31")
                )
            )
        )

        val result = upsertBusinessPartnerRelations(taskId = "TASK_1", businessPartnerRelations = createAddressRelationsRequest)
        assertThat(result.taskId).isEqualTo("TASK_1")
        assertThat(result.businessPartnerRelations.businessPartnerSourceBpn).isEqualTo(legalEntity1.legalAddress.bpna)
        assertThat(result.errors.size).isEqualTo(0)
    }

    private fun createLegalEntity(seed: String): LegalEntityPartnerCreateVerboseDto {
        val request = testDataEnvironment.requestFactory.createLegalEntityRequest(seed, true)
        return poolClient.legalEntities.createBusinessPartners(listOf(request)).entities.single()
    }

    private fun createAdditionalAddress(seed: String, legalEntity: LegalEntityPartnerCreateVerboseDto): AddressPartnerCreateVerboseDto{
        val request = testDataEnvironment.requestFactory.buildAdditionalAddressCreateRequest(seed, legalEntity.legalEntity.bpnl)
        return poolClient.addresses.createAddresses(listOf(request)).entities.single()
    }

    private fun upsertBusinessPartnerRelations(taskId: String, businessPartnerRelations: BusinessPartnerRelations) : TaskRelationsStepResultEntryDto {
        val taskEntry = singleTaskStep(taskId, businessPartnerRelations)
        return taskRelationsStepBuildDispatcherService.upsertBusinessPartnerRelations(taskEntry)
    }

    private fun singleTaskStep(taskId: String, businessPartnerRelations: BusinessPartnerRelations): TaskRelationsStepReservationEntryDto {

        return TaskRelationsStepReservationEntryDto(
                taskId = taskId,
                recordId = UUID.randomUUID().toString(),
                businessPartnerRelations = businessPartnerRelations
        )
    }

    private fun LegalEntityRelationType.toTaskDto(): RelationType {
        return when(this){
            LegalEntityRelationType.IsAlternativeHeadquarterFor -> RelationType.IsAlternativeHeadquarterFor
            LegalEntityRelationType.IsManagedBy -> RelationType.IsManagedBy
            LegalEntityRelationType.IsOwnedBy -> RelationType.IsOwnedBy
        }
    }

    private fun AddressRelationType.toTaskDto(): RelationType {
        return when(this){
            AddressRelationType.IsReplacedBy -> RelationType.IsReplacedBy
        }
    }

}