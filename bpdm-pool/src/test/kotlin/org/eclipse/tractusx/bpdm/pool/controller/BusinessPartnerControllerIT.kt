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

package org.eclipse.tractusx.bpdm.pool.controller

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.pool.api.model.BusinessPartnerSearchFilterType
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.StreetDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.AlternativePostalAddressDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.BusinessPartnerConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.BusinessPartnerIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.BusinessPartnerLegalEntity
import org.eclipse.tractusx.bpdm.pool.api.model.response.BusinessPartnerPostalAddress
import org.eclipse.tractusx.bpdm.pool.api.model.response.BusinessPartnerSearchResultDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.BusinessPartnerSite
import org.eclipse.tractusx.bpdm.pool.api.model.response.PhysicalPostalAddressDto
import org.eclipse.tractusx.bpdm.pool.util.TestHelpers
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.pool.LegalEntityStructureRequest
import org.eclipse.tractusx.bpdm.test.testdata.pool.SiteStructureRequest
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolDataHelper
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestDataEnvironment
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClientResponseException

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class]
)

@ActiveProfiles("test-no-auth")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class BusinessPartnerControllerIT@Autowired constructor(
    val testHelpers: TestHelpers,
    val poolClient: PoolClientImpl,
    val dbTestHelpers: DbTestHelpers,
    private val dataHelper: PoolDataHelper,
) {

    private lateinit var testDataEnvironment: TestDataEnvironment

    private val partnerStructure1 = LegalEntityStructureRequest(
            legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate4,
            siteStructures = listOf(
                SiteStructureRequest(BusinessPartnerNonVerboseValues.siteCreate1)
            )
        )
    private val partnerStructure2 = LegalEntityStructureRequest(
        legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate4
    )

    private lateinit var givenPartner1: LegalEntityVerboseDto
    private lateinit var legalAddress1: LogisticAddressVerboseDto

    @BeforeEach
    fun beforeEach() {
        dbTestHelpers.truncateDbTables()
        testDataEnvironment = dataHelper.createTestDataEnvironment()
        val givenStructure = testHelpers.createBusinessPartnerStructure(listOf(partnerStructure1))
        givenPartner1 = with(givenStructure[0].legalEntity) { legalEntity }
        legalAddress1 = givenStructure[0].legalEntity.legalAddress

        val parentBpn = givenStructure.firstOrNull()!!.legalEntity.legalEntity.bpnl
        val addressToCreate = with(BusinessPartnerNonVerboseValues.addressPartnerCreate1) {
            copy(bpnParent = parentBpn)
        }
        val createAdditionalAddress = poolClient.addresses.createAddresses(listOf(addressToCreate))
        print(createAdditionalAddress)
    }

    /**
     * Given null request
     * Return 400 and error message
     */
    @Test
    fun `Request parameters are all null should return 400 bad request`() {

        val request = LegalEntityPropertiesSearchRequest(null, null, null, null, null, null)

        val ex = assertThrows<WebClientResponseException.BadRequest> {
            poolClient.businessPartners.searchBusinessPartners(
                request,
                setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),
                PaginationRequest(0, 100)
            )
        }
        Assertions.assertEquals(HttpStatus.BAD_REQUEST,  ex.statusCode)
    }

    /**
     * Given empty string request
     * Return 400 and error message
     */
    @Test
    fun `Request parameters are all empty string should return 400 bad request`() {

        val request = LegalEntityPropertiesSearchRequest("", "", "", "", "", null)
        val ex = assertThrows<WebClientResponseException.BadRequest> {
            poolClient.businessPartners.searchBusinessPartners(
                request,
                setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),
                PaginationRequest(0, 100)
            )
        }
        Assertions.assertEquals(HttpStatus.BAD_REQUEST,  ex.statusCode)
    }

    /**
     * Search by BPNL
     *
     */
    @Test
    fun `Search by BPNL`() {

        val expected = PageDto(1, 1, 0, 1, listOf(expectedLegalEntity()))
        val searchLegalName = LegalEntityPropertiesSearchRequest(null, "BPNL000000000065", null, null, null, null)
        val result = poolClient.businessPartners.searchBusinessPartners(searchLegalName, setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)
    }

    /**
     * Search by incorrect BPNL format
     *
     */
    @Test
    fun `Search by incorrect BPNL format`() {

        val expected = PageDto(0, 0, 0, 0,emptyList<BusinessPartnerSearchResultDto>())
        val searchLegalName = LegalEntityPropertiesSearchRequest(null, "BPNL000065", null, null, null, null)
        val result = poolClient.businessPartners.searchBusinessPartners(searchLegalName, setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)
    }

    /**
     * Search by BPNL and incomplete legal name
     *
     */
    @Test
    fun `Search by BPNL and incomplete legal name`() {

        val expected = PageDto(1, 1, 0, 1, listOf(expectedLegalEntity()))
        val searchLegalName = LegalEntityPropertiesSearchRequest("Müller", "BPNL000000000065", null, null, null, null)
        val result = poolClient.businessPartners.searchBusinessPartners(searchLegalName, setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)
    }

    /**
     * Search by fuzzy legal name
     * Müller /  Mülle /  Mü__e / Mü_e /  Mü_e / Muller
     */
    @Test
    fun `Search by fuzzy legal name`() {

        val expected = PageDto(1, 1, 0, 1, listOf(expectedLegalEntity()))
        val expectedEmpty = PageDto(0, 0, 0, 0,emptyList<BusinessPartnerSearchResultDto>())

        var request = LegalEntityPropertiesSearchRequest("Müller", null, null, null, null, null)
        var result = poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)

        request = LegalEntityPropertiesSearchRequest("Mülle", null, null, null, null, null)
        result = poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)

        request = LegalEntityPropertiesSearchRequest("Mü__e", null, null, null, null, null)
        result = poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)

        request = LegalEntityPropertiesSearchRequest("Mü_e", null, null, null, null, null)
        result = poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expectedEmpty, result)

        /* expected empty result */

        request = LegalEntityPropertiesSearchRequest("Mü_e", null, null, null, null, null)
        result = poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expectedEmpty, result)

        request = LegalEntityPropertiesSearchRequest("Muller", null, null, null, null, null)
        result = poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expectedEmpty, result)
    }

    /**
     * Search by fuzzy
     * Street / Str / St__r
     */
    @Test
    fun `Search site by fuzzy street_1`() {

        val expectedMultipleResults = PageDto(1,1,0,1,listOf(expectedSite()))
        val request = LegalEntityPropertiesSearchRequest(null, "BPNL000000000065", "Barenyi", null, null, null)
        val result = poolClient.businessPartners.searchBusinessPartners(request,setOf(BusinessPartnerSearchFilterType.IncludeSites),PaginationRequest(0,100))
        Assertions.assertEquals(expectedMultipleResults, result)
    }

    @Test
    fun `Search Site by fuzzy street_2`() {

        val expectedMultipleResults = PageDto(1,1,0,1,listOf(expectedSite()))
        val request = LegalEntityPropertiesSearchRequest(null, "BPNL000000000065", "Bela", null, null, null)
        val result = poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.IncludeSites),PaginationRequest(0,100))
        Assertions.assertEquals(expectedMultipleResults, result)
    }

    @Test
    fun `Search by fuzzy street_3`() {

        val expectedEmptyResult = PageDto(0,0,0,0,emptyList<BusinessPartnerSearchResultDto>())
        val request = LegalEntityPropertiesSearchRequest(null, "BPNL000000000065","St__r" , null, null, null)
        val result = poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expectedEmptyResult, result)
    }

    /**
     * Legal name search support German umlauts
     *
     */
    @Test
    fun `Legal name search support German umlauts`() {

        val expected = PageDto(1, 1, 0, 1,listOf(expectedLegalEntity()))
        val request = LegalEntityPropertiesSearchRequest("Muelle", null, null, null, null, null)
        val result = poolClient.businessPartners.searchBusinessPartners(request,setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)
    }

    /**
     * City search support German umlauts and fuzzy
     *
     */
    @Test
    fun `City search support German umlauts and fuzzy`() {

        val expected = PageDto(1, 1, 0, 1, listOf(expectedLegalEntity()))
        val expectedEmpty = PageDto(0,0,0,0, emptyList<BusinessPartnerSearchResultDto>())
        var request= LegalEntityPropertiesSearchRequest(null, "BPNL000000000065", null, null, "Boe", null)
        var result = poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)

        request = LegalEntityPropertiesSearchRequest(null, "BPNL000000000065", null, null, "Boblingen", null)
        result = poolClient.businessPartners.searchBusinessPartners(request,
            setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expectedEmpty, result)

        request = LegalEntityPropertiesSearchRequest(null, "BPNL000000000065", null, null, "Bö_lingen", null)
        result = poolClient.businessPartners.searchBusinessPartners(request,setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)

        request = LegalEntityPropertiesSearchRequest(null, "BPNL000000000065", null, null, "B__lingen", null)
        result = poolClient.businessPartners.searchBusinessPartners(request,setOf(BusinessPartnerSearchFilterType.IncludeLegalEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)

    }


    /**
     * Search by BPNS return the site
     *
     */
    @Test
    fun `Search by BPNS return the site`() {

        val expected = PageDto(1, 1, 0, 1,listOf(expectedSite()))
        val request = LegalEntityPropertiesSearchRequest(null, "BPNS0000000000WN", null, null, null, null)
        val result = poolClient.businessPartners.searchBusinessPartners(request,
            setOf(BusinessPartnerSearchFilterType.IncludeSites), PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)

    }

    /**
     * Search BPNA return the address
     *
     */
    @Test
    fun `Search BPNA return the address`() {

        dbTestHelpers.truncateDbTables()
        testDataEnvironment = dataHelper.createTestDataEnvironment()
        val givenStructure =testHelpers.createBusinessPartnerStructure(listOf(partnerStructure2))

        val parentBpn = givenStructure.firstOrNull()!!.legalEntity.legalEntity.bpnl
        val addressToCreate = with(BusinessPartnerNonVerboseValues.addressPartnerCreate1) {
            copy(bpnParent = parentBpn)
        }

        poolClient.addresses.createAddresses(listOf(addressToCreate))

        val expected = PageDto(1, 1, 0, 1,listOf(expectAdditionalAddress()))
        val request = LegalEntityPropertiesSearchRequest(null, "BPNL000000000065", null, null, null, null)
        val result = poolClient.businessPartners.searchBusinessPartners(request,setOf(BusinessPartnerSearchFilterType.IncludeAdditionalAddresses), PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)
    }

    private fun expectedLegalEntity():BusinessPartnerSearchResultDto {

        val identifiers = mutableListOf<BusinessPartnerIdentifierDto>()
        val identifier = BusinessPartnerIdentifierDto(
            type = BusinessPartnerNonVerboseValues.identifier3.type,
            value = BusinessPartnerNonVerboseValues.identifier3.value,
            issuingBody = BusinessPartnerNonVerboseValues.identifier3.issuingBody
        );
        identifiers.add(identifier);

        return BusinessPartnerSearchResultDto(
            identifiers = identifiers,
            isParticipantData = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.isParticipantData,
            legalEntity = BusinessPartnerLegalEntity(
                legalEntityBpn = "BPNL000000000065",
                legalName = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.legalName,
                legalForm = BusinessPartnerVerboseValues.legalForm3.name,
                confidenceCriteria = BusinessPartnerConfidenceCriteriaDto(
                    sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.sharedByOwner,
                    checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.checkedByExternalDataSource,
                    numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.numberOfSharingMembers,
                    lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.lastConfidenceCheckAt,
                    nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.nextConfidenceCheckAt,
                    confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.confidenceLevel
                )
            ),
            site = null,
            address = BusinessPartnerPostalAddress(
                addressBpn = "BPNA00000000009W",
                addressType = AddressType.LegalAddress,
                name = null,
                physicalPostalAddress =   PhysicalPostalAddressDto(
                    street = StreetDto(
                        name = BusinessPartnerVerboseValues.address1.street?.name,
                        houseNumber = BusinessPartnerVerboseValues.address1.street?.houseNumber,
                        namePrefix = BusinessPartnerVerboseValues.address1.street?.namePrefix,
                        additionalNamePrefix = BusinessPartnerVerboseValues.address1.street?.additionalNamePrefix,
                        additionalNameSuffix = BusinessPartnerVerboseValues.address1.street?.additionalNameSuffix,
                        milestone = BusinessPartnerVerboseValues.address1.street?.milestone,
                        direction = BusinessPartnerVerboseValues.address1.street?.direction,
                        houseNumberSupplement = BusinessPartnerVerboseValues.address1.street?.houseNumberSupplement,
                        nameSuffix = BusinessPartnerVerboseValues.address1.street?.nameSuffix
                    ),
                    postalCode = BusinessPartnerVerboseValues.address1.postalCode,
                    city = BusinessPartnerVerboseValues.address1.city,
                    country = BusinessPartnerVerboseValues.address1.country,
                    administrativeAreaLevel1 = BusinessPartnerVerboseValues.address1.administrativeAreaLevel1,
                    administrativeAreaLevel2= BusinessPartnerVerboseValues.address1.administrativeAreaLevel2,
                    administrativeAreaLevel3 = BusinessPartnerVerboseValues.address1.administrativeAreaLevel3,
                    district = BusinessPartnerVerboseValues.address1.district,
                    companyPostalCode = BusinessPartnerVerboseValues.address1.companyPostalCode,
                    industrialZone = BusinessPartnerVerboseValues.address1.industrialZone,
                    building = BusinessPartnerVerboseValues.address1.building,
                    floor = BusinessPartnerVerboseValues.address1.floor,
                    door = BusinessPartnerVerboseValues.address1.door,
                    taxJurisdictionCode = BusinessPartnerVerboseValues.address1.taxJurisdictionCode
                ),
                alternativePostalAddress = AlternativePostalAddressDto(
                    geographicCoordinates = null,
                    country = null,
                    administrativeAreaLevel1 = null,
                    postalCode = null, city = null,
                    deliveryServiceType = null,
                    deliveryServiceQualifier = null,
                    deliveryServiceNumber = null
                ),
                confidenceCriteria = BusinessPartnerConfidenceCriteriaDto (
                    sharedByOwner = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.sharedByOwner,
                    checkedByExternalDataSource = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.checkedByExternalDataSource,
                    numberOfSharingMembers = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.numberOfSharingMembers,
                    lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.lastConfidenceCheckAt,
                    nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.nextConfidenceCheckAt,
                    confidenceLevel = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.confidenceLevel
                ),
                states = emptyList()
            )
        )
    }

    private fun expectedSite():BusinessPartnerSearchResultDto {

        val identifiers = mutableListOf<BusinessPartnerIdentifierDto>()
        val identifier = BusinessPartnerIdentifierDto(
            type = BusinessPartnerNonVerboseValues.identifier3.type,
            value = BusinessPartnerNonVerboseValues.identifier3.value,
            issuingBody = BusinessPartnerNonVerboseValues.identifier3.issuingBody
        );
        identifiers.add(identifier)

        return BusinessPartnerSearchResultDto(
            identifiers = identifiers,
            isParticipantData = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.isParticipantData,
            legalEntity = BusinessPartnerLegalEntity(
                legalEntityBpn = "BPNL000000000065",
                legalName = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.legalName,
                legalForm = BusinessPartnerVerboseValues.legalForm3.name,
                confidenceCriteria = BusinessPartnerConfidenceCriteriaDto (
                    sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.sharedByOwner,
                    checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.checkedByExternalDataSource,
                    numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.numberOfSharingMembers,
                    lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.lastConfidenceCheckAt,
                    nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.nextConfidenceCheckAt,
                    confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.confidenceLevel
                )
            ),
            site = BusinessPartnerSite(
                siteBpn = "BPNS0000000000WN",
                name = "Stammwerk A",
                confidenceCriteria = BusinessPartnerConfidenceCriteriaDto (
                    sharedByOwner = BusinessPartnerVerboseValues.site1.confidenceCriteria.sharedByOwner,
                    checkedByExternalDataSource = BusinessPartnerVerboseValues.site1.confidenceCriteria.checkedByExternalDataSource,
                    numberOfSharingMembers = BusinessPartnerVerboseValues.site1.confidenceCriteria.numberOfSharingMembers,
                    lastConfidenceCheckAt = BusinessPartnerVerboseValues.site1.confidenceCriteria.lastConfidenceCheckAt,
                    nextConfidenceCheckAt = BusinessPartnerVerboseValues.site1.confidenceCriteria.nextConfidenceCheckAt,
                    confidenceLevel = BusinessPartnerVerboseValues.site1.confidenceCriteria.confidenceLevel
                )
            ),
            address = BusinessPartnerPostalAddress(
                addressBpn = "BPNA000000000197",
                addressType = AddressType.SiteMainAddress,
                name = null,
                physicalPostalAddress = PhysicalPostalAddressDto(
                    street = StreetDto(
                        name = BusinessPartnerVerboseValues.address1.street?.name,
                        houseNumber = BusinessPartnerVerboseValues.address1.street?.houseNumber,
                        namePrefix = BusinessPartnerVerboseValues.address1.street?.namePrefix,
                        additionalNamePrefix =BusinessPartnerVerboseValues.address1.street?.additionalNamePrefix,
                        additionalNameSuffix = BusinessPartnerVerboseValues.address1.street?.additionalNameSuffix,
                        milestone = BusinessPartnerVerboseValues.address1.street?.milestone,
                        direction = BusinessPartnerVerboseValues.address1.street?.direction,
                        houseNumberSupplement = BusinessPartnerVerboseValues.address1.street?.houseNumberSupplement,
                        nameSuffix = BusinessPartnerVerboseValues.address1.street?.nameSuffix
                    ),
                    postalCode = BusinessPartnerVerboseValues.address1.postalCode,
                    city = BusinessPartnerVerboseValues.address1.city,
                    country = BusinessPartnerVerboseValues.address1.country,
                    administrativeAreaLevel1 = BusinessPartnerVerboseValues.address1.administrativeAreaLevel1,
                    administrativeAreaLevel2= BusinessPartnerVerboseValues.address1.administrativeAreaLevel2,
                    administrativeAreaLevel3 = BusinessPartnerVerboseValues.address1.administrativeAreaLevel3,
                    district = BusinessPartnerVerboseValues.address1.district,
                    companyPostalCode = BusinessPartnerVerboseValues.address1.companyPostalCode,
                    industrialZone = BusinessPartnerVerboseValues.address1.industrialZone,
                    building = BusinessPartnerVerboseValues.address1.building,
                    floor = BusinessPartnerVerboseValues.address1.floor,
                    door = BusinessPartnerVerboseValues.address1.door,
                    taxJurisdictionCode = BusinessPartnerVerboseValues.address1.taxJurisdictionCode
                ),
                alternativePostalAddress = AlternativePostalAddressDto(
                    geographicCoordinates = null,
                    country = null,
                    administrativeAreaLevel1 = null,
                    postalCode = null, city = null,
                    deliveryServiceType = null,
                    deliveryServiceQualifier = null,
                    deliveryServiceNumber = null
                ),
                confidenceCriteria = BusinessPartnerConfidenceCriteriaDto (
                    sharedByOwner = BusinessPartnerVerboseValues.addressPartner1.confidenceCriteria.sharedByOwner,
                    checkedByExternalDataSource = BusinessPartnerVerboseValues.addressPartner1.confidenceCriteria.checkedByExternalDataSource,
                    numberOfSharingMembers = BusinessPartnerVerboseValues.addressPartner1.confidenceCriteria.numberOfSharingMembers,
                    lastConfidenceCheckAt = BusinessPartnerVerboseValues.addressPartner1.confidenceCriteria.lastConfidenceCheckAt,
                    nextConfidenceCheckAt = BusinessPartnerVerboseValues.addressPartner1.confidenceCriteria.nextConfidenceCheckAt,
                    confidenceLevel = BusinessPartnerVerboseValues.addressPartner1.confidenceCriteria.confidenceLevel
                ),
                states = emptyList()
            )
        )
    }

    private fun expectAdditionalAddress(): BusinessPartnerSearchResultDto {
        val identifiers = mutableListOf<BusinessPartnerIdentifierDto>()
        val identifier = BusinessPartnerIdentifierDto(
            type = BusinessPartnerNonVerboseValues.identifier3.type,
            value = BusinessPartnerNonVerboseValues.identifier3.value,
            issuingBody = BusinessPartnerNonVerboseValues.identifier3.issuingBody
        );
        identifiers.add(identifier)

        return BusinessPartnerSearchResultDto(
            identifiers = identifiers,
            isParticipantData = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.isParticipantData,
            legalEntity = BusinessPartnerLegalEntity(
                legalEntityBpn = "BPNL000000000065",
                legalName = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.legalName,
                legalForm = BusinessPartnerVerboseValues.legalForm3.name,
                confidenceCriteria = BusinessPartnerConfidenceCriteriaDto (
                    sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.sharedByOwner,
                    checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.checkedByExternalDataSource,
                    numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.numberOfSharingMembers,
                    lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.lastConfidenceCheckAt,
                    nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.nextConfidenceCheckAt,
                    confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.confidenceLevel
                )
            ),
            site = null,
            address = BusinessPartnerPostalAddress(
                addressBpn = "BPNA000000000197",
                addressType = AddressType.AdditionalAddress,
                name = null,
                physicalPostalAddress = PhysicalPostalAddressDto(
                    street = StreetDto(
                        name = BusinessPartnerNonVerboseValues.logisticAddress4.physicalPostalAddress.street?.name,
                        houseNumber = BusinessPartnerNonVerboseValues.logisticAddress4.physicalPostalAddress.street?.houseNumber,
                        namePrefix = BusinessPartnerNonVerboseValues.logisticAddress4.physicalPostalAddress.street?.namePrefix,
                        additionalNamePrefix = BusinessPartnerNonVerboseValues.logisticAddress4.physicalPostalAddress.street?.additionalNamePrefix,
                        additionalNameSuffix = BusinessPartnerNonVerboseValues.logisticAddress4.physicalPostalAddress.street?.additionalNameSuffix,
                        milestone = BusinessPartnerNonVerboseValues.logisticAddress4.physicalPostalAddress.street?.milestone,
                        direction = BusinessPartnerNonVerboseValues.logisticAddress4.physicalPostalAddress.street?.direction,
                        houseNumberSupplement = BusinessPartnerNonVerboseValues.logisticAddress4.physicalPostalAddress.street?.houseNumberSupplement,
                        nameSuffix = BusinessPartnerNonVerboseValues.logisticAddress4.physicalPostalAddress.street?.nameSuffix
                    ),
                    postalCode = BusinessPartnerNonVerboseValues.logisticAddress4.physicalPostalAddress.postalCode,
                    city = BusinessPartnerNonVerboseValues.logisticAddress4.physicalPostalAddress.city,
                    country = BusinessPartnerNonVerboseValues.logisticAddress4.physicalPostalAddress.country,
                    administrativeAreaLevel1 = BusinessPartnerVerboseValues.address1.administrativeAreaLevel1,
                    administrativeAreaLevel2= BusinessPartnerVerboseValues.address1.administrativeAreaLevel2,
                    administrativeAreaLevel3 = BusinessPartnerVerboseValues.address1.administrativeAreaLevel3,
                    district = BusinessPartnerVerboseValues.address1.district,
                    companyPostalCode = BusinessPartnerVerboseValues.address1.companyPostalCode,
                    industrialZone = BusinessPartnerVerboseValues.address1.industrialZone,
                    building = BusinessPartnerVerboseValues.address1.building,
                    floor = BusinessPartnerVerboseValues.address1.floor,
                    door = BusinessPartnerVerboseValues.address1.door,
                    taxJurisdictionCode = BusinessPartnerVerboseValues.address1.taxJurisdictionCode
                ),
                alternativePostalAddress = AlternativePostalAddressDto(
                    geographicCoordinates = null,
                    country = null,
                    administrativeAreaLevel1 = null,
                    postalCode = null, city = null,
                    deliveryServiceType = null,
                    deliveryServiceQualifier = null,
                    deliveryServiceNumber = null
                ),
                confidenceCriteria = BusinessPartnerConfidenceCriteriaDto (
                    sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalAddress.confidenceCriteria.sharedByOwner,
                    checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalAddress.confidenceCriteria.checkedByExternalDataSource,
                    numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalAddress.confidenceCriteria.numberOfSharingMembers,
                    lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalAddress.confidenceCriteria.lastConfidenceCheckAt,
                    nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalAddress.confidenceCriteria.nextConfidenceCheckAt,
                    confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalAddress.confidenceCriteria.confidenceLevel
                ),
                states = emptyList()
            )
        )
    }


}