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

package org.eclipse.tractusx.bpdm.pool.auth

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierTypeDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerRequestFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestMetadata
import org.eclipse.tractusx.bpdm.test.util.AuthAssertionHelper
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test

abstract class AuthTestBase(
    private val poolApiClient: PoolApiClient,
    private val addressAuthExpectations: AddressAuthExpectations,
    private val siteAuthExpectations: SiteAuthExpectations,
    private val legalEntityAuthExpectations: LegalEntityAuthExpectations,
    private val metadataAuthExpectations: MetadataAuthExpectations,
    private val membersAuthExpectations: MembersAuthExpectations,
    private val membershipAuthExpectations: CxMembershipsAuthExpectations,
    private val changelogAuthExpectation: AuthExpectationType,
    private val bpnAuthExpectation: AuthExpectationType
) {
    private val authAssertions = AuthAssertionHelper()
    private val requestFactory = BusinessPartnerRequestFactory(TestMetadata(emptyList(), emptyList(), emptyList(), emptyList()))


    @Test
    fun `GET Addresses`() {
        authAssertions.assert(addressAuthExpectations.getAddresses) { poolApiClient.addresses.getAddresses(AddressSearchRequest(), PaginationRequest()) }
    }

    @Test
    fun `GET Address`() {
        authAssertions.assert(addressAuthExpectations.getAddress) { poolApiClient.addresses.getAddress("BPNA") }
    }

    @Test
    fun `POST Address Search`(){
        authAssertions.assert(addressAuthExpectations.postAddressSearch) { poolApiClient.addresses.searchAddresses(AddressSearchRequest(), PaginationRequest()) }
    }

    @Test
    fun `POST Addresses`(){
        authAssertions.assert(addressAuthExpectations.postAddresses) { poolApiClient.addresses.createAddresses(listOf(requestFactory.createAddressRequest("1", ""))) }
    }

    @Test
    fun `PUT Addresses`(){
        authAssertions.assert(addressAuthExpectations.putAddresses) { poolApiClient.addresses.updateAddresses(listOf(requestFactory.createAddressUpdateRequest("1", ""))) }
    }

    @Test
    fun `GET Sites`() {
        authAssertions.assert(siteAuthExpectations.getSites) { poolApiClient.sites.getSites(SiteSearchRequest(), PaginationRequest()) }
    }

    @Test
    fun `GET Site`() {
        authAssertions.assert(siteAuthExpectations.getSite) { poolApiClient.sites.getSite("BPNS") }
    }

    @Test
    fun `POST Site Search`(){
        authAssertions.assert(siteAuthExpectations.postSiteSearch) { poolApiClient.sites.postSiteSearch(SiteSearchRequest(), PaginationRequest()) }
    }

    @Test
    fun `POST Sites`(){
        authAssertions.assert(siteAuthExpectations.postSites) { poolApiClient.sites.createSite(listOf(requestFactory.createSiteRequest("1", "BPNL"))) }
    }

    @Test
    fun `POST SitesLegalReference`(){
        authAssertions.assert(siteAuthExpectations.postSites) { poolApiClient.sites.createSiteWithLegalReference(listOf(requestFactory.createSiteWithLegalReference("1", "BPNL"))) }
    }

    @Test
    fun `PUT Sites`(){
        authAssertions.assert(siteAuthExpectations.putSites) { poolApiClient.sites.updateSite(listOf(requestFactory.createSiteUpdateRequest("1", "BPNS"))) }
    }

    @Test
    fun `GET Legal Entities`() {
        authAssertions.assert(legalEntityAuthExpectations.getLegalEntities) { poolApiClient.legalEntities.getLegalEntities(LegalEntitySearchRequest(), PaginationRequest()) }
    }

    @Test
    fun `GET Legal Entity`() {
        authAssertions.assert(legalEntityAuthExpectations.getLegalEntity) { poolApiClient.legalEntities.getLegalEntity("BPNL") }
    }

    @Test
    fun `POST Legal Entity Search`(){
        authAssertions.assert(legalEntityAuthExpectations.postLegalEntitySearch) { poolApiClient.legalEntities.postLegalEntitySearch(LegalEntitySearchRequest(), PaginationRequest()) }
    }

    @Test
    fun `POST Legal Entities`(){
        authAssertions.assert(legalEntityAuthExpectations.postLegalEntities) { poolApiClient.legalEntities.createBusinessPartners(listOf(requestFactory.createLegalEntityRequest("1"))) }
    }

    @Test
    fun `PUT Legal Entities`(){
        authAssertions.assert(legalEntityAuthExpectations.putLegalEntities) { poolApiClient.legalEntities.updateBusinessPartners(listOf(requestFactory.createLegalEntityUpdateRequest("1", "BPNL"))) }
    }

    @Test
    fun `GET Legal Entity Addresses`(){
        authAssertions.assert(legalEntityAuthExpectations.getLegalEntityAddresses) { poolApiClient.legalEntities.getAddresses("BPNL", PaginationRequest()) }
    }

    @Test
    fun `GET Legal Entity Sites`(){
        authAssertions.assert(legalEntityAuthExpectations.getLegalEntitySites) { poolApiClient.legalEntities.getSites("BPNL", PaginationRequest()) }
    }

    @Test
    fun `GET Legal Form`(){
        authAssertions.assert(metadataAuthExpectations.getLegalForm) { poolApiClient.metadata.getLegalForms(PaginationRequest()) }
    }

    @Test
    fun `POST Legal Form`(){
        authAssertions.assert(metadataAuthExpectations.postLegalForm) { poolApiClient.metadata.createLegalForm(LegalFormRequest("LF", "", null, null, null, null, null, null, false)) }
    }

    @Test
    fun `GET Identifier Type`(){
        authAssertions.assert(metadataAuthExpectations.getIdentifierType) { poolApiClient.metadata.getIdentifierTypes(PaginationRequest(), IdentifierBusinessPartnerType.ADDRESS, CountryCode.CA) }
    }

    @Test
    fun `POST Identifier Type`(){
        authAssertions.assert(metadataAuthExpectations.postIdentifierType) { poolApiClient.metadata.createIdentifierType(IdentifierTypeDto("ID", IdentifierBusinessPartnerType.ADDRESS, "", "", "", "")) }
    }

    @Test
    fun `GET Admin Areas Level 1`(){
        authAssertions.assert(metadataAuthExpectations.getAdminArea) { poolApiClient.metadata.getAdminAreasLevel1(PaginationRequest()) }
    }

    @Test
    fun `GET Field Quality Rules`(){
        authAssertions.assert(metadataAuthExpectations.getFieldQualityRules) { poolApiClient.metadata.getFieldQualityRules(CountryCode.CA) }
    }

    @Test
    fun `POST Members Address Search`(){
        authAssertions.assert(membersAuthExpectations.postAddressSearch) { poolApiClient.members.searchAddresses(AddressSearchRequest(), PaginationRequest()) }
    }

    @Test
    fun `POST Members Site Search`(){
        authAssertions.assert(membersAuthExpectations.postSiteSearch) { poolApiClient.members.postSiteSearch(SiteSearchRequest(), PaginationRequest()) }
    }

    @Test
    fun `POST Members Legal Entity Search`(){
        authAssertions.assert(membersAuthExpectations.postLegalEntitySearch) { poolApiClient.members.searchLegalEntities(LegalEntitySearchRequest(), PaginationRequest()) }
    }

    @Test
    fun `POST Members Changelog Search`(){
        authAssertions.assert(membersAuthExpectations.postChangelogSearch) { poolApiClient.members.searchChangelogEntries(ChangelogSearchRequest(), PaginationRequest()) }
    }

    @Test
    fun `POST Changelog Search`(){
        authAssertions.assert(changelogAuthExpectation) { poolApiClient.changelogs.getChangelogEntries(ChangelogSearchRequest(), PaginationRequest()) }
    }

    @Test
    fun `POST Bpn Search`(){
        authAssertions.assert(bpnAuthExpectation) { poolApiClient.bpns.findBpnsByIdentifiers(IdentifiersSearchRequest(IdentifierBusinessPartnerType.ADDRESS, "ID", emptyList())) }
    }

    @Test
    fun `GET CX Memberships`(){
        authAssertions.assert(membershipAuthExpectations.getMemberships) { poolApiClient.participants.get(DataSpaceParticipantSearchRequest(), PaginationRequest()) }
    }

    @Test
    fun `PUT CX Memberships`(){
        authAssertions.assert(membershipAuthExpectations.putMemberships) { poolApiClient.participants.put(DataSpaceParticipantUpdateRequest(listOf())) }
    }
}


