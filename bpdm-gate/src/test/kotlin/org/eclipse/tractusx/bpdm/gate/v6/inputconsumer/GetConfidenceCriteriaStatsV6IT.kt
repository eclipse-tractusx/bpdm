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

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsConfidenceCriteriaResponse
import org.eclipse.tractusx.bpdm.pool.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerRequestFactory
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class GetConfidenceCriteriaStatsV6IT: InputConsumerV6Test() {


    @Autowired
    private lateinit var poolRequestFactory: BusinessPartnerRequestFactory

    /**
     * GIVEN no business partners shared
     * WHEN input consumer requests confidence criteria stats
     * THEN input consumer sees all stats zero
     */
    @Test
    fun `get confidences when no business partner exists`(){
        //WHEN
        val response = gateClient.stats.getConfidenceCriteriaStats()

        //THEN
        val expected = StatsConfidenceCriteriaResponse(0f, 0, 0, 0, 0f)

        Assertions.assertThat(response).isEqualTo(expected)
    }

    /**
     * GIVEN no business partners shared
     * WHEN input consumer requests confidence criteria stats
     * THEN input consumer sees all stats zero
     */
    @Test
    @Disabled("Calculation not correct, refer to issue: https://github.com/eclipse-tractusx/bpdm/issues/1571")
    fun `count confidences`(){
        //GIVEN
        val input1 = testDataClient.createBusinessPartnerInput("$testName 1")
        val input2 = with(testDataClient.createBusinessPartnerInput("$testName 2")){
            copy(isOwnCompanyData = false)
        }

        val legalEntityConfidence1 = ConfidenceCriteriaDto(true, true, 2, LocalDateTime.now(), LocalDateTime.now(), 8)
        val siteConfidence1 = ConfidenceCriteriaDto(true, false, 1, LocalDateTime.now(), LocalDateTime.now(), 3)
        val addressConfidence1 = ConfidenceCriteriaDto(true, false, 2, LocalDateTime.now(), LocalDateTime.now(), 5)

        val legalEntityConfidence2 = ConfidenceCriteriaDto(false, false, 3, LocalDateTime.now(), LocalDateTime.now(), 1)
        val siteConfidence2 = ConfidenceCriteriaDto(false, false, 1, LocalDateTime.now(), LocalDateTime.now(), 0)
        val addressConfidence2 = ConfidenceCriteriaDto(false, false, 3, LocalDateTime.now(), LocalDateTime.now(), 1)

        val legalEntityRequest1 = with(poolRequestFactory.createLegalEntityRequest("$testName 1")){
            copy(legalEntity = legalEntity.copy(confidenceCriteria = legalEntityConfidence1))
        }

        val siteRequest1 =  with(poolRequestFactory.buildSiteCreateRequest(testName, "BPNL$testName 1")){
            copy(site = site.copy(confidenceCriteria = siteConfidence1))
        }

        val addAddressRequest1 = with(poolRequestFactory.buildAdditionalAddressCreateRequest(testName, "BPNS$testName 1")){
            copy(address = address.copy(confidenceCriteria = addressConfidence1))
        }

        val legalEntityRequest2 = with(poolRequestFactory.createLegalEntityRequest("$testName 2")){
            copy(legalEntity = legalEntity.copy(confidenceCriteria = legalEntityConfidence2))
        }

        val siteRequest2 =  with(poolRequestFactory.buildSiteCreateRequest("$testName 2", "BPNL$testName 2")){
            copy(site = site.copy(confidenceCriteria = siteConfidence2))
        }

        val addAddressRequest2 = with(poolRequestFactory.buildAdditionalAddressCreateRequest("$testName 2", "BPNL$testName 2")){
            copy(address = address.copy(confidenceCriteria = addressConfidence2))
        }

        testDataClient.refineToAdditionalAddressOfSite(input1, legalEntityRequest1, siteRequest1, addAddressRequest1)
        testDataClient.refineToAdditionalAddressOfSite(input2, legalEntityRequest2, siteRequest2, addAddressRequest2)

        //WHEN
        val response = gateClient.stats.getConfidenceCriteriaStats()

        //THEN
        val confidenceCriteriaCount = 6f
        val sharingMemberSum = legalEntityConfidence1.numberOfSharingMembers + siteConfidence1.numberOfSharingMembers + addressConfidence1.numberOfSharingMembers + legalEntityConfidence2.numberOfSharingMembers + siteConfidence2.numberOfSharingMembers + addressConfidence2.numberOfSharingMembers
        val expectedSharingMemberAverage = sharingMemberSum  / confidenceCriteriaCount
        val expected = StatsConfidenceCriteriaResponse(expectedSharingMemberAverage, 2, 3, 3, 3f)

        Assertions.assertThat(response).isEqualTo(expected)
    }
}