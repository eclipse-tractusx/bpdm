/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

import org.assertj.core.api.Assertions
import org.assertj.core.api.RecursiveComparisonAssert
import org.eclipse.tractusx.bpdm.cleaning.testdata.CommonValues.businessPartnerWithBpnA
import org.eclipse.tractusx.bpdm.cleaning.testdata.CommonValues.businessPartnerWithBpnLAndBpnAAndLegalAddressType
import org.eclipse.tractusx.bpdm.cleaning.testdata.CommonValues.businessPartnerWithBpnSAndBpnAAndLegalAndSiteMainAddressType
import org.eclipse.tractusx.bpdm.cleaning.testdata.CommonValues.businessPartnerWithEmptyBpnAndSiteMainAddressType
import org.eclipse.tractusx.bpdm.cleaning.testdata.CommonValues.businessPartnerWithEmptyBpnLAndAdditionalAddressType
import org.eclipse.tractusx.bpdm.cleaning.testdata.CommonValues.expectedLegalEntityDto
import org.eclipse.tractusx.bpdm.cleaning.testdata.CommonValues.expectedLogisticAddressDto
import org.eclipse.tractusx.bpdm.cleaning.testdata.CommonValues.expectedSiteDto
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CleaningServiceDummyTest @Autowired constructor(
    val cleaningServiceDummy: CleaningServiceDummy
) {


    @Test
    fun `test processCleaningTask with additional address type`() {
        val taskStepReservationEntryDto = createSampleTaskStepReservationResponse(businessPartnerWithBpnA).reservedTasks[0]

        val result = cleaningServiceDummy.processCleaningTask(taskStepReservationEntryDto)

        val resultedAddress = result.businessPartner?.address
        val resultedLegalEntity = result.businessPartner?.legalEntity

        val expectedAddress = expectedLogisticAddressDto
        val expectedLegalEntity = expectedLegalEntityDto.copy(hasChanged = false, legalAddress = expectedAddress)

        assertLegalEntitiesEqual(resultedLegalEntity, expectedLegalEntity)
        assertAddressesEqual(resultedAddress, expectedAddress)
    }

    @Test
    fun `test processCleaningTask with empty BpnA and additional address type`() {
        val taskStepReservationEntryDto = createSampleTaskStepReservationResponse(businessPartnerWithEmptyBpnLAndAdditionalAddressType).reservedTasks[0]

        val result = cleaningServiceDummy.processCleaningTask(taskStepReservationEntryDto)

        val resultedAddress = result.businessPartner?.address
        val resultedLegalEntity = result.businessPartner?.legalEntity


        val expectedAddress = expectedLogisticAddressDto.copy()
        val expectedLegalEntity = expectedLegalEntityDto.copy(legalAddress = expectedLogisticAddressDto, hasChanged = false)

        assertLegalEntitiesEqual(resultedLegalEntity, expectedLegalEntity)
        assertAddressesEqual(resultedAddress, expectedAddress)
    }

    @Test
    fun `test processCleaningTask with legal address type`() {
        val taskStepReservationEntryDto = createSampleTaskStepReservationResponse(businessPartnerWithBpnLAndBpnAAndLegalAddressType).reservedTasks[0]

        val result = cleaningServiceDummy.processCleaningTask(taskStepReservationEntryDto)

        val resultedLegalEntity = result.businessPartner?.legalEntity


        val expectedLegalEntity = expectedLegalEntityDto.copy(
            legalAddress = expectedLogisticAddressDto.copy(
                hasChanged = false,
            )
        )

        assertLegalEntitiesEqual(resultedLegalEntity, expectedLegalEntity)
    }

    @Test
    fun `test processCleaningTask with legal and site main address type given`() {
        val taskStepReservationResponse = createSampleTaskStepReservationResponse(businessPartnerWithBpnSAndBpnAAndLegalAndSiteMainAddressType).reservedTasks[0]

        val result = cleaningServiceDummy.processCleaningTask(taskStepReservationResponse)

        val resultedLegalEntity = result.businessPartner?.legalEntity

        val resultedSite = result.businessPartner?.site

        val expectedLegalEntity = expectedLegalEntityDto.copy(
            hasChanged = true,
            legalAddress = expectedLogisticAddressDto.copy(
                hasChanged = false
            )
        )

        val expectedSite = expectedSiteDto.copy(
            hasChanged = true,
            mainAddress = expectedLogisticAddressDto.copy(hasChanged = false)
        )

        assertLegalEntitiesEqual(resultedLegalEntity, expectedLegalEntity)
        assertSitesEqual(resultedSite, expectedSite)
    }
    @Test
    fun `test processCleaningTask with empty Bpn and site main address type`() {
        val taskStepReservationResponse = createSampleTaskStepReservationResponse(businessPartnerWithEmptyBpnAndSiteMainAddressType).reservedTasks[0]

        val result = cleaningServiceDummy.processCleaningTask(taskStepReservationResponse)

        val resultedLegalEntity = result.businessPartner?.legalEntity
        val resultedSite = result.businessPartner?.site

        val expectedLegalEntity = expectedLegalEntityDto.copy(legalAddress = expectedLogisticAddressDto.copy(hasChanged = false), hasChanged = false)
        val expectedSite = expectedSiteDto.copy(mainAddress = expectedLogisticAddressDto.copy(hasChanged = false), hasChanged = true)

        assertLegalEntitiesEqual(resultedLegalEntity, expectedLegalEntity)
        assertSitesEqual(resultedSite, expectedSite)
    }

    // Helper method to create a sample TaskStepReservationResponse
    private fun createSampleTaskStepReservationResponse(businessPartnerGenericDto: BusinessPartnerGenericDto): TaskStepReservationResponse {
        val fullDto = BusinessPartnerFullDto(businessPartnerGenericDto)
        return TaskStepReservationResponse(listOf(TaskStepReservationEntryDto(UUID.randomUUID().toString(), fullDto)), Instant.MIN)
    }

    fun <T> assertRecursively(actual: T): RecursiveComparisonAssert<*> {
        return Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
            .ignoringFieldsOfTypes(Instant::class.java)
    }

    private fun assertLegalEntitiesEqual(actual: LegalEntityDto?, expected: LegalEntityDto) =
        assertRecursively(actual)
            .ignoringFields(LegalEntityDto::bpnLReference.name)
            .ignoringFields("${LegalEntityDto::legalAddress.name}.${LogisticAddressDto::bpnAReference.name}")
            .isEqualTo(expected)

    private fun assertSitesEqual(actual: SiteDto?, expected: SiteDto) =
        assertRecursively(actual)
            .ignoringFields(SiteDto::bpnSReference.name)
            .ignoringFields("${SiteDto::mainAddress.name}.${LogisticAddressDto::bpnAReference.name}")
            .isEqualTo(expected)

    private fun assertAddressesEqual(actual: LogisticAddressDto?, expected: LogisticAddressDto) =
        assertRecursively(actual)
            .ignoringFields(LogisticAddressDto::bpnAReference.name)
            .isEqualTo(expected)
}
