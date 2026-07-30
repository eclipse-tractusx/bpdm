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

package org.eclipse.tractusx.bpdm.pool.v6

import org.junit.jupiter.api.Test

/**
 * The v6 API has no script variants: they postdate its contract. Since a write replaces a business partner's full
 * content, a v6 write necessarily drops the script variants the business partner gained through v7 or the task path.
 * These tests hold that accepted data loss in place so it is not mistaken for a defect.
 */
class ScriptVariantsV6IT: UnscheduledPoolTestBaseV6AndV7() {

    /**
     * GIVEN address with script variants
     * WHEN operator updates that address over the v6 API
     * THEN the address has lost its script variants
     */
    @Test
    fun `update address over v6 drops its script variants`() {
        //GIVEN
        val legalEntity = testDataClientV7.createLegalEntity(testName)
        val createdAddress = testDataClientV7.createAdditionalAddress(legalEntity, testName)

        //WHEN
        val updateRequest = testDataFactory.request.buildAddressUpdateRequest("Updated $testName", createdAddress.address.bpna)
        poolClient.addresses.updateAddresses(listOf(updateRequest))

        //THEN
        val updatedAddress = poolClientV7.addresses.getAddress(createdAddress.address.bpna)
        assertRepositoryV7.assertAddressScriptVariantsCleared(updatedAddress, createdAddress.scriptVariants)
    }

    /**
     * GIVEN site with script variants
     * WHEN operator updates that site over the v6 API
     * THEN the site has lost its script variants
     */
    @Test
    fun `update site over v6 drops its script variants`() {
        //GIVEN
        val legalEntity = testDataClientV7.createLegalEntity(testName)
        val createdSite = testDataClientV7.createSite(legalEntity, testName)

        //WHEN
        val updateRequest = testDataFactory.request.createSiteUpdateRequest("Updated $testName", createdSite.site.bpns)
        poolClient.sites.updateSite(listOf(updateRequest))

        //THEN
        val updatedSite = poolClientV7.sites.getSite(createdSite.site.bpns)
        assertRepositoryV7.assertSiteScriptVariantsCleared(updatedSite, createdSite.site.scriptVariants)
    }
}
