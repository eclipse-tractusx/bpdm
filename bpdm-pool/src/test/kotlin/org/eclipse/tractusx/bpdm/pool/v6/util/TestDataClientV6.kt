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

import org.eclipse.tractusx.bpdm.pool.api.v6.client.PoolV6Client
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.LogisticAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerCreateVerboseV6Dto
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
    private val poolClient: PoolV6Client
) {

    fun createLegalEntity(seed: String): LegalEntityPartnerCreateVerboseV6Dto{
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(seed)
        val legalEntityResponse = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        return legalEntityResponse
    }

    fun createMemberLegalEntity(seed: String) = createLegalEntityWithMemberOverwrite(seed, true)
    fun createNonMemberLegalEntity(seed: String) = createLegalEntityWithMemberOverwrite(seed, false)

    fun updateLegalEntity(legalEntity: LegalEntityPartnerCreateVerboseV6Dto, seed: String): LegalEntityPartnerCreateVerboseV6Dto{
        val legalEntityRequest = requestFactory.createLegalEntityUpdateRequest(seed, legalEntity.legalEntity.bpnl)
        val legalEntityResponse = poolClient.legalEntities.updateBusinessPartners(listOf(legalEntityRequest)).entities.single()

        return legalEntityResponse
    }

    fun createSiteFor(legalEntity: LegalEntityPartnerCreateVerboseV6Dto, seed: String): SitePartnerCreateVerboseV6Dto{
        val siteRequest = requestFactory.buildSiteCreateRequest(seed, legalEntity.legalEntity.bpnl)
        val siteResponse = poolClient.sites.createSite(listOf(siteRequest)).entities.single()

        return siteResponse
    }

    fun updateSite(site: SitePartnerCreateVerboseV6Dto, seed: String): SitePartnerCreateVerboseV6Dto{
        val siteRequest = requestFactory.createSiteUpdateRequest(seed, site.site.bpns)
        val siteResponse = poolClient.sites.updateSite(listOf(siteRequest)).entities.single()

        return siteResponse
    }

    fun createLegalAddressSiteFor(legalEntity: LegalEntityPartnerCreateVerboseV6Dto, seed: String): SitePartnerCreateVerboseV6Dto{
        val siteRequest = requestFactory.buildLegalAddressSiteCreateRequest(seed, legalEntity.legalEntity.bpnl)
        val siteResponse = poolClient.sites.createSiteWithLegalReference(listOf(siteRequest)).entities.single()

        return siteResponse
    }

    fun createAdditionalAddressFor(legalEntity: LegalEntityPartnerCreateVerboseV6Dto, seed: String): AddressPartnerCreateVerboseV6Dto{
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(seed, legalEntity.legalEntity.bpnl)
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest)).entities.single()

        return addressResponse
    }

    fun createAdditionalAddressFor(site: SitePartnerCreateVerboseV6Dto, seed: String): AddressPartnerCreateVerboseV6Dto{
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(seed, site.site.bpns)
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest)).entities.single()

        return addressResponse
    }

    fun updateAddress(address: AddressPartnerCreateVerboseV6Dto, seed: String): LogisticAddressVerboseV6Dto{
        val addressRequest = requestFactory.buildAddressUpdateRequest(seed, address.address.bpna)
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest)).entities.single()

        return addressResponse
    }


    private fun createLegalEntityWithMemberOverwrite(seed: String, isMember: Boolean): LegalEntityPartnerCreateVerboseV6Dto{
        val request =  with(requestFactory.buildLegalEntityCreateRequest(seed))
        { copy(legalEntity = legalEntity.copy(isCatenaXMemberData = isMember)) }

        return poolClient.legalEntities.createBusinessPartners(listOf(request)).entities.single()
    }
}