data class AddressAuthExpectations(
    val getAddresses: AuthExpectationType,
    val getAddress: AuthExpectationType,
    val postAddressSearch: AuthExpectationType,
    val postAddresses: AuthExpectationType,
    val putAddresses: AuthExpectationType
)

data class SiteAuthExpectations(
    val getSites: AuthExpectationType,
    val getSite: AuthExpectationType,
    val postSiteSearch: AuthExpectationType,
    val postSites: AuthExpectationType,
    val putSites: AuthExpectationType
)

data class LegalEntityAuthExpectations(
    val getLegalEntities: AuthExpectationType,
    val getLegalEntity: AuthExpectationType,
    val postLegalEntitySearch: AuthExpectationType,
    val postLegalEntities: AuthExpectationType,
    val putLegalEntities: AuthExpectationType,
    val getLegalEntityAddresses: AuthExpectationType,
    val getLegalEntitySites: AuthExpectationType
)

data class MetadataAuthExpectations(
    val getLegalForm: AuthExpectationType,
    val postLegalForm: AuthExpectationType,
    val getIdentifierType: AuthExpectationType,
    val postIdentifierType: AuthExpectationType,
    val getAdminArea: AuthExpectationType,
    val getFieldQualityRules: AuthExpectationType
)

data class MembersAuthExpectations(
    val postAddressSearch: AuthExpectationType,
    val postSiteSearch: AuthExpectationType,
    val postLegalEntitySearch: AuthExpectationType,
    val postChangelogSearch: AuthExpectationType
)

data class CxMembershipsAuthExpectations(
    val getMemberships: AuthExpectationType,
    val putMemberships: AuthExpectationType
)

