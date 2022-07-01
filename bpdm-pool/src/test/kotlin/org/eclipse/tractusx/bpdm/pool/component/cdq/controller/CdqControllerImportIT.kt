package org.eclipse.tractusx.bpdm.pool.component.cdq.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.component.cdq.dto.BusinessPartnerCdq
import org.eclipse.tractusx.bpdm.pool.component.cdq.dto.BusinessPartnerCollectionCdq
import org.eclipse.tractusx.bpdm.pool.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.pool.dto.response.AddressResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerSearchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
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
import org.springframework.test.web.reactive.server.returnResult


private const val CDQ_MOCK_URL = "/test-cdq-api/storages/test-cdq-storage"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class CdqControllerImportIT @Autowired constructor(
    val webTestClient: WebTestClient,
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

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
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
            Pair(ResponseValues.businessPartner1, ResponseValues.address1),
            Pair(ResponseValues.businessPartner2, ResponseValues.address2),
            Pair(ResponseValues.businessPartner3, ResponseValues.address3)
        )

        //Import partner  and check whether successfully imported
        val importedBusinessPartnersAndAddresses = importAndGetResponseWithAddresses(partnersToImport)
        assertPartnerAddressResponseEqual(importedBusinessPartnersAndAddresses, partnersExpected)
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
            Pair(ResponseValues.businessPartner1, ResponseValues.address1),
            Pair(ResponseValues.businessPartner2, ResponseValues.address2),
            Pair(ResponseValues.businessPartner3, ResponseValues.address3)
        )

        //Import partner first time and check whether successfully imported
        var importedBusinessPartnersAndAddresses = importAndGetResponseWithAddresses(partnersToImport)
        assertPartnerAddressResponseEqual(importedBusinessPartnersAndAddresses, partnersExpected)

        //Import partner second time and check for no duplicates or other changes
        importedBusinessPartnersAndAddresses = importAndGetResponseWithAddresses(partnersToImport)
        assertPartnerAddressResponseEqual(importedBusinessPartnersAndAddresses, partnersExpected)
    }

    /**
     * Given new partners in CDQ
     * When import with pagination
     * Then partners imported
     */
    @Test
    fun importNewPartnersWithPagination() {
        val expectedPartners = listOf(
            Pair(ResponseValues.businessPartner1, ResponseValues.address1),
            Pair(ResponseValues.businessPartner2, ResponseValues.address2),
            Pair(ResponseValues.businessPartner3, ResponseValues.address3)
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

        val savedBusinessPartnersWithAddresses = savedBusinessPartners.content.map { it.businessPartner }.map {
            Pair(
                it,
                webTestClient.invokeGetEndpoint<PageResponse<AddressResponse>>(EndpointValues.CATENA_BUSINESS_PARTNER_PATH + "/${it.bpn}" + EndpointValues.CATENA_ADDRESSES_PATH_POSTFIX).content.first()
            )
        }

        assertPartnerAddressResponseEqual(savedBusinessPartnersWithAddresses, expectedPartners)
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
            Pair(ResponseValues.businessPartner1, ResponseValues.address1),
            Pair(ResponseValues.businessPartner2, ResponseValues.address2),
            Pair(ResponseValues.businessPartner3, ResponseValues.address3)
        )

        //Import partner first and check whether successfully imported
        var importedBusinessPartnersAndAddresses = importAndGetResponseWithAddresses(partnersToImport)
        assertPartnerAddressResponseEqual(importedBusinessPartnersAndAddresses, partnersExpected)


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
            Pair(updatedPartnerResponse1, ResponseValues.address1),
            Pair(updatedPartnerResponse2, ResponseValues.address2),
            Pair(updatedPartnerResponse3, ResponseValues.address3)
        )

        //Import updated partners from CDQ and check whether updates in our system
        importedBusinessPartnersAndAddresses = importAndGetResponseWithAddresses(modifiedPartnersToImport)
        assertPartnerAddressResponseEqual(importedBusinessPartnersAndAddresses, modifiedExpectedPartners)
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
            CdqValues.businessPartner1
        )

        val partnersExpected = listOf(
            Pair(ResponseValues.businessPartner1, ResponseValues.address1)
        )

        //Import partner first and check whether successfully imported
        var importedBusinessPartnersAndAddresses = importAndGetResponseWithAddresses(partnersToImport)
        assertPartnerAddressResponseEqual(importedBusinessPartnersAndAddresses, partnersExpected)

        //Prepare modified partner to import
        val modifiedPartnersToImport = listOf(
            CdqValues.businessPartner3.copy(id = CdqValues.businessPartner1.id)
        )

        val modifiedExpectedPartners = listOf(
            Pair(ResponseValues.businessPartner3.copy(identifiers = ResponseValues.businessPartner1.identifiers), ResponseValues.address3)
        )

        //Import updated partners from CDQ and check whether updates in our system
        importedBusinessPartnersAndAddresses = importAndGetResponseWithAddresses(modifiedPartnersToImport)
        assertPartnerAddressResponseEqual(importedBusinessPartnersAndAddresses, modifiedExpectedPartners)
    }

    private fun importAndGetResponseWithAddresses(partnersToImport: List<BusinessPartnerCdq>) =
        testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer).content.map { it.businessPartner }.map {
            Pair(
                it,
                webTestClient.invokeGetEndpoint<PageResponse<AddressResponse>>(EndpointValues.CATENA_BUSINESS_PARTNER_PATH + "/${it.bpn}" + EndpointValues.CATENA_ADDRESSES_PATH_POSTFIX).content.first()
            )
        }

    private fun assertPartnerAddressResponseEqual(
        actualPartners: Collection<Pair<BusinessPartnerResponse, AddressResponse>>,
        expectedPartners: Collection<Pair<BusinessPartnerResponse, AddressResponse>>
    ) {
        val cdqIdToExpected = expectedPartners.associateBy { testHelpers.extractCdqId(it.first) }
        val actualToExpectedMap = actualPartners
            .map { Pair(it, cdqIdToExpected[testHelpers.extractCdqId(it.first)]) }

        actualToExpectedMap.forEach { (actualPartner, expectedPartner) ->
            assertThat(expectedPartner).isNotNull
            val expectedWithBpn =
                Pair(replaceBpn(expectedPartner!!.first, actualPartner.first.bpn).copy(currentness = actualPartner.first.currentness), expectedPartner.second)

            assertThat(actualPartner)
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(".*uuid", Pair<BusinessPartnerResponse, AddressResponse>::second.name + "\\." + AddressResponse::bpn.name)
                .ignoringAllOverriddenEquals()
                .ignoringCollectionOrder()
                .isEqualTo(expectedWithBpn)
        }
    }

    private fun replaceBpn(response: BusinessPartnerResponse, bpn: String): BusinessPartnerResponse {
        val bpnIdentifier = response.identifiers.find { id -> id.type.technicalKey == bpnConfigProperties.id }!!
        val expectedIdentifiers = response.identifiers.map { id -> if(id == bpnIdentifier) bpnIdentifier.copy(value = bpn) else id }
        return response.copy(bpn = bpn, identifiers = expectedIdentifiers )
    }
}