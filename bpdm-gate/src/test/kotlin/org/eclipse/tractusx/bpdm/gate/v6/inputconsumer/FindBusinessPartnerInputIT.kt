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

package org.eclipse.tractusx.bpdm.gate.v6.inputconsumer

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.junit.jupiter.api.Test

class FindBusinessPartnerInputIT: InputConsumerV6Test() {

    /**
     * GIVEN business partner input under external-ID
     * WHEN input consumer searches for business partner under external-ID
     * THEN input consumer receives the given business partner input
     */
    @Test
    fun `find created business partner input`(){
        //GIVEN
        val givenBusinessPartnerInput = testDataClient.createBusinessPartnerInput(testName)

        //WHEN
        val response = gateClient.businessPartners.getBusinessPartnersInput(listOf(testName))

        //THEN
        val expected = PageDto(1, 1, 0, 1, listOf(givenBusinessPartnerInput))
        assertRepo.assertBusinessPartnerInput(response, expected)
    }

    /**
     * GIVEN business partner input under external-ID
     * WHEN input consumer searches for business partner under a wrong external-ID
     * THEN input consumer receives an empty result
     */
    @Test
    fun `try find created business partner non-existent external-ID`(){
        //GIVEN
        testDataClient.createBusinessPartnerInput(testName)

        //WHEN
        val response = gateClient.businessPartners.getBusinessPartnersInput(listOf("NOT_EXISTS"))

        //THEN
        val expected = PageDto<BusinessPartnerInputDto>(0, 0, 0, 0, emptyList())
        assertRepo.assertBusinessPartnerInput(response, expected)
    }

}