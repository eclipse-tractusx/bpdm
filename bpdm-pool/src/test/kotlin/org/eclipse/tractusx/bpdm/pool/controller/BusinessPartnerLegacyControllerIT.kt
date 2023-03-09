/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.common.dto.NameDto
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse

import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.model.response.BusinessPartnerMatchResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.BusinessPartnerResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles(value = ["test"])
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OpenSearchContextInitializer::class])
class BusinessPartnerLegacyControllerIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val testHelpers: TestHelpers
) {

    lateinit var givenPartner1: LegalEntityPartnerCreateResponse
    lateinit var givenPartner2: LegalEntityPartnerCreateResponse
    lateinit var givenPartner3: LegalEntityPartnerCreateResponse

    val uniqueName = "XXXXXX_UNIQUE_XXXXXX"

    val partnerStructures = listOf(
        LegalEntityStructureRequest(legalEntity = with(RequestValues.legalEntityCreate1) {
            copy(
                legalEntity = legalEntity.copy(
                    legalName = NameDto(
                        uniqueName,
                        null
                    )
                )
            )
        }),
        LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate2),
        LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate3),
    )

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        webTestClient.invokeDeleteEndpointWithoutResponse(EndpointValues.OPENSEARCH_SYNC_PATH)

        testHelpers.createTestMetadata()
        val givenStructure = testHelpers.createBusinessPartnerStructure(partnerStructures)
        givenPartner1 = givenStructure[0].legalEntity
        givenPartner2 = givenStructure[1].legalEntity
        givenPartner3 = givenStructure[2].legalEntity


        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.OPENSEARCH_SYNC_PATH)
    }


    /**
     * Given legal entities
     * When querying business partners
     * Then return arbitrary list of business partner matches
     */
    @Test
    fun `get existing business partners`() {

        val expected = getExpectedPage(listOf(givenPartner1, givenPartner2, givenPartner3))

        val respone = webTestClient.invokeGetEndpoint<PageResponse<BusinessPartnerMatchResponse>>(EndpointValues.CATENA_BUSINESS_PARTNER_LEGACY_PATH)


        testHelpers.assertRecursively(respone).ignoringFieldsMatchingRegexes(".*uuid", ".*score").isEqualTo(expected)
    }


    /**
     * Given legal entities
     * When searching business partners by name
     * Then return business partner matches
     */
    @Test
    fun `search business partners by name`() {

        val expected = getExpectedPage(listOf(givenPartner1))

        val response = webTestClient.invokeGetEndpoint<PageResponse<BusinessPartnerMatchResponse>>(
            EndpointValues.CATENA_BUSINESS_PARTNER_LEGACY_PATH,
            Pair("legalName", uniqueName)
        )

        testHelpers.assertRecursively(response).ignoringFieldsMatchingRegexes(".*uuid", ".*score").isEqualTo(expected)
    }

    /**
     * Given legal entities
     * When query business partner by BPN
     * Then return business partner
     */
    @Test
    fun `find by BPN`() {
        val expected = convertCreateResponse(givenPartner1)

        val bpn = givenPartner1.legalEntity.bpn
        val respone = webTestClient.invokeGetEndpoint<BusinessPartnerResponse>("${EndpointValues.CATENA_BUSINESS_PARTNER_LEGACY_PATH}/${bpn}")

        testHelpers.assertRecursively(respone).ignoringFieldsMatchingRegexes(".*uuid").isEqualTo(expected)
    }

    /**
     * Given legal entities
     * When query business partner by additional identifier
     * Then return business partner
     */
    @Test
    fun `find by other identifier`() {
        val expected = convertCreateResponse(givenPartner1)

        val additionalIdentifier = givenPartner1.legalEntity.identifiers.first()
        val idValue = additionalIdentifier.value
        val idType = additionalIdentifier.type.technicalKey
        val respone =
            webTestClient.invokeGetEndpoint<BusinessPartnerResponse>("${EndpointValues.CATENA_BUSINESS_PARTNER_LEGACY_PATH}/$idValue", Pair("idType", idType))

        testHelpers.assertRecursively(respone).ignoringFieldsMatchingRegexes(".*uuid").isEqualTo(expected)
    }

    private fun getExpectedPage(givenPartners: Collection<LegalEntityPartnerCreateResponse>): PageResponse<BusinessPartnerMatchResponse> {
        return PageResponse(givenPartners.size.toLong(), 1, 0, givenPartners.size,
            givenPartners.map { BusinessPartnerMatchResponse(0f, convertCreateResponse(it)) })
    }

    private fun convertCreateResponse(toConvert: LegalEntityPartnerCreateResponse): BusinessPartnerResponse {
        return BusinessPartnerResponse(
            "",
            toConvert.legalEntity,
            listOf(toConvert.legalAddress),
            emptyList()
        )
    }
}