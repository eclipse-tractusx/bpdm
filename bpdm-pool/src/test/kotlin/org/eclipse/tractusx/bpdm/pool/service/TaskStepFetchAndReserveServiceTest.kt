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

package org.eclipse.tractusx.bpdm.pool.service

import com.neovisionaries.i18n.CountryCode
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.ILegalEntityStateDto
import org.eclipse.tractusx.bpdm.common.dto.ISiteStateDto
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.service.TaskStepBuildService.CleaningError

import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerNonVerboseValues.addressIdentifierTypeDto1
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerNonVerboseValues.addressIdentifierTypeDto2
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.test.util.PoolDataHelpers
import org.eclipse.tractusx.orchestrator.api.model.*
import org.eclipse.tractusx.orchestrator.api.model.AddressIdentifierDto
import org.eclipse.tractusx.orchestrator.api.model.AddressStateDto
import org.eclipse.tractusx.orchestrator.api.model.AlternativePostalAddressDto
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType.Bpn
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType.BpnRequestIdentifier
import org.eclipse.tractusx.orchestrator.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityClassificationDto
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityDto
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityIdentifierDto
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityStateDto
import org.eclipse.tractusx.orchestrator.api.model.LogisticAddressDto
import org.eclipse.tractusx.orchestrator.api.model.PhysicalPostalAddressDto
import org.eclipse.tractusx.orchestrator.api.model.SiteDto
import org.eclipse.tractusx.orchestrator.api.model.SiteStateDto
import org.eclipse.tractusx.orchestrator.api.model.StreetDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.function.BiPredicate


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class]
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class TaskStepFetchAndReserveServiceTest @Autowired constructor(
    val cleaningStepService: TaskStepFetchAndReserveService,
    val bpnRequestIdentifierRepository: BpnRequestIdentifierRepository,
    val poolClient: PoolClientImpl,
    val dbTestHelpers: DbTestHelpers,
    val poolDataHelpers: PoolDataHelpers,
) {


    @BeforeEach
    fun beforeEach() {
        dbTestHelpers.truncateDbTables()
        poolDataHelpers.createPoolMetadata()
    }

    @Test
    fun `create empty legal entity`() {

        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = emptyLegalEntity()
        )

        val result = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].errors.size).isEqualTo(2)
        assertThat(result[0].errors[0].description).isEqualTo(CleaningError.LEGAL_ENTITY_IS_NULL.message)
    }

    @Test
    fun `create legal entity without legal name`() {

        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = emptyLegalEntity().copy(
                bpnLReference = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier),
                legalAddress =  minLogisticAddress(BpnReferenceDto(referenceValue = "A777", referenceType = BpnRequestIdentifier))
            )
        )

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertTaskError(createResult[0], "TASK_1", CleaningError.LEGAL_NAME_IS_NULL)
    }

    @Test
    fun `create min legal entity`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val bpnMappings = bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(listOf(leRefValue, leAddressRefValue))
        assertThat(bpnMappings.size).isEqualTo(2)

        val createdLegalEntity = poolClient.legalEntities.getLegalEntity(createResult[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)
        assertThat(createdLegalEntity.legalAddress.bpnLegalEntity).isNotNull()
        assertThat(createResult[0].businessPartner?.generic?.legalEntity?.legalEntityBpn).isEqualTo(createdLegalEntity.legalEntity.bpnl)
        compareLegalEntity(createdLegalEntity, createResult[0].businessPartner?.legalEntity)
    }

    @Test
    fun `create legal entity with all fields`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = fullValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val bpnMappings = bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(listOf(leRefValue, leAddressRefValue))
        assertThat(bpnMappings.size).isEqualTo(2)

        val createdLegalEntity = poolClient.legalEntities.getLegalEntity(createResult[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)
        assertThat(createdLegalEntity.legalAddress.bpnLegalEntity).isNotNull()
        assertThat(createResult[0].businessPartner?.generic?.legalEntity?.legalEntityBpn).isEqualTo(createdLegalEntity.legalEntity.bpnl)
        compareLegalEntity(createdLegalEntity, createResult[0].businessPartner?.legalEntity)
    }


    @Test
    fun `create legal entity with additional address`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val additionalAddressRefValue = "333"
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            ),
            address = minLogisticAddress(BpnReferenceDto(referenceValue = additionalAddressRefValue, referenceType = BpnRequestIdentifier))
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val bpnMappings = bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(listOf(leRefValue, leAddressRefValue, additionalAddressRefValue))
        assertThat(bpnMappings.size).isEqualTo(3)

        val createdLegalEntity = poolClient.legalEntities.getLegalEntity(createResult[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)
        assertThat(createdLegalEntity.legalAddress.bpnLegalEntity).isEqualTo(createdLegalEntity.legalEntity.bpnl)
        assertThat(createdLegalEntity.legalAddress.isLegalAddress).isTrue()
        assertThat(createdLegalEntity.legalAddress.isMainAddress).isFalse()
        assertThat(createResult[0].businessPartner?.generic?.legalEntity?.legalEntityBpn).isEqualTo(createdLegalEntity.legalEntity.bpnl)
        compareLegalEntity(createdLegalEntity, createResult[0].businessPartner?.legalEntity)
        val createdAdditionalAddress = poolClient.addresses.getAddress(createResult[0].businessPartner?.address?.bpnAReference?.referenceValue!!)
        assertThat(createdAdditionalAddress.bpnLegalEntity).isEqualTo(createdLegalEntity.legalEntity.bpnl)
        assertThat(createdAdditionalAddress.isLegalAddress).isFalse()
        assertThat(createdAdditionalAddress.isMainAddress).isFalse()
    }

    @Test
    fun `create legal entity with invalid identifiers`() {

        val leRef = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier)
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = leRef,
                bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
            ).copy(
                identifiers = listOf(
                    legalEntityIdentifierDto(leRef.referenceValue, 1L, BusinessPartnerVerboseValues.identifierType1),
                    legalEntityIdentifierDto(leRef.referenceValue, 2L, TypeKeyNameVerboseDto("Invalid", "Invalid"))
                )
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
        assertThat(createResult[0].errors[0].description).isEqualTo("Legal Entity Identifier Type 'Invalid' does not exist")
    }

    @Test
    fun `create legal entity with invalid legal form`() {

        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
            ).copy(
                legalForm = "Invalid Form"
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
        assertThat(createResult[0].errors[0].description).isEqualTo("Legal Form 'Invalid Form' does not exist")
    }

    @Test
    fun `create legal entity with invalid duplicate identifier`() {

        val leRef = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier)
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = leRef,
                bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
            ).copy(
                identifiers = listOf(
                    legalEntityIdentifierDto(leRef.referenceValue, 1L, BusinessPartnerVerboseValues.identifierType1),
                    legalEntityIdentifierDto(leRef.referenceValue, 1L, BusinessPartnerVerboseValues.identifierType1)
                ),
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
        assertThat(createResult[0].errors[0].description).isEqualTo("Duplicate Legal Entity Identifier: Value 'value_123_1' of type 'VAT_DE'")
    }

    @Test
    fun `create 2 legal entities with invalid duplicate identifier`() {

        val leRef1 = BpnReferenceDto(referenceValue = "111", referenceType = BpnRequestIdentifier)
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = leRef1,
                bpnAReference = BpnReferenceDto(referenceValue = "444", referenceType = BpnRequestIdentifier)
            ).copy(
                identifiers = listOf(
                    legalEntityIdentifierDto(leRef1.referenceValue, 1L, BusinessPartnerVerboseValues.identifierType1),
                    legalEntityIdentifierDto(leRef1.referenceValue, 2L, BusinessPartnerVerboseValues.identifierType2)
                ),
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val leRef2 = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
        val createLegalEntityRequest2 = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = leRef2,
                bpnAReference = BpnReferenceDto(referenceValue = "333", referenceType = BpnRequestIdentifier)
            ).copy(
                identifiers = listOf(
                    legalEntityIdentifierDto(leRef1.referenceValue, 1L, BusinessPartnerVerboseValues.identifierType1),
                ),
            )
        )
        val resultSteps2 = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = createLegalEntityRequest2)
        assertThat(resultSteps2[0].taskId).isEqualTo("TASK_2")
        assertThat(resultSteps2[0].errors.size).isEqualTo(1)
        assertThat(resultSteps2[0].errors[0].description).isEqualTo("Duplicate Legal Entity Identifier: Value 'value_111_1' of type 'VAT_DE'")
    }

    @Test
    fun `create legal entity with invalid address administrativeAreaLevel1`() {

        val bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier),
                bpnAReference = bpnAReference
            ).copy(
                legalAddress = minLogisticAddress(bpnAReference).copy(
                    physicalPostalAddress = minPhysicalPostalAddressDto(bpnAReference).copy(
                        administrativeAreaLevel1 = "Invalid"
                    )
                )
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
        assertThat(createResult[0].errors[0].description).isEqualTo("Address administrative area level1 'Invalid' does not exist")
    }

    @Test
    fun `create legal entity with invalid address identifier`() {

        val bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier),
                bpnAReference = bpnAReference
            ).copy(
                legalAddress = minLogisticAddress(bpnAReference).copy(
                    identifiers = listOf(
                        addressIdentifierDto(bpnAReference.referenceValue, 1L, TypeKeyNameVerboseDto(addressIdentifierTypeDto1.technicalKey, "")),
                        addressIdentifierDto(bpnAReference.referenceValue, 2L, TypeKeyNameVerboseDto("Invalid Ident", ""))
                    ),
                )
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
        assertThat(createResult[0].errors[0].description).isEqualTo("Address Identifier Type 'Invalid Ident' does not exist")
    }

    @Test
    fun `create legal entity with invalid duplicated address identifier`() {

        val bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier),
                bpnAReference = bpnAReference
            ).copy(
                legalAddress = minLogisticAddress(bpnAReference).copy(
                    identifiers = listOf(
                        addressIdentifierDto(bpnAReference.referenceValue, 1L, TypeKeyNameVerboseDto(addressIdentifierTypeDto1.technicalKey, "")),
                        addressIdentifierDto(bpnAReference.referenceValue, 1L, TypeKeyNameVerboseDto(addressIdentifierTypeDto1.technicalKey, ""))
                    ),
                )
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
        assertThat(createResult[0].errors[0].description).isEqualTo("Duplicate Address Identifier: Value 'value_222_1' of type 'ADDR_KEY_ONE'")
    }


    @Test
    fun `check that requests with same referenceValue don't create a new legal entity`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)
        val createdLegalEntity1 = poolClient.legalEntities.getLegalEntity(createResult[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)

        val resultSteps2 = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = createLegalEntityRequest)
        assertThat(resultSteps2[0].taskId).isEqualTo("TASK_2")
        assertThat(resultSteps2[0].errors.size).isEqualTo(0)
        assertThat(createdLegalEntity1.legalEntity.bpnl).isEqualTo(resultSteps2[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)
    }

    @Test
    fun `create legal entity with different referenceValues `() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)
        val createdLegalEntity1 = poolClient.legalEntities.getLegalEntity(createResult[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)

        val leRefValue2 = "diffenrentBpnL"
        val leAddressRefValue2 = "diffenrentBpnA"
        val createLegalEntityRequest2 = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue2, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue2, referenceType = BpnRequestIdentifier)
            )
        )

        val resultSteps2 = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = createLegalEntityRequest2)
        val bpnMappings =
            bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(listOf(leRefValue, leAddressRefValue, leRefValue2, leAddressRefValue2))
        assertThat(bpnMappings.size).isEqualTo(4)

        assertThat(resultSteps2[0].taskId).isEqualTo("TASK_2")
        assertThat(resultSteps2[0].errors.size).isEqualTo(0)
        assertThat(createdLegalEntity1.legalEntity.bpnl).isNotEqualTo(resultSteps2[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)
        val createdLegalEntity2 = poolClient.legalEntities.getLegalEntity(resultSteps2[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)
        assertThat(resultSteps2[0].businessPartner?.generic?.legalEntity?.legalEntityBpn).isEqualTo(createdLegalEntity2.legalEntity.bpnl)
    }

    @Test
    fun `update legal entity with all fields by BPN`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = fullValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val updateLegalEntityRequest = createResult[0].businessPartner?.copy(
            legalEntity = createResult[0].businessPartner?.legalEntity?.copy(
                hasChanged = true,
                legalName = "Changed Legal Entity",
                legalAddress = createResult[0].businessPartner?.legalEntity?.legalAddress?.copy(
                    hasChanged = true,
                    name = "Changed Address Name"
                )
            )
        )
        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateLegalEntityRequest!!)
        assertThat(updateResult[0].taskId).isEqualTo("TASK_2")
        assertThat(updateResult[0].errors.size).isEqualTo(0)

        val updatedLegalEntity = poolClient.legalEntities.getLegalEntity(updateResult[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)
        assertThat(updatedLegalEntity.legalEntity.legalName).isEqualTo("Changed Legal Entity")
        compareLegalEntity(updatedLegalEntity, updateResult[0].businessPartner?.legalEntity)
    }

    @Test
    fun `update legal entity with all fields by BpnRequestIdentifier`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = fullValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)

        // Update legal entity with same BpnRequestIdentifier
        val updateLegalEntityRequest = createLegalEntityRequest.copy(
            legalEntity = createLegalEntityRequest.legalEntity?.copy(
                hasChanged = true,
                legalName = "Changed Legal Entity",
                legalAddress = createLegalEntityRequest.legalEntity?.legalAddress?.copy(
                    hasChanged = true,
                    name = "Changed Address Name"
                )
            )
        )
        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateLegalEntityRequest)
        assertThat(updateResult[0].taskId).isEqualTo("TASK_2")
        assertThat(updateResult[0].errors.size).isEqualTo(0)

        val updatedLegalEntity = poolClient.legalEntities.getLegalEntity(updateResult[0].businessPartner?.legalEntity?.bpnLReference?.referenceValue!!)
        assertThat(updatedLegalEntity.legalEntity.legalName).isEqualTo("Changed Legal Entity")
        compareLegalEntity(updatedLegalEntity, updateResult[0].businessPartner?.legalEntity)
    }

    @Test
    fun `update legal entity invalid identifier type `() {

        val leRef = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier)
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = leRef,
                bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val updateLegalEntityRequest = createResult[0].businessPartner?.copy(
            legalEntity = createResult[0].businessPartner?.legalEntity?.copy(
                hasChanged = true,
                legalName = "Changed Legal Entity",
                identifiers = listOf(
                    legalEntityIdentifierDto(leRef.referenceValue, 1L, BusinessPartnerVerboseValues.identifierType1),
                    legalEntityIdentifierDto(leRef.referenceValue, 2L, TypeKeyNameVerboseDto("Invalid", "Invalid"))
                ),
                legalAddress = createResult[0].businessPartner?.legalEntity?.legalAddress?.copy(
                    hasChanged = true,
                    name = "Changed Address Name"
                )
            )
        )
        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateLegalEntityRequest!!)
        assertThat(updateResult[0].taskId).isEqualTo("TASK_2")
        assertThat(updateResult[0].errors.size).isEqualTo(1)
        assertThat(updateResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
        assertThat(updateResult[0].errors[0].description).isEqualTo("Legal Entity Identifier Type 'Invalid' does not exist")
    }

    @Test
    fun `update legal entity invalid legal form `() {

        val leRef = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier)
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = leRef,
                bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val updateLegalEntityRequest = createResult[0].businessPartner?.copy(
            legalEntity = createResult[0].businessPartner?.legalEntity?.copy(
                hasChanged = true,
                legalName = "Changed Legal Entity",
                legalForm = "Invalid Form",
                legalAddress = createResult[0].businessPartner?.legalEntity?.legalAddress?.copy(
                    hasChanged = true,
                    name = "Changed Address Name"
                )
            )
        )
        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateLegalEntityRequest!!)
        assertThat(updateResult[0].taskId).isEqualTo("TASK_2")
        assertThat(updateResult[0].errors.size).isEqualTo(1)
        assertThat(updateResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
        assertThat(updateResult[0].errors[0].description).isEqualTo("Legal Form 'Invalid Form' does not exist")
    }

    @Test
    fun `update legal entity not existing bpn `() {

        val leRef = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier)
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = leRef,
                bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val updateLegalEntityRequest = createResult[0].businessPartner?.copy(
            legalEntity = createResult[0].businessPartner?.legalEntity?.copy(
                bpnLReference = BpnReferenceDto(referenceValue = "InvalidBPN", referenceType = Bpn),
                hasChanged = true,
                legalName = "Changed Legal Entity",
                legalAddress = createResult[0].businessPartner?.legalEntity?.legalAddress?.copy(
                    hasChanged = true,
                    name = "Changed Address Name"
                )
            )
        )
        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateLegalEntityRequest!!)
        assertThat(updateResult[0].taskId).isEqualTo("TASK_2")
        assertThat(updateResult[0].errors.size).isEqualTo(1)
        assertThat(updateResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
        assertThat(updateResult[0].errors[0].description).isEqualTo("Business Partner with BPN 'InvalidBPN' can't be updated as it doesn't exist")
    }

    @Test
    fun `update legal entity with changed identifiers `() {

        val leRef = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier)
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = leRef,
                bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier),
            ).copy(
                identifiers = listOf(
                    legalEntityIdentifierDto(leRef.referenceValue, 1L, BusinessPartnerVerboseValues.identifierType1),
                    legalEntityIdentifierDto(leRef.referenceValue, 2L, BusinessPartnerVerboseValues.identifierType2)
                )
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val updateLegalEntityRequest = createResult[0].businessPartner?.copy(
            legalEntity = createResult[0].businessPartner?.legalEntity?.copy(
                identifiers = listOf(
                    legalEntityIdentifierDto(leRef.referenceValue, 3L, BusinessPartnerVerboseValues.identifierType3),
                    legalEntityIdentifierDto(leRef.referenceValue, 2L, BusinessPartnerVerboseValues.identifierType2)
                ),
                hasChanged = true,
                legalName = "Changed Legal Entity",
                legalAddress = createResult[0].businessPartner?.legalEntity?.legalAddress?.copy(
                    hasChanged = true,
                    name = "Changed Address Name"
                )
            )
        )
        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateLegalEntityRequest!!)
        assertThat(updateResult[0].taskId).isEqualTo("TASK_2")
        assertThat(updateResult[0].errors.size).isEqualTo(0)
    }

    @Test
    fun `update legal entity by reference value with invalid identifier type `() {

        val leRef = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier)
        val createLegalEntityRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = leRef,
                bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val updateLegalEntityRequest = createLegalEntityRequest.copy(
            legalEntity = createLegalEntityRequest.legalEntity?.copy(
                hasChanged = true,
                legalName = "Changed Legal Entity",
                identifiers = listOf(
                    legalEntityIdentifierDto(leRef.referenceValue, 1L, BusinessPartnerVerboseValues.identifierType1),
                    legalEntityIdentifierDto(leRef.referenceValue, 2L, TypeKeyNameVerboseDto("Invalid", "Invalid"))
                ),
                legalAddress = createLegalEntityRequest.legalEntity?.legalAddress?.copy(
                    hasChanged = true,
                    name = "Changed Address Name"
                )
            )
        )
        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateLegalEntityRequest)
        assertThat(updateResult[0].taskId).isEqualTo("TASK_2")
        assertThat(updateResult[0].errors).hasSize(1)
        assertThat(updateResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
        assertThat(updateResult[0].errors[0].description).isEqualTo("Legal Entity Identifier Type 'Invalid' does not exist")
    }

    @Test
    fun `create Site with minimal fields`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val siteRefValue = "siteRefValue"
        val mainAddressRefValue = "mainAddressRefValue"
        val createSiteRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            ),
            site = minValidSite(
                bpnSReference = BpnReferenceDto(referenceValue = siteRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = mainAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)
        val createdSite = poolClient.sites.getSite(createResult[0].businessPartner?.site?.bpnSReference?.referenceValue!!)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors).hasSize(0)
        assertThat(createdSite.site.name).isEqualTo(createSiteRequest.site?.name)
    }

    @Test
    fun `create Site with all fields`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val siteRefValue = "siteRefValue"
        val mainAddressRefValue = "mainAddressRefValue"
        val createSiteRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            ),
            site = fullValidSite(
                bpnSReference = BpnReferenceDto(referenceValue = siteRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = mainAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)
        val createdSite = poolClient.sites.getSite(createResult[0].businessPartner?.site?.bpnSReference?.referenceValue!!)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors).hasSize(0)
        assertThat(createdSite.site.name).isEqualTo(createSiteRequest.site?.name)
        compareSite(createdSite, createResult[0].businessPartner?.site)
    }

    @Test
    fun `create site with additional address`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val siteRefValue = "siteRefValue"
        val mainAddressRefValue = "mainAddressRefValue"
        val additionalAddressRefValue = "77"
        val createSiteRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            ),
            site = minValidSite(
                bpnSReference = BpnReferenceDto(referenceValue = siteRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = mainAddressRefValue, referenceType = BpnRequestIdentifier)
            ),
            address = minLogisticAddress(
                bpnAReference = BpnReferenceDto(referenceValue = additionalAddressRefValue, referenceType = BpnRequestIdentifier),
            )
        )

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)
        val createdLeAddress = poolClient.addresses.getAddress(createResult[0].businessPartner?.legalEntity?.legalAddress?.bpnAReference?.referenceValue!!)
        val createdAdditionalAddress = poolClient.addresses.getAddress(createResult[0].businessPartner?.address?.bpnAReference?.referenceValue!!)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors).hasSize(0)
        assertThat(createdLeAddress.name).isEqualTo(createSiteRequest.address?.name)
        compareLogisticAddress(createdAdditionalAddress, createResult[0].businessPartner?.address)
        assertThat(createdAdditionalAddress.bpnLegalEntity).isNull()
        assertThat(createdAdditionalAddress.bpnSite).isEqualTo(createResult[0].businessPartner?.site?.bpnSReference?.referenceValue)
        assertThat(createdAdditionalAddress.isLegalAddress).isFalse()
        assertThat(createdAdditionalAddress.isMainAddress).isFalse()
    }


    @Test
    fun `create Site without main address`() {

        val createSiteRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
            ),
            site = fullValidSite(
                bpnSReference = BpnReferenceDto(referenceValue = "siteRefValue", referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = "mainAddressRefValue", referenceType = BpnRequestIdentifier)
            ).copy(
                mainAddress = null
            )
        )

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)

        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors).hasSize(1)
        assertThat(createResult[0].errors[0].description).isEqualTo("Site main address or BpnA Reference is Empty")
    }

    @Test
    fun `create Site with invalid addresses administration level 1 and invalid identifier`() {

        val bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
        val additionalReference = BpnReferenceDto(referenceValue = "additionalRef", referenceType = BpnRequestIdentifier)
        val createSiteRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
            ),
            site = fullValidSite(
                bpnSReference = BpnReferenceDto(referenceValue = "siteRefValue", referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = "mainAddressRefValue", referenceType = BpnRequestIdentifier)
            ).copy(
                mainAddress = minLogisticAddress(bpnAReference).copy(
                    physicalPostalAddress = minPhysicalPostalAddressDto(bpnAReference).copy(
                        administrativeAreaLevel1 = "Invalid"
                    ),
                    identifiers = listOf(
                        addressIdentifierDto(bpnAReference.referenceValue, 1L, TypeKeyNameVerboseDto("InvalidKey1", "InvalidName1")),
                    )
                )
            ),
            address = minLogisticAddress(additionalReference).copy(
                physicalPostalAddress = minPhysicalPostalAddressDto(additionalReference).copy(
                    administrativeAreaLevel1 = "InvalidAdditional"
                ),
                identifiers = listOf(
                    addressIdentifierDto(additionalReference.referenceValue, 2L, TypeKeyNameVerboseDto("InvalidKey2", "InvalidName2")),
                )
            ),

            )

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)

        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        val errorDescriptions = createResult[0].errors.map { it.description }
        assertThat(errorDescriptions).containsExactlyInAnyOrder(
            "Address administrative area level1 'Invalid' does not exist",
            "Address administrative area level1 'InvalidAdditional' does not exist",
            "Address Identifier Type 'InvalidKey1' does not exist",
            "Address Identifier Type 'InvalidKey2' does not exist"
        )
    }

    @Test
    fun `create Site with same identifier in main address and additional address`() {

        val bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
        val additionalReference = BpnReferenceDto(referenceValue = "additionalRef", referenceType = BpnRequestIdentifier)
        val sameIdentifier = addressIdentifierDto(bpnAReference.referenceValue, 1L, TypeKeyNameVerboseDto(addressIdentifierTypeDto1.technicalKey, ""))
        val createSiteRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
            ),
            site = minValidSite(
                bpnSReference = BpnReferenceDto(referenceValue = "siteRefValue", referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = "mainAddressRefValue", referenceType = BpnRequestIdentifier)
            ).copy(
                mainAddress = minLogisticAddress(bpnAReference).copy(
                    identifiers = listOf(
                        sameIdentifier,
                    )
                )
            ),
            address = minLogisticAddress(additionalReference).copy(
                identifiers = listOf(
                    sameIdentifier,
                )
            ),
        )

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)

        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors).hasSize(1)
        assertThat(createResult[0].errors[0].description).isEqualTo("Duplicate Address Identifier: Value 'value_222_1' of type 'ADDR_KEY_ONE'")
    }

    @Test
    fun `update Site with minimal fields`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val siteRefValue = "siteRefValue"
        val mainAddressRefValue = "mainAddressRefValue"
        val createSiteRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            ),
            site = minValidSite(
                bpnSReference = BpnReferenceDto(referenceValue = siteRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = mainAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)
        val createdSite = poolClient.sites.getSite(createResult[0].businessPartner?.site?.bpnSReference?.referenceValue!!)
        assertThat(createdSite.site.name).isEqualTo(createSiteRequest.site?.name)

        val updateSiteRequest = createResult[0].businessPartner?.copy(
            site = createResult[0].businessPartner?.site?.copy(name = "ChangedName", hasChanged = true)
        )
        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = updateSiteRequest!!)
        val updatedSite = poolClient.sites.getSite(updateResult[0].businessPartner?.site?.bpnSReference?.referenceValue!!)
        compareSite(updatedSite, updateResult[0].businessPartner?.site)
    }

    @Test
    fun `update Site with invalid bpnS`() {

        val siteRefValue = "siteRefValue"
        val mainAddressRefValue = "mainAddressRefValue"
        val createSiteRquest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
            ),
            site = minValidSite(
                bpnSReference = BpnReferenceDto(referenceValue = siteRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = mainAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRquest)
        val createdSite = poolClient.sites.getSite(createResult[0].businessPartner?.site?.bpnSReference?.referenceValue!!)
        assertThat(createdSite.site.name).isEqualTo(createSiteRquest.site?.name)

        val updateSiteRequest = createSiteRquest.copy(
            site = createSiteRquest.site?.copy(
                name = "ChangedName",
                hasChanged = true,
                bpnSReference = BpnReferenceDto(referenceValue = "InvalidBPN", referenceType = Bpn),
            )
        )
        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = updateSiteRequest)
        assertThat(updateResult[0].errors).hasSize(1)
        assertThat(updateResult[0].errors[0].description).isEqualTo("Business Partner with BPN 'InvalidBPN' can't be updated as it doesn't exist")
    }

    @Test
    fun `update Site with invalid address administration level 1 and invalid identifier`() {

        val bpnASiteReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
        val additionalReference = BpnReferenceDto(referenceValue = "7777", referenceType = BpnRequestIdentifier)
        val siteRefValue = "siteRefValue"
        val createSiteReuqest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = "123", referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = "222", referenceType = BpnRequestIdentifier)
            ),
            site = minValidSite(
                bpnSReference = BpnReferenceDto(referenceValue = siteRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = bpnASiteReference
            )
        )

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteReuqest)
        val createdSite = poolClient.sites.getSite(createResult[0].businessPartner?.site?.bpnSReference?.referenceValue!!)
        assertThat(createdSite.site.name).isEqualTo(createSiteReuqest.site?.name)

        val updateSiteRequest = createResult[0].businessPartner?.copy(
            site = createResult[0].businessPartner?.site?.copy(
                name = "Changed Site",
                hasChanged = true,
                mainAddress = createResult[0].businessPartner?.site?.mainAddress?.copy(
                    physicalPostalAddress = minPhysicalPostalAddressDto(bpnASiteReference).copy(
                        administrativeAreaLevel1 = "Invalid"
                    ),
                    identifiers = listOf(
                        addressIdentifierDto(bpnASiteReference.referenceValue, 1L, TypeKeyNameVerboseDto("InvalidKey1", "InvalidName1")),
                    )
                )
            ),
            address = minLogisticAddress(additionalReference).copy(
                physicalPostalAddress = minPhysicalPostalAddressDto(additionalReference).copy(
                    administrativeAreaLevel1 = "InvalidAdditional"
                ),
                identifiers = listOf(
                    addressIdentifierDto(additionalReference.referenceValue, 2L, TypeKeyNameVerboseDto("InvalidKey2", "InvalidName2")),
                )
            ),
        )
        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateSiteRequest!!)
        assertThat(updateResult[0].taskId).isEqualTo("TASK_2")
        val errorDescriptions = updateResult[0].errors.map { it.description }
        assertThat(errorDescriptions).containsExactlyInAnyOrder(
            "Address administrative area level1 'Invalid' does not exist",
            "Address administrative area level1 'InvalidAdditional' does not exist",
            "Address Identifier Type 'InvalidKey1' does not exist",
            "Address Identifier Type 'InvalidKey2' does not exist"
        )
    }

    @Test
    fun `update Site with same address identifiers in main address and additional address`() {

        val mainAddressRefValue = BpnReferenceDto(referenceValue = "A222", referenceType = BpnRequestIdentifier)
        val additionalAddressReference = BpnReferenceDto(referenceValue = "A333", referenceType = BpnRequestIdentifier)
        val sameAddressIdentifier = addressIdentifierDto(additionalAddressReference.referenceValue, 2L, TypeKeyNameVerboseDto(addressIdentifierTypeDto2.technicalKey, ""))
        val createSiteRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = "LE123", referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = "A111LE", referenceType = BpnRequestIdentifier)
            ),
            site = minValidSite(
                bpnSReference = BpnReferenceDto(referenceValue = "siteRefValue", referenceType = BpnRequestIdentifier),
                bpnAReference = mainAddressRefValue
            ).copy(
                mainAddress = minLogisticAddress(mainAddressRefValue).copy(
                    identifiers = listOf(
                        addressIdentifierDto(mainAddressRefValue.referenceValue, 1L, TypeKeyNameVerboseDto(addressIdentifierTypeDto1.technicalKey, "")),
                    )
                )
            ),
            address = minLogisticAddress(
                bpnAReference = additionalAddressReference,
            ).copy(
                identifiers = listOf(
                    sameAddressIdentifier,
                )
            )
        )

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)
        val createdSite = poolClient.sites.getSite(createResult[0].businessPartner?.site?.bpnSReference?.referenceValue!!)
        assertThat(createdSite.site.name).isEqualTo(createSiteRequest.site?.name)

        val updateCopy = createResult[0].businessPartner?.copy(
            site = createResult[0].businessPartner?.site?.copy(
                name = "ChangedName",
                hasChanged = true,
                mainAddress = createResult[0].businessPartner?.site?.mainAddress?.copy(
                    identifiers = listOf(
                        addressIdentifierDto(mainAddressRefValue.referenceValue, 1L, TypeKeyNameVerboseDto(addressIdentifierTypeDto1.technicalKey, "")),
                        sameAddressIdentifier,
                    )
                )
            ),

            )
        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateCopy!!)
        assertThat(updateResult[0].errors).hasSize(1)
        assertThat(updateResult[0].errors[0].description).isEqualTo("Duplicate Address Identifier: Value 'value_A333_2' of type 'ADDR_KEY_TWO'")
    }

    @Test
    fun `update Site with same reference value`() {

        val mainAddressRefValue = BpnReferenceDto(referenceValue = "A222", referenceType = BpnRequestIdentifier)
        val additionalAddressReference = BpnReferenceDto(referenceValue = "A333", referenceType = BpnRequestIdentifier)
        val createSiteRequest = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = "LE123", referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = "A111LE", referenceType = BpnRequestIdentifier)
            ),
            site = minValidSite(
                bpnSReference = BpnReferenceDto(referenceValue = "siteRefValue", referenceType = BpnRequestIdentifier),
                bpnAReference = mainAddressRefValue
            ).copy(
                mainAddress = minLogisticAddress(mainAddressRefValue).copy(
                    identifiers = listOf(
                        addressIdentifierDto(mainAddressRefValue.referenceValue, 1L, TypeKeyNameVerboseDto(addressIdentifierTypeDto1.technicalKey, "")),
                    )
                )
            ),
            address = minLogisticAddress(
                bpnAReference = additionalAddressReference,
            ).copy(
                identifiers = listOf(
                    addressIdentifierDto(mainAddressRefValue.referenceValue, 2L, TypeKeyNameVerboseDto(addressIdentifierTypeDto2.technicalKey, "")),
                )
            )
        )

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)
        val createdSite = poolClient.sites.getSite(createResult[0].businessPartner?.site?.bpnSReference?.referenceValue!!)
        assertThat(createdSite.site.name).isEqualTo(createSiteRequest.site?.name)

        // use create request for update to have same identifier
        val updateCopy = createSiteRequest.copy(
            site = createSiteRequest.site?.copy(
                name = "ChangedName",
                hasChanged = true,
            ),
        )
        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateCopy)
        assertThat(updateResult[0].errors).hasSize(0)
    }

    @Test
    fun `create address with all fields`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val additionalAddressRefValue = "77"
        val fullBpWithAddress = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(
                bpnLReference = BpnReferenceDto(referenceValue = leRefValue, referenceType = BpnRequestIdentifier),
                bpnAReference = BpnReferenceDto(referenceValue = leAddressRefValue, referenceType = BpnRequestIdentifier)
            ),
            address = fullLogisticAddressDto(
                bpnAReference = BpnReferenceDto(referenceValue = additionalAddressRefValue, referenceType = BpnRequestIdentifier)
            )
        )

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = fullBpWithAddress)
        val createdLeAddress = poolClient.addresses.getAddress(createResult[0].businessPartner?.legalEntity?.legalAddress?.bpnAReference?.referenceValue!!)
        val createdAdditionalAddress = poolClient.addresses.getAddress(createResult[0].businessPartner?.address?.bpnAReference?.referenceValue!!)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors).hasSize(0)
        assertThat(createdLeAddress.isLegalAddress).isTrue()
        assertThat(createdAdditionalAddress.isMainAddress).isFalse()
        assertThat(createdAdditionalAddress.isLegalAddress).isFalse()
        compareLogisticAddress(createdAdditionalAddress, createResult[0].businessPartner?.address)
    }

    @Test
    fun `create multiple legal entity `() {

        val numberOfEntitiesToTest = 100
        val referenceIds = (1..numberOfEntitiesToTest).toList()
        val fullBpWithLegalEntity = referenceIds.map {
            minFullBusinessPartner().copy(
                legalEntity = fullValidLegalEntity(
                    bpnLReference = BpnReferenceDto(referenceValue = "" + it, referenceType = BpnRequestIdentifier),
                    bpnAReference = BpnReferenceDto(referenceValue = "address" + it, referenceType = BpnRequestIdentifier)
                )
            )
        }

        val taskSteps = multipleTaskStep(fullBpWithLegalEntity)
        val createResults = cleaningStepService.upsertGoldenRecordIntoPool(taskSteps)
        assertThat(createResults).hasSize(numberOfEntitiesToTest)
        assertThat(createResults.filter { it.errors.isNotEmpty() }).hasSize(0)

        val updateResults = cleaningStepService.upsertGoldenRecordIntoPool(taskSteps)
        assertThat(updateResults).hasSize(numberOfEntitiesToTest)
        assertThat(updateResults.filter { it.errors.isNotEmpty() }).hasSize(0)

        val referenceIds2 = ((numberOfEntitiesToTest + 1)..(2 * numberOfEntitiesToTest)).toList()
        val fullBpWithLegalEntity2 = referenceIds2.map {
            minFullBusinessPartner().copy(
                legalEntity = fullValidLegalEntity(
                    bpnLReference = BpnReferenceDto(referenceValue = "" + it, referenceType = BpnRequestIdentifier),
                    bpnAReference = BpnReferenceDto(referenceValue = "address" + it, referenceType = BpnRequestIdentifier)
                )
            )
        }
        val taskSteps2 = multipleTaskStep(fullBpWithLegalEntity2)
        val createResults2 = cleaningStepService.upsertGoldenRecordIntoPool(taskSteps2)
        assertThat(createResults2).hasSize(numberOfEntitiesToTest)
        assertThat(createResults2.filter { it.errors.isNotEmpty() }).hasSize(0)
    }

    @Test
    fun `update legal entity and create legal entity in one request with duplicated identifier`() {

        val leCreateRef1 = BpnReferenceDto(referenceValue = "LE111", referenceType = BpnRequestIdentifier)
        val leCreateAddressRef1 = BpnReferenceDto(referenceValue = "LE111A22", referenceType = BpnRequestIdentifier)
        val createLegalEntity1 = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(bpnLReference = leCreateRef1, bpnAReference = leCreateAddressRef1).copy(
                identifiers = listOf(
                    legalEntityIdentifierDto(leCreateRef1.referenceValue, 1L, BusinessPartnerVerboseValues.identifierType1),
                    legalEntityIdentifierDto(leCreateRef1.referenceValue, 2L, BusinessPartnerVerboseValues.identifierType2)
                )
            )
        )
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntity1)
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val legalEntityUpdateIdentifier = legalEntityIdentifierDto(leCreateRef1.referenceValue, 3L, BusinessPartnerVerboseValues.identifierType3)
        val updatedFullBpLegalEntity = createResult[0].businessPartner?.copy(
            legalEntity = createResult[0].businessPartner?.legalEntity?.copy(
                identifiers = listOf(
                    legalEntityUpdateIdentifier, // use identifier in update
                    legalEntityIdentifierDto(leCreateRef1.referenceValue, 2L, BusinessPartnerVerboseValues.identifierType2)
                ),
                hasChanged = true,
                legalName = "Changed Legal Entity"
            )
        )
        val leCreateRef2 = BpnReferenceDto(referenceValue = "LE222", referenceType = BpnRequestIdentifier)
        val leCreateAddressRef2 = BpnReferenceDto(referenceValue = "LE222A333", referenceType = BpnRequestIdentifier)
        val createLegalEntity2 = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(bpnLReference = leCreateRef2, bpnAReference = leCreateAddressRef2).copy(
                identifiers = listOf(
                    legalEntityUpdateIdentifier, // use same identifier in create
                    legalEntityIdentifierDto(leCreateRef2.referenceValue, 1L, BusinessPartnerVerboseValues.identifierType1)
                )
            )
        )
        val updateTask = TaskStepReservationEntryDto(taskId = "TASK_2", businessPartner = updatedFullBpLegalEntity!!)
        val createTask2 = TaskStepReservationEntryDto(taskId = "TASK_3", businessPartner = createLegalEntity2)
        val createUpdateResult = cleaningStepService.upsertGoldenRecordIntoPool(listOf(updateTask, createTask2))

        assertThat(createUpdateResult[0].taskId).isEqualTo("TASK_2")
        assertThat(createUpdateResult[1].taskId).isEqualTo("TASK_3")
        assertThat(createUpdateResult[0].errors.size).isEqualTo(1)
        assertThat(createUpdateResult[1].errors.size).isEqualTo(1)
        assertThat(createUpdateResult[0].errors[0].description).isEqualTo("Duplicate Legal Entity Identifier: Value 'value_LE111_3' of type 'VAT_FR'")
    }

    @Test
    fun `create legal entity and create site in one request with duplicated identifier`() {

        val leCreateRef1 = BpnReferenceDto(referenceValue = "LE111", referenceType = BpnRequestIdentifier)
        val leCreateAddressRef1 = BpnReferenceDto(referenceValue = "LE111A11", referenceType = BpnRequestIdentifier)
        val additionalAddressLeRef1 = BpnReferenceDto(referenceValue = "LE111A33", referenceType = BpnRequestIdentifier)
        val duplicatIdentifier = addressIdentifierDto(leCreateAddressRef1.referenceValue, 1L, TypeKeyNameVerboseDto(addressIdentifierTypeDto1.technicalKey, ""))
        val createLegalEntity1 = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(bpnLReference = leCreateRef1, bpnAReference = leCreateAddressRef1).copy(
                legalAddress = minLogisticAddress(leCreateAddressRef1).copy(
                    identifiers = listOf(
                        duplicatIdentifier,
                    )
                )
            ),
            address = minLogisticAddress(additionalAddressLeRef1).copy(
                identifiers = listOf(
                    addressIdentifierDto(additionalAddressLeRef1.referenceValue, 2L, TypeKeyNameVerboseDto(addressIdentifierTypeDto1.technicalKey, "")),
                )
            )
        )

        val leCreateRef2 = BpnReferenceDto(referenceValue = "LE222", referenceType = BpnRequestIdentifier)
        val leCreateAddressRef2 = BpnReferenceDto(referenceValue = "LE222A22", referenceType = BpnRequestIdentifier)
        val createSiteRef2 = BpnReferenceDto(referenceValue = "SE333", referenceType = BpnRequestIdentifier)
        val mainAddressRef2 = BpnReferenceDto(referenceValue = "SE333A66", referenceType = BpnRequestIdentifier)
        val additionalAddressSiteRef2 = BpnReferenceDto(referenceValue = "SE333A88", referenceType = BpnRequestIdentifier)
        val createSite1 = minFullBusinessPartner().copy(
            legalEntity = minValidLegalEntity(bpnLReference = leCreateRef2, bpnAReference = leCreateAddressRef2).copy(
                legalAddress = minLogisticAddress(leCreateAddressRef2).copy(
                    identifiers = listOf(
                        addressIdentifierDto(leCreateAddressRef2.referenceValue, 3L, TypeKeyNameVerboseDto(addressIdentifierTypeDto1.technicalKey, "")),
                    )
                )
            ),
            site = minValidSite(bpnSReference = createSiteRef2, bpnAReference = mainAddressRef2).copy(
                mainAddress = minLogisticAddress(mainAddressRef2).copy(
                    identifiers = listOf(
                        addressIdentifierDto(mainAddressRef2.referenceValue, 4L, TypeKeyNameVerboseDto(addressIdentifierTypeDto1.technicalKey, "")),
                    )
                )
            ),
            address = minLogisticAddress(additionalAddressSiteRef2).copy(
                identifiers = listOf(
                    duplicatIdentifier,
                )
            )
        )

        val createLegalEntityTask = TaskStepReservationEntryDto(taskId = "TASK_1", businessPartner = createLegalEntity1)
        val createSiteTask = TaskStepReservationEntryDto(taskId = "TASK_2", businessPartner = createSite1)
        val createResult = cleaningStepService.upsertGoldenRecordIntoPool(listOf(createLegalEntityTask, createSiteTask))
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[1].taskId).isEqualTo("TASK_2")
        assertThat(createResult[0].errors.size).isEqualTo(1)
        assertThat(createResult[1].errors.size).isEqualTo(1)
        assertThat(createResult[0].errors[0].description).isEqualTo("Duplicate Address Identifier: Value 'value_LE111A11_1' of type 'ADDR_KEY_ONE'")
    }

    fun upsertGoldenRecordIntoPool(taskId: String, businessPartner: BusinessPartnerFullDto): List<TaskStepResultEntryDto> {

        val taskStep = singleTaskStep(taskId = taskId, businessPartner = businessPartner)
        return cleaningStepService.upsertGoldenRecordIntoPool(taskStep)
    }

    fun singleTaskStep(taskId: String, businessPartner: BusinessPartnerFullDto): List<TaskStepReservationEntryDto> {

        return listOf(
            TaskStepReservationEntryDto(
                taskId = taskId,
                businessPartner = businessPartner
            )
        )
    }

    fun multipleTaskStep(businessPartners: List<BusinessPartnerFullDto>): List<TaskStepReservationEntryDto> {

        return businessPartners.map {
            TaskStepReservationEntryDto(
                taskId = it.legalEntity?.bpnLReference?.referenceValue!!,
                businessPartner = it
            )
        }

    }


    fun minFullBusinessPartner(): BusinessPartnerFullDto {

        return BusinessPartnerFullDto(generic = BusinessPartnerGenericDto())
    }

    fun emptyLegalEntity(): LegalEntityDto {

        return LegalEntityDto()
    }

    fun minValidLegalEntity(bpnLReference: BpnReferenceDto, bpnAReference: BpnReferenceDto): LegalEntityDto {

        return LegalEntityDto(
            bpnLReference = bpnLReference,
            legalName = "legalName_" + bpnLReference.referenceValue,
            legalAddress = minLogisticAddress(bpnAReference = bpnAReference),
            confidenceCriteria = fullConfidenceCriteria()
        )
    }

    fun fullValidLegalEntity(bpnLReference: BpnReferenceDto, bpnAReference: BpnReferenceDto): LegalEntityDto {

        return LegalEntityDto(
            bpnLReference = bpnLReference,
            legalName = "legalName_" + bpnLReference.referenceValue,
            legalShortName = "shortName_" + bpnLReference.referenceValue,
            legalForm = BusinessPartnerVerboseValues.legalForm1.technicalKey,
            identifiers = listOf(
                legalEntityIdentifierDto(bpnLReference.referenceValue, 1L, BusinessPartnerVerboseValues.identifierType1),
                legalEntityIdentifierDto(bpnLReference.referenceValue, 2L, BusinessPartnerVerboseValues.identifierType2)
            ),
            states = listOf(
                legalEntityState(bpnLReference.referenceValue, 1L, BusinessStateType.ACTIVE),
                legalEntityState(bpnLReference.referenceValue, 2L, BusinessStateType.INACTIVE)
            ),
            legalAddress = fullLogisticAddressDto(bpnAReference),
            confidenceCriteria = fullConfidenceCriteria()
        )
    }

    fun legalEntityIdentifierDto(name: String, id: Long, type: TypeKeyNameVerboseDto<String>): LegalEntityIdentifierDto {

        return LegalEntityIdentifierDto(
            value = "value_" + name + "_" + id,
            issuingBody = "issuingBody_" + name + "_" + id,
            type = type.technicalKey
        )
    }

    fun addressIdentifierDto(name: String, id: Long, type: TypeKeyNameVerboseDto<String>): AddressIdentifierDto {

        return AddressIdentifierDto(
            value = "value_" + name + "_" + id,
            type = type.technicalKey
        )
    }

    fun legalEntityState(name: String, id: Long, type: BusinessStateType): LegalEntityStateDto {

        return LegalEntityStateDto(
            validFrom = LocalDateTime.now().plusDays(id),
            validTo = LocalDateTime.now().plusDays(id + 2),
            type = type
        )
    }

    fun siteState(name: String, id: Long, type: BusinessStateType): SiteStateDto {

        return SiteStateDto(
            validFrom = LocalDateTime.now().plusDays(id),
            validTo = LocalDateTime.now().plusDays(id + 2),
            type = type
        )
    }

    fun addressState(name: String, id: Long, type: BusinessStateType): AddressStateDto {

        return AddressStateDto(
            validFrom = LocalDateTime.now().plusDays(id),
            validTo = LocalDateTime.now().plusDays(id + 2),
            type = type
        )
    }


    fun classificationDto(name: String, id: Long, type: ClassificationType): LegalEntityClassificationDto {

        return LegalEntityClassificationDto(
            code = "code_" + name + "_" + id,
            value = "value_" + name + "_" + id,
            type = type
        )
    }

    fun minLogisticAddress(bpnAReference: BpnReferenceDto): LogisticAddressDto {

        return LogisticAddressDto(
            bpnAReference = bpnAReference,
            physicalPostalAddress = minPhysicalPostalAddressDto(bpnAReference),
            confidenceCriteria = fullConfidenceCriteria()
        )
    }

    private fun minPhysicalPostalAddressDto(bpnAReference: BpnReferenceDto) = PhysicalPostalAddressDto(
        country = CountryCode.DE,
        city = "City_" + bpnAReference.referenceValue
    )

    fun fullLogisticAddressDto(bpnAReference: BpnReferenceDto): LogisticAddressDto {

        return LogisticAddressDto(
            bpnAReference = bpnAReference,
            name = "name_" + bpnAReference.referenceValue,
            identifiers = listOf(
                addressIdentifierDto(bpnAReference.referenceValue, 1L, TypeKeyNameVerboseDto(addressIdentifierTypeDto1.technicalKey, "")),
                addressIdentifierDto(bpnAReference.referenceValue, 2L, TypeKeyNameVerboseDto(addressIdentifierTypeDto2.technicalKey, ""))
            ),
            states = listOf(
                addressState(bpnAReference.referenceValue, 1L, BusinessStateType.ACTIVE),
                addressState(bpnAReference.referenceValue, 2L, BusinessStateType.INACTIVE)
            ),
            physicalPostalAddress = PhysicalPostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(longitude = 1.1f, latitude = 2.2f, altitude = 3.3f),
                country = CountryCode.DE,
                administrativeAreaLevel1 = "AD-07",
                administrativeAreaLevel2 = "adminArea2_" + bpnAReference.referenceValue,
                administrativeAreaLevel3 = "adminArea3_" + bpnAReference.referenceValue,
                postalCode = "postalCode_" + bpnAReference.referenceValue,
                city = "city_" + bpnAReference.referenceValue,
                street = StreetDto(
                    name = "name_" + bpnAReference.referenceValue,
                    houseNumber = "houseNumber_" + bpnAReference.referenceValue,
                    houseNumberSupplement = "houseNumberSupplement_" + bpnAReference.referenceValue,
                    milestone = "milestone_" + bpnAReference.referenceValue,
                    direction = "direction_" + bpnAReference.referenceValue,
                    namePrefix = "namePrefix_" + bpnAReference.referenceValue,
                    additionalNamePrefix = "additionalNamePrefix_" + bpnAReference.referenceValue,
                    nameSuffix = "nameSuffix_" + bpnAReference.referenceValue,
                    additionalNameSuffix = "additionalNameSuffix_" + bpnAReference.referenceValue,
                ),
                district = "district_" + bpnAReference.referenceValue,
                companyPostalCode = "companyPostalCode_" + bpnAReference.referenceValue,
                industrialZone = "industrialZone_" + bpnAReference.referenceValue,
                building = "building_" + bpnAReference.referenceValue,
                floor = "floor_" + bpnAReference.referenceValue,
                door = "door_" + bpnAReference.referenceValue,
            ),
            alternativePostalAddress = AlternativePostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(longitude = 12.3f, latitude = 4.56f, altitude = 7.89f),
                country = CountryCode.DE,
                administrativeAreaLevel1 = "DE-BW",
                postalCode = "alternate_postalCode_" + bpnAReference.referenceValue,
                city = "alternate_city_" + bpnAReference.referenceValue,
                deliveryServiceType = DeliveryServiceType.PO_BOX,
                deliveryServiceQualifier = "deliveryServiceQualifier_" + bpnAReference.referenceValue,
                deliveryServiceNumber = "deliveryServiceNumber_" + bpnAReference.referenceValue,
            ),
            confidenceCriteria = fullConfidenceCriteria()
        )
    }

    fun minValidSite(bpnSReference: BpnReferenceDto, bpnAReference: BpnReferenceDto): SiteDto {

        return SiteDto(
            bpnSReference = bpnSReference,
            name = "siteName_" + bpnSReference.referenceValue,
            mainAddress = minLogisticAddress(bpnAReference = bpnAReference),
            confidenceCriteria = fullConfidenceCriteria()
        )
    }

    fun fullValidSite(bpnSReference: BpnReferenceDto, bpnAReference: BpnReferenceDto): SiteDto {

        return SiteDto(
            bpnSReference = bpnSReference,
            name = "siteName_" + bpnSReference.referenceValue,
            states = listOf(
                siteState(bpnSReference.referenceValue, 1L, BusinessStateType.ACTIVE), siteState(bpnSReference.referenceValue, 2L, BusinessStateType.INACTIVE)
            ),
            mainAddress = fullLogisticAddressDto(bpnAReference),
            confidenceCriteria = fullConfidenceCriteria()
        )
    }

    fun fullConfidenceCriteria() =
        ConfidenceCriteriaDto(
            sharedByOwner = true,
            numberOfBusinessPartners = 1,
            checkedByExternalDataSource = true,
            lastConfidenceCheckAt = LocalDateTime.now(),
            nextConfidenceCheckAt = LocalDateTime.now().plusDays(1),
            confidenceLevel = 10
        )

    fun assertTaskError(step: TaskStepResultEntryDto, taskId: String, error: CleaningError) {

        assertThat(step.taskId).isEqualTo(taskId)
        assertThat(step.errors.size).isEqualTo(1)
        assertThat(step.errors[0].description).isEqualTo(error.message)

    }

    fun compareLegalEntity(verboseRequest: LegalEntityWithLegalAddressVerboseDto, legalEntity: LegalEntityDto?) {

        val verboseLegalEntity = verboseRequest.legalEntity

        assertThat(verboseLegalEntity.legalShortName).isEqualTo(legalEntity?.legalShortName)
        assertThat(verboseLegalEntity.legalFormVerbose?.technicalKey).isEqualTo(legalEntity?.legalForm)
        compareStates(verboseLegalEntity.states, legalEntity?.states)
        compareIdentifiers(verboseLegalEntity.identifiers, legalEntity?.identifiers)

        val verboseLegalAddress = verboseRequest.legalAddress
        assertThat(verboseLegalAddress.bpnLegalEntity).isEqualTo(legalEntity?.bpnLReference?.referenceValue)
        assertThat(verboseLegalAddress.isLegalAddress).isTrue()
        compareLogisticAddress(verboseLegalAddress, legalEntity?.legalAddress)
    }

    fun compareSite(verboseRequest: SiteWithMainAddressVerboseDto, site: SiteDto?) {

        val verboseSite = verboseRequest.site

        assertThat(verboseSite.name).isEqualTo(site?.name)
        assertThat(verboseSite.bpns).isEqualTo(site?.bpnSReference?.referenceValue)
        compareSiteStates(verboseSite.states, site?.states)

        val verboseMainAddress = verboseRequest.mainAddress
        assertThat(verboseMainAddress.bpnSite).isEqualTo(site?.bpnSReference?.referenceValue)
        val mainAddress = site?.mainAddress
        assertThat(verboseMainAddress.isMainAddress).isTrue()
        compareLogisticAddress(verboseMainAddress, mainAddress)
    }

    private fun compareLogisticAddress(verboseAddress: LogisticAddressVerboseDto, address: LogisticAddressDto?) {

        assertThat(verboseAddress.name).isEqualTo(address?.name)
        compareAddressStates(verboseAddress.states, address?.states)
        compareAddressIdentifiers(verboseAddress.identifiers, address?.identifiers)


        val verbosePhysicalAddress = verboseAddress.physicalPostalAddress
        val physicalAddress = address?.physicalPostalAddress
        assertThat(verbosePhysicalAddress).usingRecursiveComparison()
            .ignoringFields(PhysicalPostalAddressVerboseDto::countryVerbose.name, PhysicalPostalAddressVerboseDto::administrativeAreaLevel1Verbose.name)
            .isEqualTo(physicalAddress)
        assertThat(verbosePhysicalAddress.country.name).isEqualTo(physicalAddress?.country?.name)
        assertThat(verbosePhysicalAddress.administrativeAreaLevel1).isEqualTo(physicalAddress?.administrativeAreaLevel1)
        val verboseAlternAddress = verboseAddress.alternativePostalAddress
        val alternAddress = address?.alternativePostalAddress
        assertThat(verboseAlternAddress).usingRecursiveComparison()
            .ignoringFields(AlternativePostalAddressVerboseDto::countryVerbose.name, AlternativePostalAddressVerboseDto::administrativeAreaLevel1Verbose.name)
            .isEqualTo(alternAddress)
        assertThat(verboseAlternAddress?.country?.name).isEqualTo(alternAddress?.country?.name)
        assertThat(verboseAlternAddress?.administrativeAreaLevel1).isEqualTo(alternAddress?.administrativeAreaLevel1)
    }

    fun compareAddressStates(statesVerbose: Collection<AddressStateVerboseDto>, states: Collection<AddressStateDto>?) {

        assertThat(statesVerbose.size).isEqualTo(states?.size ?: 0)
        val sortedVerboseStates = statesVerbose.sortedBy { it.validFrom }
        val sortedStates = states?.sortedBy { it.validFrom }
        sortedVerboseStates.indices.forEach {
            assertThat(sortedVerboseStates[it].typeVerbose.technicalKey.name).isEqualTo(sortedStates!![it].type.name)
            assertThat(sortedVerboseStates[it]).usingRecursiveComparison()
                .withEqualsForFields(isEqualToIgnoringMilliseconds(), AddressStateVerboseDto::validTo.name)
                .withEqualsForFields(isEqualToIgnoringMilliseconds(), AddressStateVerboseDto::validFrom.name)
                .ignoringFields(AddressStateVerboseDto::typeVerbose.name).isEqualTo(sortedStates[it])
        }
    }

    fun compareAddressIdentifiers(identifiersVerbose: Collection<AddressIdentifierVerboseDto>, identifiers: Collection<AddressIdentifierDto>?) {

        assertThat(identifiersVerbose.size).isEqualTo(identifiers?.size ?: 0)
        val sortedVerboseIdentifiers = identifiersVerbose.sortedBy { it.typeVerbose.name }
        val sortedIdentifiers = identifiers!!.sortedBy { it.type }
        sortedVerboseIdentifiers.indices.forEach {
            assertThat(sortedVerboseIdentifiers[it].typeVerbose.technicalKey).isEqualTo(sortedIdentifiers[it].type)
            assertThat(sortedVerboseIdentifiers[it]).usingRecursiveComparison()
                .ignoringFields(AddressIdentifierVerboseDto::typeVerbose.name)
                .isEqualTo(sortedIdentifiers[it])
        }
    }

    fun compareStates(statesVerbose: Collection<LegalEntityStateVerboseDto>, states: Collection<ILegalEntityStateDto>?) {

        assertThat(statesVerbose.size).isEqualTo(states?.size ?: 0)
        val sortedVerboseStates = statesVerbose.sortedBy { it.validFrom }
        val sortedStates = states!!.sortedBy { it.validFrom }
        sortedVerboseStates.indices.forEach {
            assertThat(sortedVerboseStates[it].typeVerbose.technicalKey.name).isEqualTo(sortedStates[it].type.name)
            assertThat(sortedVerboseStates[it]).usingRecursiveComparison()
                .withEqualsForFields(isEqualToIgnoringMilliseconds(), LegalEntityStateVerboseDto::validTo.name )
                .withEqualsForFields(isEqualToIgnoringMilliseconds(), LegalEntityStateVerboseDto::validFrom.name)
                .ignoringFields(LegalEntityStateVerboseDto::typeVerbose.name)
                .isEqualTo(sortedStates[it])
        }
    }

    fun compareSiteStates(statesVerbose: Collection<SiteStateVerboseDto>, states: Collection<ISiteStateDto>?) {

        assertThat(statesVerbose.size).isEqualTo(states?.size ?: 0)
        val sortedVerboseStates = statesVerbose.sortedBy { it.validFrom }
        val sortedStates = states!!.sortedBy { it.validFrom }
        sortedVerboseStates.indices.forEach {
            assertThat(sortedVerboseStates[it].typeVerbose.technicalKey.name).isEqualTo(sortedStates[it].type.name)
            assertThat(sortedVerboseStates[it]).usingRecursiveComparison()
                .withEqualsForFields(isEqualToIgnoringMilliseconds(), SiteStateVerboseDto::validTo.name)
                .withEqualsForFields(isEqualToIgnoringMilliseconds(), SiteStateVerboseDto::validFrom.name)
                .ignoringFields(SiteStateVerboseDto::typeVerbose.name)
                .isEqualTo(sortedStates[it])
        }
    }

    fun isEqualToIgnoringMilliseconds(): BiPredicate<LocalDateTime?, LocalDateTime?> {
        return BiPredicate<LocalDateTime?, LocalDateTime?> { d1, d2 ->
            (d1 == null && d2 == null)
                    || d1.truncatedTo(ChronoUnit.SECONDS).equals(d2.truncatedTo(ChronoUnit.SECONDS))
        }
    }

    fun compareClassifications(
        classificationsVerbose: Collection<LegalEntityClassificationVerboseDto>,
        classifications: Collection<LegalEntityClassificationDto>?
    ) {

        assertThat(classificationsVerbose.size).isEqualTo(classifications?.size ?: 0)
        val sortedVerboseClassifications = classificationsVerbose.sortedBy { it.typeVerbose.name }
        val sortedClassifications = classifications!!.sortedBy { it.type.name }
        sortedVerboseClassifications.indices.forEach {
            assertThat(sortedVerboseClassifications[it].typeVerbose.technicalKey.name).isEqualTo(sortedClassifications[it].type.name)
            assertThat(sortedVerboseClassifications[it]).usingRecursiveComparison()
                .ignoringFields(LegalEntityClassificationVerboseDto::typeVerbose.name)
                .isEqualTo(sortedClassifications[it])
        }
    }

    fun compareIdentifiers(identifiersVerbose: Collection<LegalEntityIdentifierVerboseDto>, identifiers: Collection<LegalEntityIdentifierDto>?) {

        assertThat(identifiersVerbose.size).isEqualTo(identifiers?.size ?: 0)
        val sortedVerboseIdentifiers = identifiersVerbose.sortedBy { it.typeVerbose.name }
        val sortedIdentifiers = identifiers!!.sortedBy { it.type }
        sortedVerboseIdentifiers.indices.forEach {
            assertThat(sortedVerboseIdentifiers[it].typeVerbose.technicalKey).isEqualTo(sortedIdentifiers[it].type)
            assertThat(sortedVerboseIdentifiers[it]).usingRecursiveComparison()
                .ignoringFields(LegalEntityIdentifierVerboseDto::typeVerbose.name).isEqualTo(sortedIdentifiers[it])
        }
    }

}
