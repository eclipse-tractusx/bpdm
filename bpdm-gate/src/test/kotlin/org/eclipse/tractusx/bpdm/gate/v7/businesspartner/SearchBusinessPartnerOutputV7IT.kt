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

package org.eclipse.tractusx.bpdm.gate.v7.businesspartner

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.junit.jupiter.api.Test

class SearchBusinessPartnerOutputV7IT: UnscheduledGateTestBaseV7() {

    /**
     * GIVEN business partner output based on legal entity
     * WHEN output consumer searches for output with that external-ID
     * THEN output consumer sees received golden record result
     *
     */
    @Test
    fun `search legal entity business partner output`(){
        //GIVEN
        val createdInput = testDataClient.upsertBusinessPartnerInput(testName)
        val legalEntityGoldenRecord = testDataClient.refineToLegalEntity(createdInput)

        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(createdInput.externalId))

        //THEN
        val expectedOutput =  testData.businessPartner.output.fromLegalEntity(createdInput, legalEntityGoldenRecord)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertBusinessPartnerOutput(response, expectedResponse)
    }

    /**
     * GIVEN business partner output based on legal entity that is also a site main address
     * WHEN output consumer searches for output with that external-ID
     * THEN output consumer sees golden record result with both legal entity and site references
     *
     */
    @Test
    fun `search legal entity on site business partner output`(){
        //GIVEN
        val createdInput = testDataClient.upsertBusinessPartnerInput(testName)
        val goldenRecord = testDataClient.refineToLegalEntityOnSite(createdInput)

        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(createdInput.externalId))

        //THEN
        val expectedOutput = testData.businessPartner.output.fromLegalEntityOnSite(createdInput, goldenRecord.legalEntityParent, goldenRecord.site)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertBusinessPartnerOutput(response, expectedResponse)
    }

    /**
     * GIVEN business partner output based on site main address
     * WHEN output consumer searches for output with that external-ID
     * THEN output consumer sees golden record result with site address and legal entity reference
     *
     */
    @Test
    fun `search site business partner output`(){
        //GIVEN
        val createdInput = testDataClient.upsertBusinessPartnerInput(testName)
        val goldenRecord = testDataClient.refineToSite(createdInput)

        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(createdInput.externalId))

        //THEN
        val expectedOutput = testData.businessPartner.output.fromSite(createdInput, goldenRecord.legalEntityParent, goldenRecord.site)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertBusinessPartnerOutput(response, expectedResponse)
    }

    /**
     * GIVEN business partner output based on additional address belonging to a site
     * WHEN output consumer searches for output with that external-ID
     * THEN output consumer sees golden record result with additional address, site, and legal entity references
     *
     */
    @Test
    fun `search additional address of site business partner output`(){
        //GIVEN
        val createdInput = testDataClient.upsertBusinessPartnerInput(testName)
        val goldenRecord = testDataClient.refineToAdditionalAddressOfSite(createdInput)

        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(createdInput.externalId))

        //THEN
        val expectedOutput = testData.businessPartner.output.fromAdditionalAddressOnSite(createdInput, goldenRecord.legalEntityParent, goldenRecord.siteParent, goldenRecord.additionalAddress)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertBusinessPartnerOutput(response, expectedResponse)
    }

    /**
     * GIVEN no business partner output for a given external-ID
     * WHEN output consumer searches for output with that external-ID
     * THEN output consumer sees an empty result
     *
     */
    @Test
    fun `search business partner output by non-existing external-ID`(){
        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf("non-existing-external-id"))

        //THEN
        val expectedResponse = PageDto(0, 0, 0, 0, emptyList<BusinessPartnerOutputDto>())

        assertRepo.assertBusinessPartnerOutput(response, expectedResponse)
    }

    /**
     * GIVEN business partner output exists for one external-ID but not for another
     * WHEN output consumer searches for outputs with both external-IDs
     * THEN output consumer sees only the output for the existing external-ID
     *
     */
    @Test
    fun `search business partner output filters out non-existing external-IDs`(){
        //GIVEN
        val createdInput = testDataClient.upsertBusinessPartnerInput(testName)
        val legalEntityGoldenRecord = testDataClient.refineToLegalEntity(createdInput)

        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(createdInput.externalId, "non-existing-external-id"))

        //THEN
        val expectedOutput = testData.businessPartner.output.fromLegalEntity(createdInput, legalEntityGoldenRecord)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertBusinessPartnerOutput(response, expectedResponse)
    }
}