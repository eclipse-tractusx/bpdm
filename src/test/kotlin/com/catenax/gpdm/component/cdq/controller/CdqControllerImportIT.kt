package com.catenax.gpdm.component.cdq.controller

import com.catenax.gpdm.Application
import com.catenax.gpdm.component.cdq.config.CdqIdentifierConfigProperties
import com.catenax.gpdm.component.cdq.dto.BusinessPartnerCdq
import com.catenax.gpdm.component.cdq.dto.BusinessPartnerCollectionCdq
import com.catenax.gpdm.config.BpnConfigProperties
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.catenax.gpdm.dto.response.BusinessPartnerSearchResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.util.CdqValues
import com.catenax.gpdm.util.EndpointValues
import com.catenax.gpdm.util.ResponseValues
import com.catenax.gpdm.util.TestHelpers
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult


private const val CDQ_MOCK_URL = "/test-cdq-api/storages/test-cdq-storage"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
class CdqControllerImportIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val cdqIdProperties: CdqIdentifierConfigProperties,
    val bpnConfigProperties: BpnConfigProperties,
    val objectMapper: ObjectMapper,
    val testHelpers: TestHelpers
) {

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

    @AfterEach
    fun afterEach() {
        testHelpers.truncateH2()
    }

    /**
     * Given new partners in CDQ
     * When Import from CDQ
     * Then partners imported
     */
    @Test
    fun importNewPartners() {

        val partnersToImport = listOf(
            CdqValues.businessPartner1,
            CdqValues.businessPartner2,
            CdqValues.businessPartner3
        )

        val partnersExpected = listOf(
            ResponseValues.businessPartner1,
            ResponseValues.businessPartner2,
            ResponseValues.businessPartner3
        )

        //Import partner  and check whether successfully imported
        val importedBusinessPartners = importAndGetResponse(partnersToImport)
        assertPartnerResponseEqual(importedBusinessPartners.content.map { it.businessPartner }, partnersExpected)
    }

    /**
     * Given new partners
     * When Import partners multiple times
     * Then no duplicate partners
     */
    @Test
    fun importPartnersMultipleTimes() {
        val partnersToImport = listOf(
            CdqValues.businessPartner1,
            CdqValues.businessPartner2,
            CdqValues.businessPartner3
        )

        val partnersExpected = listOf(
            ResponseValues.businessPartner1,
            ResponseValues.businessPartner2,
            ResponseValues.businessPartner3
        )

        //Import partner first time and check whether successfully imported
        var importedBusinessPartners = importAndGetResponse(partnersToImport)
        assertPartnerResponseEqual(importedBusinessPartners.content.map { it.businessPartner }, partnersExpected)

        //Import partner second time and check for no duplicates or other changes
        importedBusinessPartners = importAndGetResponse(partnersToImport)
        assertPartnerResponseEqual(importedBusinessPartners.content.map { it.businessPartner }, partnersExpected)

    }

    /**
     * Given new partners in CDQ
     * When import with pagination
     * Then partners imported
     */
    @Test
    fun importNewPartnersWithPagination() {
        val expectedPartners = listOf(
            ResponseValues.businessPartner1,
            ResponseValues.businessPartner2,
            ResponseValues.businessPartner3,
        )

        val page1 =  BusinessPartnerCollectionCdq(
            1,
            null,
            CdqValues.partnerId2,
            1,
            listOf(CdqValues.businessPartner1)
        )

        wireMockServer.stubFor(
            get(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", absent())
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(page1))
                )
        )

        val page2 =  BusinessPartnerCollectionCdq(
            1,
            null,
            CdqValues.partnerId3,
            1,
            listOf(CdqValues.businessPartner2)
        )

        wireMockServer.stubFor(
            get(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", equalTo(CdqValues.partnerId2))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(page2))
                )
        )

        val page3 =  BusinessPartnerCollectionCdq(
            1,
            null,
            null,
            1,
            listOf(CdqValues.businessPartner3)
        )

        wireMockServer.stubFor(
            get(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", equalTo(CdqValues.partnerId3))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(page3))
                )
        )

        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.CDQ_SYNCH_PATH)

        val savedBusinessPartners =
            webTestClient.get().uri(EndpointValues.CATENA_BUSINESS_PARTNER_PATH)
                .exchange()
                .expectStatus()
                .isOk
                .returnResult<PageResponse<BusinessPartnerSearchResponse>>()
                .responseBody
                .blockFirst()!!

       assertPartnerResponseEqual(savedBusinessPartners.content.map { it.businessPartner }, expectedPartners)
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
            CdqValues.businessPartner1,
            CdqValues.businessPartner2,
            CdqValues.businessPartner3
        )

        val partnersExpected = listOf(
            ResponseValues.businessPartner1,
            ResponseValues.businessPartner2,
            ResponseValues.businessPartner3
        )

        //Import partner first and check whether successfully imported
        var importedBusinessPartners = importAndGetResponse(partnersToImport)
        assertPartnerResponseEqual(importedBusinessPartners.content.map { it.businessPartner }, partnersExpected)


        //Prepare modified partners to import
        val modifiedName1 = CdqValues.name1.copy(value = "${CdqValues.name1.value}_mod")
        val modifiedName2 = CdqValues.name2.copy(value = "${CdqValues.name2.value}_mod")
        val modifiedName3 = CdqValues.name3.copy(value = "${CdqValues.name3.value}_mod")

        val updatedBusinessPartner1 = CdqValues.businessPartner1.copy(names = listOf(modifiedName1))
        val updatedBusinessPartner2 = CdqValues.businessPartner2.copy(names = listOf(modifiedName2))
        val updatedBusinessPartner3 = CdqValues.businessPartner3.copy(names = listOf(modifiedName3))

        val modifiedNameResponse1 = ResponseValues.name1.copy(value = modifiedName1.value)
        val modifiedNameResponse2 = ResponseValues.name2.copy(value = modifiedName2.value)
        val modifiedNameResponse3 = ResponseValues.name3.copy(value = modifiedName3.value)

        val updatedPartnerResponse1 = ResponseValues.businessPartner1.copy(names = listOf(modifiedNameResponse1))
        val updatedPartnerResponse2 = ResponseValues.businessPartner2.copy(names = listOf(modifiedNameResponse2))
        val updatedPartnerResponse3 = ResponseValues.businessPartner3.copy(names = listOf(modifiedNameResponse3))

        val modifiedPartnersToImport = listOf(
            updatedBusinessPartner1,
            updatedBusinessPartner2,
            updatedBusinessPartner3
        )

        val modifiedExpectedPartners = listOf(
            updatedPartnerResponse1,
            updatedPartnerResponse2,
            updatedPartnerResponse3
        )

        //Import updated partners from CDQ and check whether updates in our system
        importedBusinessPartners = importAndGetResponse(modifiedPartnersToImport)
        assertPartnerResponseEqual(importedBusinessPartners.content.map { it.businessPartner }, modifiedExpectedPartners)
    }

    /**
     *
     * All modified fields of a partner should be updated:
     * Given imported partner is modified in CDQ
     * When import partners
     * Then partner is updated
     */
    @Test
    fun updateModifiedPartner(){
        val partnersToImport = listOf(
            CdqValues.businessPartner1
        )

        val partnersExpected = listOf(
            ResponseValues.businessPartner1
        )

        //Import partner first and check whether successfully imported
        var importedBusinessPartners = importAndGetResponse(partnersToImport)
        assertPartnerResponseEqual(importedBusinessPartners.content.map { it.businessPartner }, partnersExpected)

        //Prepare modified partner to import
        val modifiedPartnersToImport = listOf(
          CdqValues.businessPartner3.copy(id =  CdqValues.businessPartner1.id)
        )

        val modifiedExpectedPartners = listOf(
            ResponseValues.businessPartner3.copy(identifiers = ResponseValues.businessPartner1.identifiers)
        )

        //Import updated partners from CDQ and check whether updates in our system
        importedBusinessPartners = importAndGetResponse(modifiedPartnersToImport)
        assertPartnerResponseEqual(importedBusinessPartners.content.map { it.businessPartner }, modifiedExpectedPartners)
    }

    private fun extractCdqId(it: BusinessPartnerResponse) = it.identifiers.find { id -> id.type.technicalKey == cdqIdProperties.typeKey }!!.value

    private fun importAndGetResponse(partnersToImport: Collection<BusinessPartnerCdq>): PageResponse<BusinessPartnerSearchResponse>{
        val importCollection = BusinessPartnerCollectionCdq(
            partnersToImport.size,
            null,
            null,
            partnersToImport.size,
            partnersToImport
        )

        wireMockServer.stubFor(
            get(urlPathMatching(EndpointValues.CDQ_MOCK_BUSINESS_PARTNER_PATH)).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(objectMapper.writeValueAsString(importCollection))
            )
        )

        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.CDQ_SYNCH_PATH)

        return webTestClient
            .get()
            .uri(EndpointValues.CATENA_BUSINESS_PARTNER_PATH)
            .exchange().expectStatus().isOk
            .returnResult<PageResponse<BusinessPartnerSearchResponse>>()
            .responseBody
            .blockFirst()!!
    }

    private fun assertPartnerResponseEqual(actualPartners: Collection<BusinessPartnerResponse>, expectedPartners: Collection<BusinessPartnerResponse>){
        val cdqIdToExpected = expectedPartners.associateBy { extractCdqId(it) }
        val actualToExpectedMap = actualPartners
            .map { Pair(it, cdqIdToExpected[extractCdqId(it)]) }

        actualToExpectedMap.forEach { (actualPartner, expectedPartner) ->
            assertThat(expectedPartner).isNotNull
            val expectedWithBpn = replaceBpn(expectedPartner!!, actualPartner.bpn)

            assertThat(actualPartner)
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(".*uuid")
                .ignoringAllOverriddenEquals()
                .ignoringCollectionOrder()
                .isEqualTo(expectedWithBpn)
        }
    }

    private fun replaceBpn(response: BusinessPartnerResponse, bpn: String): BusinessPartnerResponse{
        val bpnIdentifier = response.identifiers.find { id -> id.type.technicalKey == bpnConfigProperties.id }!!
        val expectedIdentifiers = response.identifiers.map { id -> if(id == bpnIdentifier) bpnIdentifier.copy(value = bpn) else id }
        return response.copy(bpn = bpn, identifiers = expectedIdentifiers )
    }
}