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

package org.eclipse.tractusx.bpdm.pool.v7.businesspartner

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.BusinessPartnerSearchFilterType
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException

class BusinessPartnerSearchV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN search request with all null parameters
     * WHEN sharing member submits search
     * THEN 400 bad request is returned
     */
    @Test
    fun `null search parameters return 400 bad request`() {
        //GIVEN
        val request = LegalEntityPropertiesSearchRequest(null, null, null, null, null, null)

        //WHEN / THEN
        Assertions.assertThatThrownBy {
            poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities), PaginationRequest(0, 100))
        }.isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN search request with all empty string parameters
     * WHEN sharing member submits search
     * THEN 400 bad request is returned
     */
    @Test
    fun `empty string search parameters return 400 bad request`() {
        //GIVEN
        val request = LegalEntityPropertiesSearchRequest("", "", "", "", "", null)

        //WHEN / THEN
        Assertions.assertThatThrownBy {
            poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities), PaginationRequest(0, 100))
        }.isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN legal entity
     * WHEN sharing member searches by BPNL
     * THEN sharing member finds the legal entity
     */
    @Test
    fun `search by BPNL returns matching legal entity`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        testDataClient.createLegalEntity("Other $testName")
        val bpnl = legalEntityResponse.header.bpnl

        //WHEN
        val searchResponse = poolClient.businessPartners.searchBusinessPartners(
            LegalEntityPropertiesSearchRequest(null, bpnl, null, null, null, null),
            setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),
            PaginationRequest(0, 100)
        )

        //THEN
        Assertions.assertThat(searchResponse.totalElements).isEqualTo(1)
        Assertions.assertThat(searchResponse.content.single().legalEntity.legalEntityBpn).isEqualTo(bpnl)
    }

    /**
     * GIVEN legal entity
     * WHEN sharing member searches by BPNL with incorrect format
     * THEN empty result is returned
     */
    @Test
    fun `search by invalid BPNL format returns empty result`() {
        //GIVEN
        testDataClient.createLegalEntity(testName)

        //WHEN
        val searchResponse = poolClient.businessPartners.searchBusinessPartners(
            LegalEntityPropertiesSearchRequest(null, "INVALIDBPN", null, null, null, null),
            setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),
            PaginationRequest(0, 100)
        )

        //THEN
        Assertions.assertThat(searchResponse.content).isEmpty()
    }

    /**
     * GIVEN legal entity
     * WHEN sharing member searches by legal name
     * THEN sharing member finds only the matching legal entity
     */
    @Test
    fun `search by legal name returns matching legal entity`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        testDataClient.createLegalEntity("Other $testName")
        val legalName = legalEntityResponse.header.legalName

        //WHEN
        val searchResponse = poolClient.businessPartners.searchBusinessPartners(
            LegalEntityPropertiesSearchRequest(legalName, null, null, null, null, null),
            setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),
            PaginationRequest(0, 100)
        )

        //THEN
        Assertions.assertThat(searchResponse.totalElements).isEqualTo(1)
        Assertions.assertThat(searchResponse.content.single().legalEntity.legalEntityBpn).isEqualTo(legalEntityResponse.header.bpnl)
    }

    /**
     * GIVEN legal entity
     * WHEN sharing member searches by BPNL combined with legal name
     * THEN sharing member finds the legal entity matching both
     */
    @Test
    fun `search by BPNL and legal name returns matching legal entity`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        testDataClient.createLegalEntity("Other $testName")
        val bpnl = legalEntityResponse.header.bpnl
        val legalName = legalEntityResponse.header.legalName

        //WHEN
        val searchResponse = poolClient.businessPartners.searchBusinessPartners(
            LegalEntityPropertiesSearchRequest(legalName, bpnl, null, null, null, null),
            setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),
            PaginationRequest(0, 100)
        )

        //THEN
        Assertions.assertThat(searchResponse.totalElements).isEqualTo(1)
        Assertions.assertThat(searchResponse.content.single().legalEntity.legalEntityBpn).isEqualTo(bpnl)
    }

    /**
     * GIVEN legal entity
     * WHEN sharing member searches by city of its legal address
     * THEN sharing member finds the legal entity
     */
    @Test
    fun `search by city returns matching legal entity`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val bpnl = legalEntityResponse.header.bpnl
        val city = legalEntityResponse.legalAddress.physicalPostalAddress.city!!

        //WHEN
        val searchResponse = poolClient.businessPartners.searchBusinessPartners(
            LegalEntityPropertiesSearchRequest(null, bpnl, null, null, city, null),
            setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),
            PaginationRequest(0, 100)
        )

        //THEN
        Assertions.assertThat(searchResponse.totalElements).isEqualTo(1)
        Assertions.assertThat(searchResponse.content.single().legalEntity.legalEntityBpn).isEqualTo(bpnl)
    }

    /**
     * GIVEN legal entity
     * WHEN sharing member searches by street name of its legal address
     * THEN sharing member finds the legal entity
     */
    @Test
    fun `search by street name returns matching legal entity`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val bpnl = legalEntityResponse.header.bpnl
        val streetName = legalEntityResponse.legalAddress.physicalPostalAddress.street!!.name!!

        //WHEN
        val searchResponse = poolClient.businessPartners.searchBusinessPartners(
            LegalEntityPropertiesSearchRequest(null, bpnl, streetName, null, null, null),
            setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),
            PaginationRequest(0, 100)
        )

        //THEN
        Assertions.assertThat(searchResponse.totalElements).isEqualTo(1)
        Assertions.assertThat(searchResponse.content.single().legalEntity.legalEntityBpn).isEqualTo(bpnl)
    }

    /**
     * GIVEN legal entity with site and additional address
     * WHEN sharing member searches with IncludeLegalEntities filter
     * THEN result contains the legal entity with its legal address
     */
    @Test
    fun `IncludeLegalEntities filter returns legal entity with legal address`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val bpnl = legalEntityResponse.header.bpnl
        val siteResponse = testDataClient.createSite(legalEntityResponse, testName)
        testDataClient.createAdditionalAddress(legalEntityResponse, testName)

        //WHEN
        val searchResponse = poolClient.businessPartners.searchBusinessPartners(
            LegalEntityPropertiesSearchRequest(null, bpnl, null, null, null, null),
            setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),
            PaginationRequest(0, 100)
        )

        //THEN
        Assertions.assertThat(searchResponse.totalElements).isEqualTo(1)
        val result = searchResponse.content.single()
        Assertions.assertThat(result.legalEntity.legalEntityBpn).isEqualTo(bpnl)
        Assertions.assertThat(result.site).isNull()
        Assertions.assertThat(result.address.addressType).isEqualTo(AddressType.LegalAddress)
        Assertions.assertThat(result.address.addressBpn).isEqualTo(legalEntityResponse.legalAddress.bpna)
    }

    /**
     * GIVEN legal entity with site
     * WHEN sharing member searches with IncludeSites filter
     * THEN result contains the site with its main address
     */
    @Test
    fun `IncludeSites filter returns site with main address`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val bpnl = legalEntityResponse.header.bpnl
        val siteResponse = testDataClient.createSite(legalEntityResponse, testName)

        //WHEN
        val searchResponse = poolClient.businessPartners.searchBusinessPartners(
            LegalEntityPropertiesSearchRequest(null, bpnl, null, null, null, null),
            setOf(BusinessPartnerSearchFilterType.IncludeSites),
            PaginationRequest(0, 100)
        )

        //THEN
        Assertions.assertThat(searchResponse.totalElements).isEqualTo(1)
        val result = searchResponse.content.single()
        Assertions.assertThat(result.legalEntity.legalEntityBpn).isEqualTo(bpnl)
        Assertions.assertThat(result.site?.siteBpn).isEqualTo(siteResponse.site.bpns)
        Assertions.assertThat(result.address.addressType).isEqualTo(AddressType.SiteMainAddress)
        Assertions.assertThat(result.address.addressBpn).isEqualTo(siteResponse.mainAddress.bpna)
    }

    /**
     * GIVEN legal entity with additional address
     * WHEN sharing member searches with IncludeAdditionalAddresses filter
     * THEN result contains the additional address
     */
    @Test
    fun `IncludeAdditionalAddresses filter returns additional address`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val bpnl = legalEntityResponse.header.bpnl
        val additionalAddress = testDataClient.createAdditionalAddress(legalEntityResponse, testName)

        //WHEN
        val searchResponse = poolClient.businessPartners.searchBusinessPartners(
            LegalEntityPropertiesSearchRequest(null, bpnl, null, null, null, null),
            setOf(BusinessPartnerSearchFilterType.IncludeAdditionalAddresses),
            PaginationRequest(0, 100)
        )

        //THEN
        Assertions.assertThat(searchResponse.totalElements).isEqualTo(1)
        val result = searchResponse.content.single()
        Assertions.assertThat(result.legalEntity.legalEntityBpn).isEqualTo(bpnl)
        Assertions.assertThat(result.site).isNull()
        Assertions.assertThat(result.address.addressType).isEqualTo(AddressType.AdditionalAddress)
        Assertions.assertThat(result.address.addressBpn).isEqualTo(additionalAddress.address.bpna)
    }

    /**
     * GIVEN legal entity with site and additional address
     * WHEN sharing member searches with all filter types
     * THEN result contains all three business partner representations
     */
    @Test
    fun `all filter types combined return legal entity site and additional address`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val bpnl = legalEntityResponse.header.bpnl
        val siteResponse = testDataClient.createSite(legalEntityResponse, testName)
        val additionalAddress = testDataClient.createAdditionalAddress(legalEntityResponse, testName)

        //WHEN
        val searchResponse = poolClient.businessPartners.searchBusinessPartners(
            LegalEntityPropertiesSearchRequest(null, bpnl, null, null, null, null),
            setOf(
                BusinessPartnerSearchFilterType.IncludeLegalEntities,
                BusinessPartnerSearchFilterType.IncludeSites,
                BusinessPartnerSearchFilterType.IncludeAdditionalAddresses
            ),
            PaginationRequest(0, 100)
        )

        //THEN
        Assertions.assertThat(searchResponse.totalElements).isEqualTo(3)
        val addressTypes = searchResponse.content.map { it.address.addressType }.toSet()
        Assertions.assertThat(addressTypes).containsExactlyInAnyOrder(
            AddressType.LegalAddress,
            AddressType.SiteMainAddress,
            AddressType.AdditionalAddress
        )
    }
}
