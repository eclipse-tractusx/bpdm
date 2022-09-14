/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.component.cdq.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.common.dto.response.IdentifierResponse
import org.eclipse.tractusx.bpdm.common.dto.response.LegalEntityPartnerResponse
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.service.CdqMappings
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.component.cdq.config.CdqAdapterConfigProperties
import org.eclipse.tractusx.bpdm.pool.component.cdq.dto.BusinessPartnerWithParent
import org.eclipse.tractusx.bpdm.pool.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.pool.dto.request.AddressPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.*
import org.eclipse.tractusx.bpdm.pool.dto.response.LegalEntityMatchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SitePartnerSearchResponse
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient


private const val CDQ_MOCK_URL = "/test-cdq-api/storages/test-cdq-storage"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class CdqControllerImportIT @Autowired constructor(
    private val webTestClient: WebTestClient,
    bpnConfigProperties: BpnConfigProperties,
    private val objectMapper: ObjectMapper,
    private val testHelpers: TestHelpers,
    private val cdqAdapterConfigProperties: CdqAdapterConfigProperties
) {
    private val idTypeBpn = TypeKeyNameUrlCdq(cdqAdapterConfigProperties.bpnKey, bpnConfigProperties.name, "")
    private val issuerBpn = TypeKeyNameUrlCdq(bpnConfigProperties.agencyKey, bpnConfigProperties.agencyName, "")
    private val statusBpn = TypeKeyNameCdq("UNKNOWN", "Unknown")

    private val idTypeCdq = TypeKeyNameUrlDto("CDQID", "CDQ Identifier", "")
    private val issuerCdq = TypeKeyNameUrlDto("CDQ", "CDQ AG", "")
    private val statusCdq = TypeKeyNameDto("CDQ_IMPORTED", "Imported from CDQ but not synchronized")

    private val identifier1 = IdentifierResponse(CdqValues.partnerId1, idTypeCdq, issuerCdq, statusCdq)
    private val identifier2 = IdentifierResponse(CdqValues.partnerId2, idTypeCdq, issuerCdq, statusCdq)
    private val identifier3 = IdentifierResponse(CdqValues.partnerId3, idTypeCdq, issuerCdq, statusCdq)

    val expectedLegalEntity1 = with(ResponseValues.legalEntity1) { copy(properties = properties.copy(identifiers = properties.identifiers.plus(identifier1))) }
    val expectedLegalEntity2 = with(ResponseValues.legalEntity2) { copy(properties = properties.copy(identifiers = properties.identifiers.plus(identifier2))) }
    val expectedLegalEntity3 = with(ResponseValues.legalEntity3) { copy(properties = properties.copy(identifiers = properties.identifiers.plus(identifier3))) }


    companion object {
        @RegisterExtension
        var wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.cdq.host") { wireMockServer.baseUrl() }
        }
    }

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
    }

    /**
     * Given new partners of type legal entity in CDQ
     * When Import from CDQ
     * Then partners imported
     */
    @Test
    fun `import new legal entity partners`() {

        val partnersToImport = listOf(
            CdqValues.legalEntity1,
            CdqValues.legalEntity2,
            CdqValues.legalEntity3
        )

        val partnersExpected = listOf(
            expectedLegalEntity1,
            expectedLegalEntity2,
            expectedLegalEntity3
        )

        //Import partner  and check whether successfully imported
        val response = testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer)
        assertLegalEntityResponseEquals(response.content.map { it.legalEntity }, partnersExpected)
    }

    /**
     * Given new partners of type site in CDQ
     * When Import from CDQ
     * Then partners imported
     */
    @Test
    fun `import new site partners`() {

        val cxPoolIdType = TypeKeyNameUrlCdq("CX_POOL")
        val parentIdentifier1 = "p1"
        val parentIdentifier2 = "p2"
        val parentIdentifier3 = "p3"

        val parentToCreate1 = with(CdqValues.legalEntity1) { copy(identifiers = listOf(IdentifierCdq(cxPoolIdType, parentIdentifier1))) }
        val parentToCreate2 = with(CdqValues.legalEntity2) { copy(identifiers = listOf(IdentifierCdq(cxPoolIdType, parentIdentifier2))) }
        val parentToCreate3 = with(CdqValues.legalEntity3) { copy(identifiers = listOf(IdentifierCdq(cxPoolIdType, parentIdentifier3))) }

        val parentsToCreate = listOf(
            parentToCreate1,
            parentToCreate2,
            parentToCreate3
        )

        val bpns = importLegalEntities(parentsToCreate).map { it.bpn }

        val parentBpn1 = bpns[0]
        val parentBpn2 = bpns[1]
        val parentBpn3 = bpns[2]

        val mockedRequest = FetchBatchRequest(listOf(parentIdentifier1, parentIdentifier2, parentIdentifier3))

        val mockedFetchBatchResponse = listOf(
            FetchBatchRecord(
                parentIdentifier1,
                BusinessPartnerBatchCdq(parentToCreate1.identifiers.plus(IdentifierCdq(CdqValues.bpnIdentifiertype, parentBpn1)))
            ),
            FetchBatchRecord(
                parentIdentifier2,
                BusinessPartnerBatchCdq(parentToCreate1.identifiers.plus(IdentifierCdq(CdqValues.bpnIdentifiertype, parentBpn2)))
            ),
            FetchBatchRecord(
                parentIdentifier3,
                BusinessPartnerBatchCdq(parentToCreate1.identifiers.plus(IdentifierCdq(CdqValues.bpnIdentifiertype, parentBpn3)))
            )
        )

        wireMockServer.stubFor(
            post(urlPathMatching(cdqAdapterConfigProperties.fetchBusinessPartnersBatchUrl))
                .withRequestBody(equalTo(objectMapper.writeValueAsString(mockedRequest)))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockedFetchBatchResponse))
                )
        )


        val sitesToImport = listOf(
            BusinessPartnerWithParent(
                with(CdqValues.site1) { copy(relations = listOf(CdqValues.parentRelation.copy(startNode = parentIdentifier1))) },
                parentBpn1
            ),
            BusinessPartnerWithParent(
                with(CdqValues.site2) { copy(relations = listOf(CdqValues.parentRelation.copy(startNode = parentIdentifier2))) },
                parentBpn2
            ),
            BusinessPartnerWithParent(
                with(CdqValues.site3) { copy(relations = listOf(CdqValues.parentRelation.copy(startNode = parentIdentifier3))) },
                parentBpn2
            ),
        )

        //Import sites
        val actualSites = importSites(sitesToImport)

        val expectedSites = listOf(
            SitePartnerSearchResponse(ResponseValues.site1, parentBpn1),
            SitePartnerSearchResponse(ResponseValues.site2, parentBpn2),
            SitePartnerSearchResponse(ResponseValues.site3, parentBpn3)
        )

        testHelpers.assertRecursively(actualSites).ignoringFieldsMatchingRegexes(".*bpn").isEqualTo(expectedSites)
    }

    /**
     * Given new partners of type address in CDQ
     * When Import from CDQ
     * Then partners imported
     */
    @Test
    fun `import new address partners`() {

        val cxPoolIdType = TypeKeyNameUrlCdq("CX_POOL")
        val parentIdentifier1 = "p1"
        val parentIdentifier2 = "p2"
        val parentIdentifier3 = "p3"

        val parentToCreate1 = with(CdqValues.legalEntity1) { copy(identifiers = listOf(IdentifierCdq(cxPoolIdType, parentIdentifier1))) }
        val parentToCreate2 = with(CdqValues.legalEntity2) { copy(identifiers = listOf(IdentifierCdq(cxPoolIdType, parentIdentifier2))) }
        val parentToCreate3 = with(CdqValues.legalEntity3) { copy(identifiers = listOf(IdentifierCdq(cxPoolIdType, parentIdentifier3))) }

        val parentsToCreate = listOf(
            parentToCreate1,
            parentToCreate2,
            parentToCreate3
        )

        val bpns = importLegalEntities(parentsToCreate).map { it.bpn }

        val parentBpn1 = bpns[0]
        val parentBpn2 = bpns[1]
        val parentBpn3 = bpns[2]

        val mockedRequest = FetchBatchRequest(listOf(parentIdentifier1, parentIdentifier2, parentIdentifier3))

        val mockedFetchBatchResponse = listOf(
            FetchBatchRecord(
                parentIdentifier1,
                BusinessPartnerBatchCdq(parentToCreate1.identifiers.plus(IdentifierCdq(CdqValues.bpnIdentifiertype, parentBpn1)))
            ),
            FetchBatchRecord(
                parentIdentifier2,
                BusinessPartnerBatchCdq(parentToCreate1.identifiers.plus(IdentifierCdq(CdqValues.bpnIdentifiertype, parentBpn2)))
            ),
            FetchBatchRecord(
                parentIdentifier3,
                BusinessPartnerBatchCdq(parentToCreate1.identifiers.plus(IdentifierCdq(CdqValues.bpnIdentifiertype, parentBpn3)))
            )
        )

        wireMockServer.stubFor(
            post(urlPathMatching(cdqAdapterConfigProperties.fetchBusinessPartnersBatchUrl))
                .withRequestBody(equalTo(objectMapper.writeValueAsString(mockedRequest)))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockedFetchBatchResponse))
                )
        )


        val addressesToImport = listOf(
            with(CdqValues.addressPartner1) { copy(relations = listOf(CdqValues.parentRelation.copy(startNode = parentIdentifier1))) },
            with(CdqValues.addressPartner2) { copy(relations = listOf(CdqValues.parentRelation.copy(startNode = parentIdentifier2))) },
            with(CdqValues.addressPartner3) { copy(relations = listOf(CdqValues.parentRelation.copy(startNode = parentIdentifier3))) },
        )

        //Import address partners
        testHelpers.importAndGetResponse(addressesToImport, webTestClient, wireMockServer)

        val actualAddresses = webTestClient.invokePostEndpoint<PageResponse<AddressPartnerSearchResponse>>(
            EndpointValues.CATENA_ADDRESSES_SEARCH_PATH,
            AddressPartnerSearchRequest(legalEntities = bpns)
        )

        val expectedAddresses = listOf(
            AddressPartnerSearchResponse(ResponseValues.addressPartner1, parentBpn1, null),
            AddressPartnerSearchResponse(ResponseValues.addressPartner2, parentBpn2, null),
            AddressPartnerSearchResponse(ResponseValues.addressPartner3, parentBpn3, null)
        )

        testHelpers.assertRecursively(actualAddresses.content).ignoringFieldsMatchingRegexes(".*bpn").isEqualTo(expectedAddresses)
    }

    /**
     * Given new partners
     * When Import partners multiple times
     * Then no duplicate partners
     */
    @Test
    fun importPartnersMultipleTimes() {
        val partnersToImport = listOf(
            CdqValues.legalEntity1,
            CdqValues.legalEntity2,
            CdqValues.legalEntity3
        )

        val partnersExpected = listOf(
            expectedLegalEntity1,
            expectedLegalEntity2,
            expectedLegalEntity3
        )

        //Import partner first time and check whether successfully imported
        val firstResponse = testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer)
        assertLegalEntityResponseEquals(firstResponse.content.map { it.legalEntity }, partnersExpected)

        //Import partner second time and check for no duplicates or other changes
        val secondResponse = testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer)
        assertLegalEntityResponseEquals(secondResponse.content.map { it.legalEntity }, partnersExpected)
    }

    /**
     * Given new partners in CDQ
     * When import with pagination
     * Then partners imported
     */
    @Test
    fun importNewPartnersWithPagination() {
        val expectedPartners = listOf(
            expectedLegalEntity1,
            expectedLegalEntity2,
            expectedLegalEntity3,
        )

        val page1 = PagedResponseCdq(
            1,
            null,
            CdqValues.partnerId2,
            1,
            listOf(CdqValues.legalEntity1)
        )

        wireMockServer.stubFor(
            get(urlPathMatching(cdqAdapterConfigProperties.readBusinessPartnerUrl))
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", absent())
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(page1))
                )
        )

        val page2 = PagedResponseCdq(
            1,
            null,
            CdqValues.partnerId3,
            1,
            listOf(CdqValues.legalEntity2)
        )

        wireMockServer.stubFor(
            get(urlPathMatching(cdqAdapterConfigProperties.readBusinessPartnerUrl))
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", equalTo(CdqValues.partnerId2))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(page2))
                )
        )

        val page3 = PagedResponseCdq(
            1,
            null,
            null,
            1,
            listOf(CdqValues.legalEntity3)
        )

        wireMockServer.stubFor(
            get(urlPathMatching(cdqAdapterConfigProperties.readBusinessPartnerUrl))
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", equalTo(CdqValues.partnerId3))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(page3))
                )
        )

        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.CDQ_SYNCH_PATH)

        val savedBusinessPartners = webTestClient.invokeGetEndpoint<PageResponse<LegalEntityMatchResponse>>(EndpointValues.CATENA_LEGAL_ENTITY_PATH)
        assertLegalEntityResponseEquals(savedBusinessPartners.content.map { it.legalEntity }, expectedPartners)
    }

    /**
     * Several partner updates should be considered:
     * Given imported partners names are modified in CDQ
     * When import partners
     * Then partners are updated
     */
    @Test
    fun updateModifiedPartners_names(){
        val partnersToImport = listOf(
            CdqValues.legalEntity1,
            CdqValues.legalEntity2,
            CdqValues.legalEntity3
        )

        val partnersExpected = listOf(
            expectedLegalEntity1,
            expectedLegalEntity2,
            expectedLegalEntity3
        )

        //Import partner first and check whether successfully imported
        val response = testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer)
        assertLegalEntityResponseEquals(response.content.map { it.legalEntity }, partnersExpected)

        //retrieve BPNs for update
        val cdqIdBpnPairs = response.content.map { Pair(testHelpers.extractCdqId(it.legalEntity.properties), it.legalEntity.bpn) }
        val bpnL1 = cdqIdBpnPairs.find { (cdqId, _) -> cdqId == CdqValues.legalEntity1.id }!!.second
        val bpnL2 = cdqIdBpnPairs.find { (cdqId, _) -> cdqId == CdqValues.legalEntity2.id }!!.second
        val bpnL3 = cdqIdBpnPairs.find { (cdqId, _) -> cdqId == CdqValues.legalEntity3.id }!!.second


        //Prepare modified partners to import
        val modifiedName1 = CdqValues.legalEntityName1.copy(value = "${CdqValues.legalEntityName1.value}_mod")
        val modifiedName2 = CdqValues.legalEntityName2.copy(value = "${CdqValues.legalEntityName2.value}_mod")
        val modifiedName3 = CdqValues.legalEntityName3.copy(value = "${CdqValues.legalEntityName3.value}_mod")

        val updatedBusinessPartner1 = plusBpn(CdqValues.legalEntity1.copy(names = listOf(modifiedName1)), bpnL1)
        val updatedBusinessPartner2 = plusBpn(CdqValues.legalEntity2.copy(names = listOf(modifiedName2)), bpnL2)
        val updatedBusinessPartner3 = plusBpn(CdqValues.legalEntity3.copy(names = listOf(modifiedName3)), bpnL3)

        val modifiedNameResponse1 = ResponseValues.name1.copy(value = modifiedName1.value)
        val modifiedNameResponse2 = ResponseValues.name2.copy(value = modifiedName2.value)
        val modifiedNameResponse3 = ResponseValues.name3.copy(value = modifiedName3.value)

        val updatedPartnerResponse1 =
            expectedLegalEntity1.copy(properties = expectedLegalEntity1.properties.copy(names = listOf(modifiedNameResponse1)))
        val updatedPartnerResponse2 =
            expectedLegalEntity2.copy(properties = expectedLegalEntity2.properties.copy(names = listOf(modifiedNameResponse2)))
        val updatedPartnerResponse3 =
            expectedLegalEntity3.copy(properties = expectedLegalEntity3.properties.copy(names = listOf(modifiedNameResponse3)))

        val modifiedPartnersToImport = listOf(
            updatedBusinessPartner1,
            updatedBusinessPartner2,
            updatedBusinessPartner3
        )

        val modifiedExpectedPartners = listOf(
            updatedPartnerResponse1,
            updatedPartnerResponse2,
            updatedPartnerResponse3,
        )

        //Import updated partners from CDQ and check whether updates in our system
        val modifiedResponse = testHelpers.importAndGetResponse(modifiedPartnersToImport, webTestClient, wireMockServer)
        assertLegalEntityResponseEquals(modifiedResponse.content.map { it.legalEntity }, modifiedExpectedPartners)
    }

    /**
     *
     * All modified fields of a partner should be updated:
     * Given imported partner is modified in CDQ
     * When import partners
     * Then partner is updated
     */
    @Test
    fun updateModifiedPartner() {
        val partnersToImport = listOf(
            CdqValues.legalEntity1
        )

        val partnersExpected = listOf(
            expectedLegalEntity1
        )

        //Import partner first and check whether successfully imported
        val response = testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer)
        assertLegalEntityResponseEquals(response.content.map { it.legalEntity }, partnersExpected)

        //Prepare modified partner to import
        val bpn = response.content.single().legalEntity.bpn
        val modifiedPartnersToImport = listOf(
            plusBpn(CdqValues.legalEntity3, bpn)
        )

        val modifiedExpectedPartners = listOf(
            expectedLegalEntity3.copy(bpn = bpn)
        )

        //Import updated partners from CDQ and check whether updates in our system
        val modifiedResponse = testHelpers.importAndGetResponse(modifiedPartnersToImport, webTestClient, wireMockServer)
        assertLegalEntityResponseEquals(modifiedResponse.content.map { it.legalEntity }, modifiedExpectedPartners)
    }

    /**
     * Given updated partners of type site in CDQ
     * When Import from CDQ
     * Then partners updated
     */
    @Test
    fun `import updated site partners`() {

        val cxPoolIdType = TypeKeyNameUrlCdq("CX_POOL")
        val parentIdentifier1 = "p1"
        val parentIdentifier2 = "p2"
        val parentIdentifier3 = "p3"

        val parentToCreate1 = with(CdqValues.legalEntity1) { copy(identifiers = listOf(IdentifierCdq(cxPoolIdType, parentIdentifier1))) }
        val parentToCreate2 = with(CdqValues.legalEntity2) { copy(identifiers = listOf(IdentifierCdq(cxPoolIdType, parentIdentifier2))) }
        val parentToCreate3 = with(CdqValues.legalEntity3) { copy(identifiers = listOf(IdentifierCdq(cxPoolIdType, parentIdentifier3))) }

        val parentsToCreate = listOf(
            parentToCreate1,
            parentToCreate2,
            parentToCreate3
        )

        val parentBpns = importLegalEntities(parentsToCreate).map { it.bpn }

        val parentBpn1 = parentBpns[0]
        val parentBpn2 = parentBpns[1]
        val parentBpn3 = parentBpns[2]

        val mockedRequest = FetchBatchRequest(listOf(parentIdentifier1, parentIdentifier2, parentIdentifier3))

        val mockedFetchBatchResponse = listOf(
            FetchBatchRecord(
                parentIdentifier1,
                BusinessPartnerBatchCdq(parentToCreate1.identifiers.plus(IdentifierCdq(CdqValues.bpnIdentifiertype, parentBpn1)))
            ),
            FetchBatchRecord(
                parentIdentifier2,
                BusinessPartnerBatchCdq(parentToCreate1.identifiers.plus(IdentifierCdq(CdqValues.bpnIdentifiertype, parentBpn2)))
            ),
            FetchBatchRecord(
                parentIdentifier3,
                BusinessPartnerBatchCdq(parentToCreate1.identifiers.plus(IdentifierCdq(CdqValues.bpnIdentifiertype, parentBpn3)))
            )
        )

        wireMockServer.stubFor(
            post(urlPathMatching(cdqAdapterConfigProperties.fetchBusinessPartnersBatchUrl))
                .withRequestBody(equalTo(objectMapper.writeValueAsString(mockedRequest)))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockedFetchBatchResponse))
                )
        )


        val sitesToImport = listOf(
            BusinessPartnerWithParent(
                with(CdqValues.site1) { copy(relations = listOf(CdqValues.parentRelation.copy(startNode = parentIdentifier1))) },
                parentBpn1
            ),
            BusinessPartnerWithParent(
                with(CdqValues.site2) { copy(relations = listOf(CdqValues.parentRelation.copy(startNode = parentIdentifier2))) },
                parentBpn2
            ),
            BusinessPartnerWithParent(
                with(CdqValues.site3) { copy(relations = listOf(CdqValues.parentRelation.copy(startNode = parentIdentifier3))) },
                parentBpn2
            ),
        )

        val siteBpns = importSites(sitesToImport).map { it.bpn }

        val sitesToUpdate = listOf(
            BusinessPartnerWithParent(plusBpn(CdqValues.site1, siteBpns[1]), parentBpn1),
            BusinessPartnerWithParent(plusBpn(CdqValues.site2, siteBpns[2]), parentBpn2),
            BusinessPartnerWithParent(plusBpn(CdqValues.site3, siteBpns[0]), parentBpn3)
        )

        val updatedSites = importSites(sitesToUpdate)

        val expectedSites = listOf(
            ResponseValues.site1.copy(bpn = siteBpns[1]),
            ResponseValues.site2.copy(bpn = siteBpns[2]),
            ResponseValues.site3.copy(bpn = siteBpns[0])
        )

        testHelpers.assertRecursively(updatedSites).isEqualTo(expectedSites)
    }

    private fun plusBpn(partner: BusinessPartnerCdq, bpn: String) =
        partner.copy(identifiers = partner.identifiers.plus(IdentifierCdq(idTypeBpn, bpn, issuerBpn, statusBpn)))


    private fun assertLegalEntityResponseEquals(
        actualPartners: Collection<LegalEntityPartnerResponse>,
        expectedPartners: Collection<LegalEntityPartnerResponse>
    ) {
        assertThat(actualPartners)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*uuid", ".*bpn", ".*currentness")
            .ignoringAllOverriddenEquals()
            .ignoringCollectionOrder()
            .isEqualTo(expectedPartners)
    }


    /**
     * Import the given legal entities and return them as response in the same order
     */
    private fun importLegalEntities(partners: List<BusinessPartnerCdq>): List<LegalEntityPartnerResponse> {
        val partnerIds = partners.map { partner -> partner.identifiers.first().value }
        val response = testHelpers.importAndGetResponse(partners, webTestClient, wireMockServer)
        return partnerIds.map { response.content.find { entry -> entry.legalEntity.properties.identifiers.any { id -> id.value == it } }!!.legalEntity }
    }

    /**
     * Import the given sites and return them as response in the same order
     */
    private fun importSites(partners: List<BusinessPartnerWithParent>): List<SitePartnerResponse> {
        val parentIds = partners.map { it.parentBpn }
        testHelpers.importAndGetResponse(partners.map { it.partner }, webTestClient, wireMockServer)
        val createdSites = webTestClient.invokePostEndpoint<PageResponse<SitePartnerSearchResponse>>(
            EndpointValues.CATENA_SITE_SEARCH_PATH,
            SiteSearchRequest(legalEntities = parentIds)
        )
        return partners.map { createdSites.content.find { site -> site.bpnLegalEntity == it.parentBpn }!!.site }
    }

}