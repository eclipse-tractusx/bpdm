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
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityMatchVerboseDto
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Integration tests for the search endpoint of the business partner controller
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles(value = ["test"])
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class LegalEntityControllerSearchIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val testHelpers: TestHelpers,
    val poolClient: PoolClientImpl
) {

    private val partnerStructure1 = LegalEntityStructureRequest(
        legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
        siteStructures = listOf(
            SiteStructureRequest(BusinessPartnerNonVerboseValues.siteCreate1)
        )
    )
    private val partnerStructure2 = LegalEntityStructureRequest(
        legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate2,
        siteStructures = listOf(
            SiteStructureRequest(BusinessPartnerNonVerboseValues.siteCreate2),
            SiteStructureRequest(BusinessPartnerNonVerboseValues.siteCreate1) //same site here to attain multiple results when needed
        )
    )

    private lateinit var givenPartner1: LegalEntityVerboseDto
    private lateinit var givenPartner2: LegalEntityVerboseDto
    private lateinit var legalName1: String
    private lateinit var legalName2: String
    private lateinit var legalAddress1: LogisticAddressVerboseDto
    private lateinit var legalAddress2: LogisticAddressVerboseDto

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()

        testHelpers.createTestMetadata()
        val givenStructure = testHelpers.createBusinessPartnerStructure(listOf(partnerStructure1, partnerStructure2))
        givenPartner1 = with(givenStructure[0].legalEntity) { legalEntity }
        givenPartner2 = with(givenStructure[1].legalEntity) { legalEntity }
        legalName1 = givenStructure[0].legalEntity.legalEntity.legalName
        legalName2 = givenStructure[1].legalEntity.legalEntity.legalName
        legalAddress1 = givenStructure[0].legalEntity.legalAddress
        legalAddress2 = givenStructure[1].legalEntity.legalAddress
    }

    /**
     * Given partners with same siteName
     * When searching by site name and requesting page with multiple items
     * Then response contains correct pagination values
     */
    @Test
    fun `search business partner with pagination, multiple items in page`() {

        val expected = PageDto(
            2, 1, 0, 2,
            listOf(
                LegalEntityMatchVerboseDto(score = 0f, legalEntity = givenPartner1, legalAddress = legalAddress1),
                LegalEntityMatchVerboseDto(score = 0f, legalEntity = givenPartner2, legalAddress = legalAddress2)
            )
        )

        val pageResponse = searchBusinessPartnerBySiteName(BusinessPartnerNonVerboseValues.siteCreate1.site.name, page = 0, size = 100)

        assertPageEquals(pageResponse, expected)
    }

    /**
     * Given partners with same siteName
     * When searching by site name and requesting multiple pages
     * Then responses contains correct pagination values
     */
    @Test
    fun `search business partner with pagination, multiple pages`() {

        val expectedFirstPage = PageDto(
            2, 2, 0, 1, listOf(
                LegalEntityMatchVerboseDto(score = 0f, legalEntity = givenPartner1, legalAddress = legalAddress1)
            )
        )
        val expectedSecondPage = PageDto(
            2, 2, 1, 1, listOf(
                LegalEntityMatchVerboseDto(score = 0f, legalEntity = givenPartner2, legalAddress = legalAddress2)
            )
        )

        val firstPage = searchBusinessPartnerBySiteName(BusinessPartnerNonVerboseValues.siteCreate1.site.name, page = 0, size = 1)
        val secondPage = searchBusinessPartnerBySiteName(BusinessPartnerNonVerboseValues.siteCreate1.site.name, page = 1, size = 1)

        assertPageEquals(firstPage, expectedFirstPage)
        assertPageEquals(secondPage, expectedSecondPage)
    }

    private fun searchBusinessPartnerBySiteName(siteName: String, page: Int, size: Int): PageDto<LegalEntityMatchVerboseDto> {
        val sitePropertiesSearchRequest = SitePropertiesSearchRequest(siteName)

        return poolClient.legalEntities.getLegalEntities(
            LegalEntityPropertiesSearchRequest.EmptySearchRequest,
            PaginationRequest(page, size)
        )


    }

    private fun assertPageEquals(actual: PageDto<LegalEntityMatchVerboseDto>, expected: PageDto<LegalEntityMatchVerboseDto>) {
        testHelpers.assertRecursively(actual)
            .ignoringFieldsMatchingRegexes(".*${LegalEntityMatchVerboseDto::score.name}")
            .isEqualTo(expected)
    }
}