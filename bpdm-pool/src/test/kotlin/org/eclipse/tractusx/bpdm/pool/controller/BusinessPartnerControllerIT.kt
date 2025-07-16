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

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.pool.api.model.BusinessPartnerSearchFilterType
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityIdentifierDto
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
import org.eclipse.tractusx.bpdm.test.util.PoolDataHelpers
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
    val poolDataHelpers: PoolDataHelpers
) {

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
        poolDataHelpers.createPoolMetadata()
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

        var identifiers = mutableListOf<LegalEntityIdentifierDto>()
        identifiers.add(BusinessPartnerNonVerboseValues.identifier3)

        val expected = PageDto(
            1, 1, 0, 100,
            listOf(
                BusinessPartnerSearchResultDto(
                    id = "BPNL000000000065",
                    name = "Müller Handels GmbH & Co. KG",
                    legalForm = BusinessPartnerVerboseValues.legalForm3,
                    street = BusinessPartnerVerboseValues.address1.street,
                    city = "Böblingen",
                    postalCode ="71059",
                    country = "DE",
                    identifiers =  identifiers
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

        var identifiers = mutableListOf<LegalEntityIdentifierDto>()
        identifiers.add(BusinessPartnerNonVerboseValues.identifier3)

        val expected = PageDto(
            0, 0, 0, 100,
            emptyList<BusinessPartnerSearchResultDto>()
        )
        val searchLegalName = LegalEntityPropertiesSearchRequest(null, "BPNL000065", null, null, null, null)
        var result = poolClient.businessPartners.searchBusinessPartners(searchLegalName,
            setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities),PaginationRequest(0,100))

        Assertions.assertEquals(expected, result)
    }

    /**
     * Search by BPNL and incomplete legal name
     *
     */
    @Test
    fun `Search by BPNL and incomplete legal name`() {

        var identifiers = mutableListOf<LegalEntityIdentifierDto>()
        identifiers.add(BusinessPartnerNonVerboseValues.identifier3)

        val expected = PageDto(
            1, 1, 0, 100,
            listOf(
                BusinessPartnerSearchResultDto(
                    id = "BPNL000000000065",
                    name = "Müller Handels GmbH & Co. KG",
                    legalForm = BusinessPartnerVerboseValues.legalForm3,
                    street = BusinessPartnerVerboseValues.address1.street,
                    city = "Böblingen",
                    postalCode ="71059",
                    country = "DE",
                    identifiers =  identifiers
                )
            )
        )
        val searchLegalName = LegalEntityPropertiesSearchRequest("Müller", "BPNL000000000065", null, null, null, null)
        var result = poolClient.businessPartners.searchBusinessPartners(searchLegalName, setOf(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities),PaginationRequest(0,100))

        Assertions.assertEquals(expected, result)
    }

    /**
     * Search by fuzzy legal name
     * Müller /  Mülle /  Mü__e / Mü_e /  Mü_e / Muller
     */
    @Test
    fun `Search by fuzzy legal name`() {

        var identifiers = mutableListOf<LegalEntityIdentifierDto>()
        identifiers.add(BusinessPartnerNonVerboseValues.identifier3)

        val expected = PageDto(
            1, 1, 0, 100,
            listOf(
                BusinessPartnerSearchResultDto(
                    id = "BPNL000000000065",
                    name = "Müller Handels GmbH & Co. KG",
                    legalForm = BusinessPartnerVerboseValues.legalForm3,
                    street = BusinessPartnerVerboseValues.address1.street,
                    city = "Böblingen",
                    postalCode ="71059",
                    country = "DE",
                    identifiers =  identifiers
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
     * Search by fuzzy street
     * Street / Str / St__r
     */
    @Test
    fun `Search by fuzzy street_1`() {

        var identifiers = mutableListOf<LegalEntityIdentifierDto>()
        identifiers.add(BusinessPartnerNonVerboseValues.identifier3)

        val expectedMultipleResults = PageDto(
            2,1,0,100,
            listOf(
                BusinessPartnerSearchResultDto(
                    id = "BPNL000000000065",
                    name = "Müller Handels GmbH & Co. KG",
                    legalForm = BusinessPartnerVerboseValues.legalForm3,
                    street = BusinessPartnerVerboseValues.address1.street,
                    city = "Böblingen",
                    postalCode ="71059",
                    country = "DE",
                    identifiers =  identifiers
                ),
                BusinessPartnerSearchResultDto(
                    id = "BPNS0000000000WN",
                    name = "Stammwerk A",
                    legalForm = null,
                    street = BusinessPartnerVerboseValues.address1.street,
                    city = "Böblingen",
                    postalCode ="71059",
                    country = "DE",
                    identifiers =  emptyList()
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

        var identifiers = mutableListOf<LegalEntityIdentifierDto>()
        identifiers.add(BusinessPartnerNonVerboseValues.identifier3)

        val expectedMultipleResults = PageDto(
            2,1,0,100,
            listOf(
                BusinessPartnerSearchResultDto(
                    id = "BPNL000000000065",
                    name = "Müller Handels GmbH & Co. KG",
                    legalForm = BusinessPartnerVerboseValues.legalForm3,
                    street = BusinessPartnerVerboseValues.address1.street,
                    city = "Böblingen",
                    postalCode ="71059",
                    country = "DE",
                    identifiers =  identifiers
                ),
                BusinessPartnerSearchResultDto(
                    id = "BPNS0000000000WN",
                    name = "Stammwerk A",
                    legalForm = null,
                    street = BusinessPartnerVerboseValues.address1.street,
                    city = "Böblingen",
                    postalCode ="71059",
                    country = "DE",
                    identifiers =  emptyList()
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

        var identifiers = mutableListOf<LegalEntityIdentifierDto>()
        identifiers.add(BusinessPartnerNonVerboseValues.identifier3)

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

        val identifiers = mutableListOf<LegalEntityIdentifierDto>()
        identifiers.add(BusinessPartnerNonVerboseValues.identifier3)

        val expected = PageDto(
            1, 1, 0, 100,
            listOf(
                BusinessPartnerSearchResultDto(
                    id = "BPNL000000000065",
                    name = "Müller Handels GmbH & Co. KG",
                    legalForm = BusinessPartnerVerboseValues.legalForm3,
                    street = BusinessPartnerVerboseValues.address1.street,
                    city = "Böblingen",
                    postalCode ="71059",
                    country = "DE",
                    identifiers =  identifiers
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

        var identifiers = mutableListOf<LegalEntityIdentifierDto>()
        identifiers.add(BusinessPartnerNonVerboseValues.identifier3)

        val expected = PageDto(
            1, 1, 0, 100,
            listOf(
                BusinessPartnerSearchResultDto(
                    id = "BPNL000000000065",
                    name = "Müller Handels GmbH & Co. KG",
                    legalForm = BusinessPartnerVerboseValues.legalForm3,
                    street = BusinessPartnerVerboseValues.address1.street,
                    city = "Böblingen",
                    postalCode ="71059",
                    country = "DE",
                    identifiers =  identifiers
                )
            )
        )

        val expectedEmpty = PageDto(0,0,0,100, emptyList<BusinessPartnerSearchResultDto>())

        val request_Koeln = LegalEntityPropertiesSearchRequest(null, "BPNL000000000065", null, null, "Boe", null)
        var result = poolClient.businessPartners.searchBusinessPartners(request_Koeln, setOf(
            BusinessPartnerSearchFilterType.ShowOnlyLegaEntities
        ),PaginationRequest(0,100))
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

        var identifiers = mutableListOf<LegalEntityIdentifierDto>()
        identifiers.add(BusinessPartnerNonVerboseValues.identifier3)

        val expected = PageDto(
            2, 1, 0, 100,
            listOf(
                BusinessPartnerSearchResultDto(
                    id = "BPNS0000000000WN",
                    name = "Stammwerk A",
                    legalForm = null,
                    street = BusinessPartnerVerboseValues.address1.street,
                    city = "Böblingen",
                    postalCode ="71059",
                    country = "DE",
                    identifiers =  emptyList()
                ),
                BusinessPartnerSearchResultDto(
                    id = "BPNL000000000065",
                    name = "Müller Handels GmbH & Co. KG",
                    legalForm = BusinessPartnerVerboseValues.legalForm3,
                    street = BusinessPartnerVerboseValues.address1.street,
                    city = "Böblingen",
                    postalCode ="71059",
                    country = "DE",
                    identifiers =  identifiers
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

        var identifiers = mutableListOf<LegalEntityIdentifierDto>()
        identifiers.add(BusinessPartnerNonVerboseValues.identifier3)

        val expected = PageDto(
            2, 1, 0, 100,
            listOf(
                BusinessPartnerSearchResultDto(
                    id = "BPNA00000000009W",
                    name = "Business Partner Name",
                    legalForm = null,
                    street = BusinessPartnerVerboseValues.address1.street,
                    city = "Böblingen",
                    postalCode ="71059",
                    country = "DE",
                    identifiers =  emptyList()
                ),
                BusinessPartnerSearchResultDto(
                    id = "BPNL000000000065",
                    name = "Müller Handels GmbH & Co. KG",
                    legalForm = BusinessPartnerVerboseValues.legalForm3,
                    street = BusinessPartnerVerboseValues.address1.street,
                    city = "Böblingen",
                    postalCode ="71059",
                    country = "DE",
                    identifiers =  identifiers
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