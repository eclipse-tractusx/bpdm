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

package org.eclipse.tractusx.bpdm.pool.v6.util

import org.eclipse.tractusx.bpdm.pool.api.v6.client.PoolV6ApiClient
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerCreateVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerCreateVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateVerboseDtoV6
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
    private val poolClient: PoolV6ApiClient
) {

    fun createLegalEntity(seed: String): LegalEntityPartnerCreateVerboseDtoV6{
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(seed)
        val legalEntityResponse = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        return legalEntityResponse
    }

    fun createMemberLegalEntity(seed: String) = createLegalEntityWithMemberOverwrite(seed, true)
    fun createNonMemberLegalEntity(seed: String) = createLegalEntityWithMemberOverwrite(seed, false)

    fun updateLegalEntity(legalEntity: LegalEntityPartnerCreateVerboseDtoV6, seed: String): LegalEntityPartnerCreateVerboseDtoV6{
        val legalEntityRequest = requestFactory.createLegalEntityUpdateRequest(seed, legalEntity.legalEntity.bpnl)
        val legalEntityResponse = poolClient.legalEntities.updateBusinessPartners(listOf(legalEntityRequest)).entities.single()

        return legalEntityResponse
    }

    fun createSiteFor(legalEntity: LegalEntityPartnerCreateVerboseDtoV6, seed: String): SitePartnerCreateVerboseDtoV6{
        val siteRequest = requestFactory.buildSiteCreateRequest(seed, legalEntity.legalEntity.bpnl)
        val siteResponse = poolClient.sites.createSite(listOf(siteRequest)).entities.single()

        return siteResponse
    }

    fun updateSite(site: SitePartnerCreateVerboseDtoV6, seed: String): SitePartnerCreateVerboseDtoV6{
        val siteRequest = requestFactory.createSiteUpdateRequest(seed, site.site.bpns)
        val siteResponse = poolClient.sites.updateSite(listOf(siteRequest)).entities.single()

        return siteResponse
    }

    fun createLegalAddressSiteFor(legalEntity: LegalEntityPartnerCreateVerboseDtoV6, seed: String): SitePartnerCreateVerboseDtoV6{
        val siteRequest = requestFactory.buildLegalAddressSiteCreateRequest(seed, legalEntity.legalEntity.bpnl)
        val siteResponse = poolClient.sites.createSiteWithLegalReference(listOf(siteRequest)).entities.single()

        return siteResponse
    }

    fun createAdditionalAddressFor(legalEntity: LegalEntityPartnerCreateVerboseDtoV6, seed: String): AddressPartnerCreateVerboseDtoV6{
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(seed, legalEntity.legalEntity.bpnl)
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest)).entities.single()

        return addressResponse
    }

    fun createAdditionalAddressFor(site: SitePartnerCreateVerboseDtoV6, seed: String): AddressPartnerCreateVerboseDtoV6{
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(seed, site.site.bpns)
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest)).entities.single()

        return addressResponse
    }

    fun updateAddress(address: AddressPartnerCreateVerboseDtoV6, seed: String): LogisticAddressVerboseDtoV6{
        val addressRequest = requestFactory.buildAddressUpdateRequest(seed, address.address.bpna)
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest)).entities.single()

        return addressResponse
    }


    private fun createLegalEntityWithMemberOverwrite(seed: String, isMember: Boolean): LegalEntityPartnerCreateVerboseDtoV6{
        val request =  with(requestFactory.buildLegalEntityCreateRequest(seed))
        { copy(legalEntity = legalEntity.copy(isCatenaXMemberData = isMember)) }

        return poolClient.legalEntities.createBusinessPartners(listOf(request)).entities.single()
    }
}