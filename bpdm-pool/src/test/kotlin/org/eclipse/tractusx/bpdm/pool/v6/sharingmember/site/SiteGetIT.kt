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

package org.eclipse.tractusx.bpdm.pool.v6.sharingmember.site

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.pool.v6.sharingmember.SharingMemberTest
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException

class SiteGetIT: SharingMemberTest() {

    /**
     * GIVEN sites
     * WHEN sharing member fetches site by BPNS
     * THEN sharing member sees site
     */
    @Test
    fun `get site by BPNS`(){
        //GIVEN
        val legalEntityResponseA =  testDataClient.createLegalEntity("$testName A")
        val legalEntityResponseB =  testDataClient.createLegalEntity("$testName B")

        val siteResponseA = testDataClient.createLegalAddressSiteFor(legalEntityResponseA, "$testName A")
        testDataClient.createSiteFor(legalEntityResponseA, "$testName B")

        testDataClient.createSiteFor(legalEntityResponseB, "$testName C")
        testDataClient.createSiteFor(legalEntityResponseB, "$testName D")

        //WHEN
        val fetchedSite = poolClient.sites.getSite(siteResponseA.site.bpns)

        //THEN
        val expectedSite = testDataFactory.result.buildExpectedSiteSearchResponse(siteResponseA)

        assertRepository.assertSiteGet(fetchedSite, expectedSite)
    }

    /**
     * WHEN sharing member requests  site by unknown BPNS
     * THEN sharing member sees 404 not found error
     */
    @Test
    fun `try get site by unknown BPNL`(){
        //WHEN
        val unknownGet = {  poolClient.sites.getSite("UNKNOWN"); Unit }

        //THEN
        Assertions.assertThatExceptionOfType(WebClientResponseException.NotFound::class.java).isThrownBy(unknownGet)
    }
}