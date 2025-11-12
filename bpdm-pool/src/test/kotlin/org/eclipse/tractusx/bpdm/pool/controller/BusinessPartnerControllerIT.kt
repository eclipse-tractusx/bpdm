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

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerIdentifierDto
import org.eclipse.tractusx.bpdm.gate.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.gate.api.model.PhysicalPostalAddressDto
import org.eclipse.tractusx.bpdm.gate.api.model.StreetDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressComponentOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationOutputDto
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.pool.api.model.BusinessPartnerSearchFilterType
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.BusinessPartnerSearchResultDto
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

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

    private lateinit var givenPartner1: LegalEntityVerboseDto
    private lateinit var legalAddress1: LogisticAddressVerboseDto

    @BeforeEach
    fun beforeEach() {
        dbTestHelpers.truncateDbTables()
        testDataEnvironment = dataHelper.createTestDataEnvironment()
        val givenStructure = testHelpers.createBusinessPartnerStructure(listOf(partnerStructure1))
        givenPartner1 = with(givenStructure[0].legalEntity) { legalEntity }
        legalAddress1 = givenStructure[0].legalEntity.legalAddress
    }

    /**
     * Given null request
     * Return 200 and empty and empty content
     */
    @Test
    fun `Request parameters are all null should return 200 and empty content`() {

        val request = LegalEntityPropertiesSearchRequest(null, null, null, null, null, null)
        val expect = PageDto(0,0,0,0,emptyList<BusinessPartnerSearchResultDto>())
        val result = poolClient.businessPartners.searchBusinessPartners(request,  setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities),  PaginationRequest(0, 100))
        Assertions.assertEquals(expect,result )
    }

    /**
     * Given empty string request
     * Return 200 and empty content
     */
    @Test
    fun `Request parameters are all empty string should return 200 and empty content`() {

        val request = LegalEntityPropertiesSearchRequest("", "", "", "", "", "")
        val expect = PageDto(0,0,0,0,emptyList<BusinessPartnerSearchResultDto>())
        val result = poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities), PaginationRequest(0, 100))
        Assertions.assertEquals(expect, result)
    }

    /**
     * Search by BPNL
     *
     */
    @Test
    fun `Search by BPNL`() {

        val identifiers = mutableListOf<BusinessPartnerIdentifierDto>()
        val identifier = BusinessPartnerIdentifierDto(
            type = BusinessPartnerNonVerboseValues.identifier3.type,
            value = BusinessPartnerNonVerboseValues.identifier3.value,
            issuingBody = BusinessPartnerNonVerboseValues.identifier3.issuingBody
        );
        identifiers.add(identifier)

        val expected = PageDto(
            1, 1, 0, 100,
            listOf(
                BusinessPartnerSearchResultDto(
                    identifiers = identifiers,
                    isParticipantData = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.isParticipantData,
                    legalEntity = LegalEntityRepresentationOutputDto(
                        legalEntityBpn = "BPNL000000000065",
                        legalName = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.legalName,
                        legalForm = BusinessPartnerVerboseValues.legalForm3.name,
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.confidenceLevel
                        )
                    ),
                    site = null,
                    address = AddressComponentOutputDto(
                        addressBpn = "BPNA00000000009W",
                        addressType = AddressType.LegalAddress,
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
                            country = BusinessPartnerVerboseValues.address1.country
                        ),
                        alternativePostalAddress = null,
                        confidenceCriteria = ConfidenceCriteriaDto (
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
            )
        )
        val searchLegalName = LegalEntityPropertiesSearchRequest(null, "BPNL000000000065", null, null, null, null)
        var result = poolClient.businessPartners.searchBusinessPartners(searchLegalName, setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities),PaginationRequest(0,100))

        Assertions.assertEquals(expected, result)
    }

    /**
     * Search by incorrect BPNL format
     *
     */
    @Test
    fun `Search by incorrect BPNL format`() {

        val identifiers = mutableListOf<BusinessPartnerIdentifierDto>()
        val identifier = BusinessPartnerIdentifierDto(
            type = BusinessPartnerNonVerboseValues.identifier3.type,
            value = BusinessPartnerNonVerboseValues.identifier3.value,
            issuingBody = BusinessPartnerNonVerboseValues.identifier3.issuingBody
        );
        identifiers.add(identifier)

        val expected = PageDto(0, 0, 0, 100,emptyList<BusinessPartnerSearchResultDto>())
        val searchLegalName = LegalEntityPropertiesSearchRequest(null, "BPNL000065", null, null, null, null)
        val result = poolClient.businessPartners.searchBusinessPartners(searchLegalName,
            setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities),PaginationRequest(0,100))

        Assertions.assertEquals(expected, result)
    }

    /**
     * Search by BPNL and incomplete legal name
     *
     */
    @Test
    fun `Search by BPNL and incomplete legal name`() {

        val identifiers = mutableListOf<BusinessPartnerIdentifierDto>()
        val identifier = BusinessPartnerIdentifierDto(
            type = BusinessPartnerNonVerboseValues.identifier3.type,
            value = BusinessPartnerNonVerboseValues.identifier3.value,
            issuingBody = BusinessPartnerNonVerboseValues.identifier3.issuingBody
        );
        identifiers.add(identifier)

        val expected = PageDto(
            1, 1, 0, 100,
            listOf(
                BusinessPartnerSearchResultDto(
                    identifiers = identifiers,
                    isParticipantData = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.isParticipantData,
                    legalEntity = LegalEntityRepresentationOutputDto(
                        legalEntityBpn = "BPNL000000000065",
                        legalName = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.legalName,
                        legalForm = BusinessPartnerVerboseValues.legalForm3.name,
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.confidenceLevel
                        )
                    ),
                    site = null,
                    address = AddressComponentOutputDto(
                        addressBpn = "BPNA00000000009W",
                        addressType = AddressType.LegalAddress,
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
                            country = BusinessPartnerVerboseValues.address1.country
                        ),
                        alternativePostalAddress = null,
                        confidenceCriteria = ConfidenceCriteriaDto (
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
            )
        )
        val searchLegalName = LegalEntityPropertiesSearchRequest("Müller", "BPNL000000000065", null, null, null, null)
        val result = poolClient.businessPartners.searchBusinessPartners(searchLegalName, setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities),PaginationRequest(0,100))

        Assertions.assertEquals(expected, result)
    }

    /**
     * Search by fuzzy legal name
     * Müller /  Mülle /  Mü__e / Mü_e /  Mü_e / Muller
     */
    @Test
    fun `Search by fuzzy legal name`() {

        val identifiers = mutableListOf<BusinessPartnerIdentifierDto>()
        val identifier = BusinessPartnerIdentifierDto(
            type = BusinessPartnerNonVerboseValues.identifier3.type,
            value = BusinessPartnerNonVerboseValues.identifier3.value,
            issuingBody = BusinessPartnerNonVerboseValues.identifier3.issuingBody
        );
        identifiers.add(identifier)

        val expected = PageDto(
            1, 1, 0, 100,
            listOf(
                BusinessPartnerSearchResultDto(
                    identifiers = identifiers,
                    isParticipantData = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.isParticipantData,
                    legalEntity = LegalEntityRepresentationOutputDto(
                        legalEntityBpn = "BPNL000000000065",
                        legalName = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.legalName,
                        legalForm = BusinessPartnerVerboseValues.legalForm3.name,
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.confidenceLevel
                        )
                    ),
                    site = null,
                    address = AddressComponentOutputDto(
                        addressBpn = "BPNA00000000009W",
                        addressType = AddressType.LegalAddress,
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
                            country = BusinessPartnerVerboseValues.address1.country
                        ),
                        alternativePostalAddress = null,
                        confidenceCriteria = ConfidenceCriteriaDto (
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
            )
        )

        val expectedEmpty = PageDto(0, 0, 0, 100,emptyList<BusinessPartnerSearchResultDto>())

        var request = LegalEntityPropertiesSearchRequest("Müller", null, null, null, null, null)
        var result = poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)

        request = LegalEntityPropertiesSearchRequest("Mülle", null, null, null, null, null)
        result = poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)

        request = LegalEntityPropertiesSearchRequest("Mü__e", null, null, null, null, null)
        result = poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)

        request = LegalEntityPropertiesSearchRequest("Mü_e", null, null, null, null, null)
        result = poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expectedEmpty, result)

        /* expected empty result */

        request = LegalEntityPropertiesSearchRequest("Mü_e", null, null, null, null, null)
        result = poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expectedEmpty, result)

        request = LegalEntityPropertiesSearchRequest("Muller", null, null, null, null, null)
        result = poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expectedEmpty, result)
    }

    /**
     * Search by fuzzy
     * Street / Str / St__r
     */
    @Test
    fun `Search by fuzzy street_1`() {

        val identifiers = mutableListOf<BusinessPartnerIdentifierDto>()
        val identifier = BusinessPartnerIdentifierDto(
            type = BusinessPartnerNonVerboseValues.identifier3.type,
            value = BusinessPartnerNonVerboseValues.identifier3.value,
            issuingBody = BusinessPartnerNonVerboseValues.identifier3.issuingBody
        );
        identifiers.add(identifier)

        val expectedMultipleResults = PageDto(
            2,1,0,100,
            listOf(
                BusinessPartnerSearchResultDto(
                    identifiers = identifiers,
                    isParticipantData = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.isParticipantData,
                    legalEntity = LegalEntityRepresentationOutputDto(
                        legalEntityBpn = "BPNL000000000065",
                        legalName = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.legalName,
                        legalForm = BusinessPartnerVerboseValues.legalForm3.name,
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.confidenceLevel
                        )
                    ),
                    site = null,
                    address = AddressComponentOutputDto(
                        addressBpn = "BPNA00000000009W",
                        addressType = AddressType.LegalAddress,
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
                            country = BusinessPartnerVerboseValues.address1.country
                        ),
                        alternativePostalAddress = null,
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.confidenceLevel
                        ),
                        states = emptyList()
                    )
                ),
                /*Site content*/
                BusinessPartnerSearchResultDto(
                    identifiers = identifiers,
                    isParticipantData = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.isParticipantData,
                    legalEntity = LegalEntityRepresentationOutputDto(
                        legalEntityBpn = "BPNL000000000065",
                        legalName = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.legalName,
                        legalForm = BusinessPartnerVerboseValues.legalForm3.name,
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.confidenceLevel
                        )
                    ),
                    site = SiteRepresentationOutputDto(
                        siteBpn = "BPNS0000000000WN",
                        name = "Stammwerk A",
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerVerboseValues.site1.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerVerboseValues.site1.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerVerboseValues.site1.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerVerboseValues.site1.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerVerboseValues.site1.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerVerboseValues.site1.confidenceCriteria.confidenceLevel
                        )
                    ),
                    address = AddressComponentOutputDto(
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
                            country = BusinessPartnerVerboseValues.address1.country
                        ),
                        alternativePostalAddress = null,
                        confidenceCriteria = ConfidenceCriteriaDto (
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
            )
        )

        val request = LegalEntityPropertiesSearchRequest(null, "BPNL000000000065", "Barenyi", null, null, null)
        val result = poolClient.businessPartners.searchBusinessPartners(request, setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities,
            BusinessPartnerSearchFilterType.ShowOnlySites),PaginationRequest(0,100))
        Assertions.assertEquals(expectedMultipleResults, result)
    }

    @Test
    fun `Search by fuzzy street_2`() {

        val identifiers = mutableListOf<BusinessPartnerIdentifierDto>()
        val identifier = BusinessPartnerIdentifierDto(
            type = BusinessPartnerNonVerboseValues.identifier3.type,
            value = BusinessPartnerNonVerboseValues.identifier3.value,
            issuingBody = BusinessPartnerNonVerboseValues.identifier3.issuingBody
        );
        identifiers.add(identifier)

        val expectedMultipleResults = PageDto(
            2,1,0,100,
            listOf(
                BusinessPartnerSearchResultDto(
                    identifiers = identifiers,
                    isParticipantData = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.isParticipantData,
                    legalEntity = LegalEntityRepresentationOutputDto(
                        legalEntityBpn = "BPNL000000000065",
                        legalName = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.legalName,
                        legalForm = BusinessPartnerVerboseValues.legalForm3.name,
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.confidenceLevel
                        )
                    ),
                    site = null,
                    address = AddressComponentOutputDto(
                        addressBpn = "BPNA00000000009W",
                        addressType = AddressType.LegalAddress,
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
                            country = BusinessPartnerVerboseValues.address1.country
                        ),
                        alternativePostalAddress = null,
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerNonVerboseValues.logisticAddress4.confidenceCriteria.confidenceLevel
                        ),
                        states = emptyList()
                    )
                ),
                /*Site content*/
                BusinessPartnerSearchResultDto(
                    identifiers = identifiers,
                    isParticipantData = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.isParticipantData,
                    legalEntity = LegalEntityRepresentationOutputDto(
                        legalEntityBpn = "BPNL000000000065",
                        legalName = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.legalName,
                        legalForm = BusinessPartnerVerboseValues.legalForm3.name,
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.confidenceLevel
                        )
                    ),
                    site = SiteRepresentationOutputDto(
                        siteBpn = "BPNS0000000000WN",
                        name = "Stammwerk A",
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerVerboseValues.site1.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerVerboseValues.site1.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerVerboseValues.site1.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerVerboseValues.site1.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerVerboseValues.site1.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerVerboseValues.site1.confidenceCriteria.confidenceLevel
                        )
                    ),
                    address = AddressComponentOutputDto(
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
                            country = BusinessPartnerVerboseValues.address1.country
                        ),
                        alternativePostalAddress = null,
                        confidenceCriteria = ConfidenceCriteriaDto (
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
            )
        )

        val request = LegalEntityPropertiesSearchRequest(null, "BPNL000000000065", "Bela", null, null, null)
        val result = poolClient.businessPartners.searchBusinessPartners(request,
            setOf(
                BusinessPartnerSearchFilterType.ShowOnlyLegaEntities,
                BusinessPartnerSearchFilterType.ShowOnlySites
            ),PaginationRequest(0,100))
        Assertions.assertEquals(expectedMultipleResults, result)
    }

    @Test
    fun `Search by fuzzy street_3`() {

        val identifiers = mutableListOf<BusinessPartnerIdentifierDto>()
        val identifier = BusinessPartnerIdentifierDto(
            type = BusinessPartnerNonVerboseValues.identifier3.type,
            value = BusinessPartnerNonVerboseValues.identifier3.value,
            issuingBody = BusinessPartnerNonVerboseValues.identifier3.issuingBody
        );
        identifiers.add(identifier)

        val expectedEmptyResult = PageDto(0,0,0,100,emptyList<BusinessPartnerSearchResultDto>())

        val request = LegalEntityPropertiesSearchRequest(null, "BPNL000000000065", "St__r", null, null, null)
        val result = poolClient.businessPartners.searchBusinessPartners(request, setOf(
            BusinessPartnerSearchFilterType.ShowOnlyLegaEntities
        ),PaginationRequest(0,100))
        Assertions.assertEquals(expectedEmptyResult, result)
    }

    /**
     * Legal name search support German umlauts
     *
     */
    @Test
    fun `Legal name search support German umlauts`() {

        val identifiers = mutableListOf<BusinessPartnerIdentifierDto>()
        val identifier = BusinessPartnerIdentifierDto(
            type = BusinessPartnerNonVerboseValues.identifier3.type,
            value = BusinessPartnerNonVerboseValues.identifier3.value,
            issuingBody = BusinessPartnerNonVerboseValues.identifier3.issuingBody
        );
        identifiers.add(identifier)

        val expected = PageDto(
            1, 1, 0, 100,
            listOf(
                BusinessPartnerSearchResultDto(
                    identifiers = identifiers,
                    isParticipantData = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.isParticipantData,
                    legalEntity = LegalEntityRepresentationOutputDto(
                        legalEntityBpn = "BPNL000000000065",
                        legalName = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.legalName,
                        legalForm = BusinessPartnerVerboseValues.legalForm3.name,
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.confidenceLevel
                        )
                    ),
                    site = null,
                    address = AddressComponentOutputDto(
                        addressBpn = "BPNA00000000009W",
                        addressType = AddressType.LegalAddress,
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
                            country = BusinessPartnerVerboseValues.address1.country
                        ),
                        alternativePostalAddress = null,
                        confidenceCriteria = ConfidenceCriteriaDto (
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
            )
        )

        val request = LegalEntityPropertiesSearchRequest("Muelle", null, null, null, null, null)
        val result = poolClient.businessPartners.searchBusinessPartners(request,
            setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)
    }

    /**
     * City search support German umlauts and fuzzy
     *
     */
    @Test
    fun `City search support German umlauts and fuzzy`() {

        val identifiers = mutableListOf<BusinessPartnerIdentifierDto>()
        val identifier = BusinessPartnerIdentifierDto(
            type = BusinessPartnerNonVerboseValues.identifier3.type,
            value = BusinessPartnerNonVerboseValues.identifier3.value,
            issuingBody = BusinessPartnerNonVerboseValues.identifier3.issuingBody
        );
        identifiers.add(identifier)

        val expected = PageDto(
            1, 1, 0, 100,
            listOf(
                BusinessPartnerSearchResultDto(
                    identifiers = identifiers,
                    isParticipantData = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.isParticipantData,
                    legalEntity = LegalEntityRepresentationOutputDto(
                        legalEntityBpn = "BPNL000000000065",
                        legalName = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.legalName,
                        legalForm = BusinessPartnerVerboseValues.legalForm3.name,
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.confidenceLevel
                        )
                    ),
                    site = null,
                    address = AddressComponentOutputDto(
                        addressBpn = "BPNA00000000009W",
                        addressType = AddressType.LegalAddress,
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
                            country = BusinessPartnerVerboseValues.address1.country
                        ),
                        alternativePostalAddress = null,
                        confidenceCriteria = ConfidenceCriteriaDto (
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
            )
        )

        val expectedEmpty = PageDto(0,0,0,100, emptyList<BusinessPartnerSearchResultDto>())

        val request_Koeln = LegalEntityPropertiesSearchRequest(null, "BPNL000000000065", null, null, "Boe", null)
        var result = poolClient.businessPartners.searchBusinessPartners(request_Koeln, setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities),PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)

        val request_Koln = LegalEntityPropertiesSearchRequest(null, "BPNL000000000065", null, null, "Boblingen", null)
        result = poolClient.businessPartners.searchBusinessPartners(request_Koln,
            setOf(
                BusinessPartnerSearchFilterType.ShowOnlyLegaEntities
            ),PaginationRequest(0,100))
        Assertions.assertEquals(expectedEmpty, result)

        val request_K_ln = LegalEntityPropertiesSearchRequest(null, "BPNL000000000065", null, null, "B_blingen", null)
        result = poolClient.businessPartners.searchBusinessPartners(request_K_ln,
            setOf(
                BusinessPartnerSearchFilterType.ShowOnlyLegaEntities
            ),PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)

        val request_K__n = LegalEntityPropertiesSearchRequest(null, "BPNL000000000065", null, null, "B__lingen", null)
        result = poolClient.businessPartners.searchBusinessPartners(request_K__n,
            setOf(
                BusinessPartnerSearchFilterType.ShowOnlyLegaEntities
            ),PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)

    }


    /**
     * Search by BPNS return the site and its parents
     *
     */
    @Test
    fun `Search by BPNS return the site and its parents `() {

        val identifiers = mutableListOf<BusinessPartnerIdentifierDto>()
        val identifier = BusinessPartnerIdentifierDto(
            type = BusinessPartnerNonVerboseValues.identifier3.type,
            value = BusinessPartnerNonVerboseValues.identifier3.value,
            issuingBody = BusinessPartnerNonVerboseValues.identifier3.issuingBody
        );
        identifiers.add(identifier);

        val expected = PageDto(
            2, 1, 0, 100,
            listOf(
                /*Site content*/
                BusinessPartnerSearchResultDto(
                    identifiers = identifiers,
                    isParticipantData = false,
                    legalEntity = LegalEntityRepresentationOutputDto(
                        legalEntityBpn = "BPNL000000000065",
                        legalName = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.legalName,
                        legalForm = BusinessPartnerVerboseValues.legalForm3.name,
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.confidenceLevel
                        )
                    ),
                    site = SiteRepresentationOutputDto(
                        siteBpn = "BPNS0000000000WN",
                        name = "Stammwerk A",
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerVerboseValues.site1.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerVerboseValues.site1.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerVerboseValues.site1.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerVerboseValues.site1.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerVerboseValues.site1.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerVerboseValues.site1.confidenceCriteria.confidenceLevel
                        )
                    ),
                    address = AddressComponentOutputDto(
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
                            country = BusinessPartnerVerboseValues.address1.country
                        ),
                        alternativePostalAddress = null,
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerVerboseValues.addressPartner1.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerVerboseValues.addressPartner1.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerVerboseValues.addressPartner1.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerVerboseValues.addressPartner1.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerVerboseValues.addressPartner1.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerVerboseValues.addressPartner1.confidenceCriteria.confidenceLevel
                        ),
                        states = emptyList()
                    )
                ),
                BusinessPartnerSearchResultDto(
                    identifiers = identifiers,
                    isParticipantData = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.isParticipantData,
                    legalEntity = LegalEntityRepresentationOutputDto(
                        legalEntityBpn = "BPNL000000000065",
                        legalName = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.legalName,
                        legalForm = BusinessPartnerVerboseValues.legalForm3.name,
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.confidenceLevel
                        )
                    ),
                    site = null,
                    address = AddressComponentOutputDto(
                        addressBpn = "BPNA00000000009W",
                        addressType = AddressType.LegalAddress,
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
                            country = BusinessPartnerVerboseValues.address1.country
                        ),
                        alternativePostalAddress = null,
                        confidenceCriteria = ConfidenceCriteriaDto (
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
            )
        )

        val request = LegalEntityPropertiesSearchRequest(null, "BPNS0000000000WN", null, null, null, null)
        val result = poolClient.businessPartners.searchBusinessPartners(request,
            setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities, BusinessPartnerSearchFilterType.ShowOnlySites), PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)

    }

    /**
     * Search BPNA return the address and its parents
     *
     */
    @Test
    fun `Search BPNA return the address and its parents `() {

        val identifiers = mutableListOf<BusinessPartnerIdentifierDto>()
        val identifier = BusinessPartnerIdentifierDto(
            type = BusinessPartnerNonVerboseValues.identifier3.type,
            value = BusinessPartnerNonVerboseValues.identifier3.value,
            issuingBody = BusinessPartnerNonVerboseValues.identifier3.issuingBody
        );
        identifiers.add(identifier);

        val expected = PageDto(
            2, 1, 0, 100,
            listOf(
                BusinessPartnerSearchResultDto(
                    identifiers = identifiers,
                    isParticipantData = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.isParticipantData,
                    legalEntity = LegalEntityRepresentationOutputDto(
                        legalEntityBpn = "BPNL000000000065",
                        legalName = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.legalName,
                        legalForm = BusinessPartnerVerboseValues.legalForm3.name,
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.confidenceLevel
                        )
                    ),
                    site = null,
                    address = AddressComponentOutputDto(
                        addressBpn = "BPNA00000000009W",
                        addressType = AddressType.LegalAddress,
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
                            country = BusinessPartnerVerboseValues.address1.country
                        ),
                        alternativePostalAddress = null,
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalAddress.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalAddress.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalAddress.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalAddress.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalAddress.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalAddress.confidenceCriteria.confidenceLevel
                        ),
                        states = emptyList()
                    )
                ),
                BusinessPartnerSearchResultDto(
                    identifiers = identifiers,
                    isParticipantData = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.isParticipantData,
                    legalEntity = LegalEntityRepresentationOutputDto(
                        legalEntityBpn = "BPNL000000000065",
                        legalName = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.legalName,
                        legalForm = BusinessPartnerVerboseValues.legalForm3.name,
                        confidenceCriteria = ConfidenceCriteriaDto (
                            sharedByOwner = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.sharedByOwner,
                            checkedByExternalDataSource = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.checkedByExternalDataSource,
                            numberOfSharingMembers = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.numberOfSharingMembers,
                            lastConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.lastConfidenceCheckAt,
                            nextConfidenceCheckAt = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.nextConfidenceCheckAt,
                            confidenceLevel = BusinessPartnerNonVerboseValues.legalEntityCreate4.legalEntity.confidenceCriteria.confidenceLevel
                        )
                    ),
                    site = null,
                    address = AddressComponentOutputDto(
                        addressBpn = "BPNA00000000009W",
                        addressType = AddressType.LegalAddress,
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
                            country = BusinessPartnerVerboseValues.address1.country
                        ),
                        alternativePostalAddress = null,
                        confidenceCriteria = ConfidenceCriteriaDto (
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
            )
        )

        val request = LegalEntityPropertiesSearchRequest(null, "BPNA00000000009W", null, null, null, null)
        val result = poolClient.businessPartners.searchBusinessPartners(request,
            setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities,
                BusinessPartnerSearchFilterType.ShowOnlyAdditionalAddresses), PaginationRequest(0,100))
        Assertions.assertEquals(expected, result)
    }


}