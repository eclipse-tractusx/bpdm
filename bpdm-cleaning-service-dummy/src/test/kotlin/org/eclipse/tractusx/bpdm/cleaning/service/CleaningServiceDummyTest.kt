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

import org.eclipse.tractusx.bpdm.cleaning.testdata.CommonValues.businessPartnerWithBpnA
import org.eclipse.tractusx.bpdm.cleaning.testdata.CommonValues.businessPartnerWithBpnLAndBpnAAndLegalAddressType
import org.eclipse.tractusx.bpdm.cleaning.testdata.CommonValues.businessPartnerWithBpnSAndBpnAAndLegalAndSiteMainAddressType
import org.eclipse.tractusx.bpdm.cleaning.testdata.CommonValues.businessPartnerWithEmptyBpnA
import org.eclipse.tractusx.bpdm.cleaning.testdata.CommonValues.businessPartnerWithEmptyBpnAndSiteMainAddressType
import org.eclipse.tractusx.bpdm.cleaning.testdata.CommonValues.businessPartnerWithEmptyBpnLAndAdditionalAddressType
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CleaningServiceDummyTest @Autowired constructor(
    val cleaningServiceDummy: CleaningServiceDummy,
) {


    @Test
    fun `test processCleaningTask with BpnA present and additional address type`() {
        val taskStepReservationEntryDto = createSampleTaskStepReservationResponse(businessPartnerWithBpnA).reservedTasks[0]

        val result = cleaningServiceDummy.processCleaningTask(taskStepReservationEntryDto)

        val expectedBpnA = taskStepReservationEntryDto.businessPartner.generic.bpnA

        val resultedAddress = result.businessPartner?.address

        val resultedLegalEntity = result.businessPartner?.legalEntity

        assertEquals(expectedBpnA, resultedAddress?.bpnAReference?.referenceValue)

        assertEquals(BpnReferenceType.BpnRequestIdentifier, resultedLegalEntity?.bpnLReference?.referenceType)
    }

    @Test
    fun `test processCleaningTask with empty BpnA and additional address type`() {
        val taskStepReservationResponse = createSampleTaskStepReservationResponse(businessPartnerWithEmptyBpnA).reservedTasks[0]

        val result = cleaningServiceDummy.processCleaningTask(taskStepReservationResponse)

        val resultedAddress = result.businessPartner?.address

        val resultedLegalEntity = result.businessPartner?.legalEntity

        assertEquals(BpnReferenceType.BpnRequestIdentifier, resultedAddress?.bpnAReference?.referenceType)

        assertEquals(BpnReferenceType.BpnRequestIdentifier, resultedLegalEntity?.bpnLReference?.referenceType)
    }

    @Test
    fun `test processCleaningTask with BpnL and BpnA present and legal address type`() {
        val taskStepReservationResponse = createSampleTaskStepReservationResponse(businessPartnerWithBpnLAndBpnAAndLegalAddressType).reservedTasks[0]

        val result = cleaningServiceDummy.processCleaningTask(taskStepReservationResponse)

        val expectedBpnA = taskStepReservationResponse.businessPartner.generic.bpnA
    }

    @Test
    fun `test processCleaningTask with empty BpnL and additional address type`() {
        val taskStepReservationResponse = createSampleTaskStepReservationResponse(businessPartnerWithEmptyBpnLAndAdditionalAddressType).reservedTasks[0]

        val result = cleaningServiceDummy.processCleaningTask(taskStepReservationResponse)

        val expectedBpnA = taskStepReservationResponse.businessPartner.generic.bpnA


    }

    @Test
    fun `test processCleaningTask with BpnS and BpnA present and legal and site main address type`() {
        val taskStepReservationResponse = createSampleTaskStepReservationResponse(businessPartnerWithBpnSAndBpnAAndLegalAndSiteMainAddressType).reservedTasks[0]

        val result = cleaningServiceDummy.processCleaningTask(taskStepReservationResponse)

        val expectedBpnA = taskStepReservationResponse.businessPartner.generic.bpnA
    }

    @Test
    fun `test processCleaningTask with empty Bpn and site main address type`() {
        val taskStepReservationResponse = createSampleTaskStepReservationResponse(businessPartnerWithEmptyBpnAndSiteMainAddressType).reservedTasks[0]

        val result = cleaningServiceDummy.processCleaningTask(taskStepReservationResponse)

        val expectedBpnA = taskStepReservationResponse.businessPartner.generic.bpnA
    }

    // Helper method to create a sample TaskStepReservationResponse
    private fun createSampleTaskStepReservationResponse(businessPartnerGenericDto: BusinessPartnerGenericDto): TaskStepReservationResponse {
        val fullDto = BusinessPartnerFullDto(businessPartnerGenericDto)
        return TaskStepReservationResponse(listOf(TaskStepReservationEntryDto(UUID.randomUUID().toString(), fullDto)), Instant.MIN)
    }


}
