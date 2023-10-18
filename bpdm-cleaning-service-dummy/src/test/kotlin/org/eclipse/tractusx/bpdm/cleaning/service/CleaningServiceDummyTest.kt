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

import com.github.tomakehurst.wiremock.client.WireMock.*
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
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.Assertions.*
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
    fun `test processCleaningTask with BpnA present and additional address type`() {
        val taskStepReservationEntryDto = createSampleTaskStepReservationResponse(businessPartnerWithBpnA).reservedTasks[0]

        val result = cleaningServiceDummy.processCleaningTask(taskStepReservationEntryDto)

        val expectedBpnA = taskStepReservationEntryDto.businessPartner.generic.bpnA

        val resultedAddress = result.businessPartner?.address

        val resultedLegalEntity = result.businessPartner?.legalEntity

        // legalEntity should be Generated with new bpnL and legalAddress bpnA
        // addressPartner should use passed bpnA, and it will be different from legalAddress since type is additional address type

        assertEquals(expectedBpnA, resultedAddress?.bpnAReference?.referenceValue)

        assertEquals(BpnReferenceType.BpnRequestIdentifier, resultedLegalEntity?.bpnLReference?.referenceType)

        val expectedAddress = expectedLogisticAddressDto.copy(bpnAReference = BpnReferenceDto(expectedBpnA.toString(), BpnReferenceType.Bpn))

        val expectedLegalEntity = expectedLegalEntityDto.copy(legalAddress = expectedLogisticAddressDto)

        assertRecursively(resultedAddress).isEqualTo(expectedAddress)

        // ignoring bpnLReference and bpnAReference since they are generated
        assertRecursively(resultedLegalEntity).ignoringFields("bpnLReference", "legalAddress.bpnAReference").isEqualTo(expectedLegalEntity)


    }

    @Test
    fun `test processCleaningTask with empty BpnA and additional address type`() {
        val taskStepReservationEntryDto = createSampleTaskStepReservationResponse(businessPartnerWithEmptyBpnLAndAdditionalAddressType).reservedTasks[0]

        val result = cleaningServiceDummy.processCleaningTask(taskStepReservationEntryDto)

        val resultedAddress = result.businessPartner?.address

        val resultedLegalEntity = result.businessPartner?.legalEntity

        // legalEntity should be Generated with new bpnL and legalAddress bpnA
        // addressPartner should be Generated bpnA, and it will be different from legalAddress since type is additional address type

        assertEquals(BpnReferenceType.BpnRequestIdentifier, resultedAddress?.bpnAReference?.referenceType)

        assertEquals(BpnReferenceType.BpnRequestIdentifier, resultedLegalEntity?.bpnLReference?.referenceType)

        assertNotEquals(resultedAddress?.bpnAReference?.referenceValue, resultedLegalEntity?.legalAddress?.bpnAReference?.referenceValue)

        val expectedAddress = expectedLogisticAddressDto.copy()

        val expectedLegalEntity = expectedLegalEntityDto.copy(legalAddress = expectedLogisticAddressDto)

        // bpnAReference since they are generated
        assertRecursively(resultedAddress).ignoringFields("bpnAReference").isEqualTo(expectedAddress)

        // ignoring bpnLReference and bpnAReference since they are generated
        assertRecursively(resultedLegalEntity).ignoringFields("bpnAReference", "bpnLReference", "legalAddress.bpnAReference").isEqualTo(expectedLegalEntity)
    }

    @Test
    fun `test processCleaningTask with BpnL and BpnA present and legal address type`() {
        val taskStepReservationEntryDto = createSampleTaskStepReservationResponse(businessPartnerWithBpnLAndBpnAAndLegalAddressType).reservedTasks[0]

        val result = cleaningServiceDummy.processCleaningTask(taskStepReservationEntryDto)

        val expectedBpnA = taskStepReservationEntryDto.businessPartner.generic.bpnA

        val expectedBpnL = taskStepReservationEntryDto.businessPartner.generic.bpnL

        val resultedAddress = result.businessPartner?.address

        val resultedLegalEntity = result.businessPartner?.legalEntity

        // legalEntity should use passed bpnL and legalAddress should use passed bpnA since address type is LegalAddressType
        // addressPartner should use null, since it does not create when type is LegalAddressType

        assertNull(resultedAddress?.bpnAReference?.referenceValue)
        assertEquals(expectedBpnL, resultedLegalEntity?.bpnLReference?.referenceValue)
        assertEquals(expectedBpnA, resultedLegalEntity?.legalAddress?.bpnAReference?.referenceValue)


        val expectedLegalEntity = expectedLegalEntityDto.copy(
            legalAddress = expectedLogisticAddressDto.copy(
                bpnAReference = BpnReferenceDto(
                    expectedBpnA.toString(),
                    BpnReferenceType.Bpn
                )
            ), bpnLReference = BpnReferenceDto(expectedBpnL.toString(), BpnReferenceType.Bpn)
        )

        assertRecursively(resultedLegalEntity).isEqualTo(expectedLegalEntity)
    }

    @Test
    fun `test processCleaningTask with BpnS and BpnA present and legal and site main address type`() {
        val taskStepReservationResponse = createSampleTaskStepReservationResponse(businessPartnerWithBpnSAndBpnAAndLegalAndSiteMainAddressType).reservedTasks[0]

        val result = cleaningServiceDummy.processCleaningTask(taskStepReservationResponse)

        val resultedAddress = result.businessPartner?.address

        val resultedLegalEntity = result.businessPartner?.legalEntity

        val resultedSite = result.businessPartner?.site

        val expectedBpnA = taskStepReservationResponse.businessPartner.generic.bpnA

        val expectedBpnS = taskStepReservationResponse.businessPartner.generic.bpnS


        // legalEntity should Generate new bpnL and legalAddress should use passed bpnA since address type is LegalAndSiteMainAddress
        // addressPartner should use null, since it does not create when type is LegalAndSiteMainAddress
        // Site should use passed bpnS, and it will be the same MainAddress as legalAddress and addressPartner since address type is LegalAndSiteMainAddress


        assertNull(resultedAddress?.bpnAReference?.referenceValue)

        assertEquals(expectedBpnA, resultedLegalEntity?.legalAddress?.bpnAReference?.referenceValue)

        assertEquals(expectedBpnA, resultedSite?.mainAddress?.bpnAReference?.referenceValue)

        assertEquals(BpnReferenceType.BpnRequestIdentifier, resultedLegalEntity?.bpnLReference?.referenceType)

        assertEquals(expectedBpnS, resultedSite?.bpnSReference?.referenceValue)


        val expectedLegalEntity = expectedLegalEntityDto.copy(
            legalAddress = expectedLogisticAddressDto.copy(
                bpnAReference = BpnReferenceDto(
                    expectedBpnA.toString(),
                    BpnReferenceType.Bpn
                )
            )
        )

        val expectedSite = expectedSiteDto.copy(
            mainAddress = expectedLogisticAddressDto.copy(bpnAReference = BpnReferenceDto(expectedBpnA.toString(), BpnReferenceType.Bpn)),
            bpnSReference = BpnReferenceDto(expectedBpnS.toString(), BpnReferenceType.Bpn)
        )


        // ignoring bpnLReference since they are generated
        assertRecursively(resultedLegalEntity).ignoringFields("bpnLReference").isEqualTo(expectedLegalEntity)

        assertRecursively(resultedSite).isEqualTo(expectedSite)


    }
    @Test
    fun `test processCleaningTask with empty Bpn and site main address type`() {
        val taskStepReservationResponse = createSampleTaskStepReservationResponse(businessPartnerWithEmptyBpnAndSiteMainAddressType).reservedTasks[0]

        val result = cleaningServiceDummy.processCleaningTask(taskStepReservationResponse)

        val resultedAddress = result.businessPartner?.address

        val resultedLegalEntity = result.businessPartner?.legalEntity

        val resultedSite = result.businessPartner?.site


        // legalEntity should Generate new bpnL and legalAddress should Generate new bpnA since address type is SiteMainAddress
        // addressPartner should use null, since it does not create when type is SiteMainAddress
        // Site should Generate new bpnS


        assertEquals(BpnReferenceType.BpnRequestIdentifier, resultedLegalEntity?.bpnLReference?.referenceType)

        assertNull(resultedAddress?.bpnAReference?.referenceType)

        assertEquals(BpnReferenceType.BpnRequestIdentifier, resultedSite?.bpnSReference?.referenceType)

        assertNotEquals(resultedAddress?.bpnAReference?.referenceValue, resultedLegalEntity?.legalAddress?.bpnAReference?.referenceValue)
        assertNotEquals(resultedAddress?.bpnAReference?.referenceValue, resultedSite?.mainAddress?.bpnAReference?.referenceValue)

        val expectedLegalEntity = expectedLegalEntityDto.copy(legalAddress = expectedLogisticAddressDto.copy())

        val expectedSite = expectedSiteDto.copy(mainAddress = expectedLogisticAddressDto.copy())


        // ignoring bpnLReference and legalAddress.bpnAReference since they are generated
        assertRecursively(resultedLegalEntity).ignoringFields("bpnLReference", "legalAddress.bpnAReference").isEqualTo(expectedLegalEntity)

        // ignoring bpnSReference and mainAddress.bpnAReference since they are generated
        assertRecursively(resultedSite).ignoringFields("bpnSReference", "mainAddress.bpnAReference").isEqualTo(expectedSite)

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

}
