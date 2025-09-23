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

package org.eclipse.tractusx.bpdm.pool.v6.util

import org.eclipse.tractusx.bpdm.pool.api.v6.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.test.testdata.pool.v6.BusinessPartnerV6RequestFactory

/**
 * Creates new business partner test data based on a given request factory and pool client
 *
 * Use this class as a convenience to quickly create a test environment for a method under test
 *
 * The class expects that we only create valid business partners
 */
class TestDataClientV6(
    private val requestFactory: BusinessPartnerV6RequestFactory,
    private val poolClient: PoolApiClient
) {

    fun createLegalEntity(seed: String): LegalEntityPartnerCreateVerboseDto{
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(seed)
        val legalEntityResponse = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        return legalEntityResponse
    }

    fun createSiteFor(legalEntity: LegalEntityPartnerCreateVerboseDto, seed: String): SitePartnerCreateVerboseDto{
        val siteRequest = requestFactory.buildSiteCreateRequest(seed, legalEntity.legalEntity.bpnl)
        val siteResponse = poolClient.sites.createSite(listOf(siteRequest)).entities.single()

        return siteResponse
    }

    fun createLegalAddressSiteFor(legalEntity: LegalEntityPartnerCreateVerboseDto, seed: String): SitePartnerCreateVerboseDto{
        val siteRequest = requestFactory.buildLegalAddressSiteCreateRequest(seed, legalEntity.legalEntity.bpnl)
        val siteResponse = poolClient.sites.createSiteWithLegalReference(listOf(siteRequest)).entities.single()

        return siteResponse
    }

    fun createAdditionalAddressFor(legalEntity: LegalEntityPartnerCreateVerboseDto, seed: String): AddressPartnerCreateVerboseDto{
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(seed, legalEntity.legalEntity.bpnl)
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest)).entities.single()

        return addressResponse
    }

    fun createAdditionalAddressFor(site: SitePartnerCreateVerboseDto, seed: String): AddressPartnerCreateVerboseDto{
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(seed, site.site.bpns)
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest)).entities.single()

        return addressResponse
    }
}