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

package org.eclipse.tractusx.bpdm.gate.v6.outputconsumer

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.junit.jupiter.api.Test

class SearchBusinessPartnerOutputV6IT: GateOutputConsumerV6Test(){

    /**
     * GIVEN business partner output for external-ID
     * WHEN output consumer searches for output with that external-ID
     * THEN output consumer sees received golden record result
     *
     */
    @Test
    fun `find business partner output by external id`(){
        //GIVEN
        val externalIdToFind = testName
        val externalIdOther1 = "$testName 1"
        val externalIdOther2 = "$testName 2"

        val createdInput = testDataClient.createBusinessPartnerInput(externalIdToFind)
        val otherCreatedInput1 = testDataClient.createBusinessPartnerInput(externalIdOther1)
        val otherCreatedInput2 = testDataClient.createBusinessPartnerInput(externalIdOther2)

        val connectedGoldenRecords = testDataClient.refineToAdditionalAddressOfSite(createdInput, externalIdToFind)
        testDataClient.refineToAdditionalAddressOfSite(otherCreatedInput1, externalIdOther1)
        testDataClient.refineToAdditionalAddressOfSite(otherCreatedInput2, externalIdOther2)

        //WHEN
        val response = gateClient.businessPartners.getBusinessPartnersOutput(listOf(externalIdToFind))

        //THEN
        val expected = PageDto(1, 1, 0, 1, listOf(expectedResultFactory.buildBusinessPartnerOutput(
            createdInput,
            connectedGoldenRecords.legalEntityParent,
            connectedGoldenRecords.siteParent,
            connectedGoldenRecords.additionalAddress
        )))
        assertRepo.assertBusinessPartnerOutput(response, expected)
    }

    /**
     * GIVEN several business partner outputs
     * WHEN output consumer searches for all outputs
     * THEN output consumer sees those outputs
     *
     */
    @Test
    fun `find all business partner outputs`(){
        //GIVEN
        val externalId1 = testName
        val externalId2 = "$testName 1"
        val externalId3 = "$testName 2"

        val createdInput1 = testDataClient.createBusinessPartnerInput(externalId1)
        val createdInput2 = testDataClient.createBusinessPartnerInput(externalId2)
        val createdInput3 = testDataClient.createBusinessPartnerInput(externalId3)

        val goldenRecords1 = testDataClient.refineToAdditionalAddressOfSite(createdInput1, externalId1)
        val goldenRecords2 = testDataClient.refineToAdditionalAddressOfSite(createdInput2, externalId2)
        val goldenRecords3 = testDataClient.refineToAdditionalAddressOfSite(createdInput3, externalId3)

        //WHEN
        val response = gateClient.businessPartners.getBusinessPartnersOutput(emptyList())

        //THEN
        val expectedContent = listOf(createdInput1, createdInput2, createdInput3).zip(listOf(goldenRecords1, goldenRecords2, goldenRecords3)){ input, goldenRecords ->
            expectedResultFactory.buildBusinessPartnerOutput(
                input,
                goldenRecords.legalEntityParent,
                goldenRecords.siteParent,
                goldenRecords.additionalAddress
            )
        }
        val expected = PageDto(3, 1, 0, 3, expectedContent)

        assertRepo.assertBusinessPartnerOutput(response, expected)
    }

}