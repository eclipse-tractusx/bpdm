package org.eclipse.tractusx.bpdm.pool.controller

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.component.cdq.dto.BusinessPartnerCdq
import org.eclipse.tractusx.bpdm.pool.dto.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.*
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class AddressControllerIT @Autowired constructor(
    val testHelpers: TestHelpers,
    val webTestClient: WebTestClient,
    val businessPartnerBuildService: BusinessPartnerBuildService
) {
    companion object {
        @RegisterExtension
        val wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.cdq.host") { wireMockServer.baseUrl() }
        }
    }

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
    }

    /**
     * Given partners in db
     * When requesting an address by bpn-a
     * Then address is returned
     */
    @Test
    fun `get address by bpn-a`() {
        val partnersToImport = listOf(CdqValues.businessPartner1)
        val importedBusinessPartners = testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer)

        val importedPartner = importedBusinessPartners.content.single().businessPartner
        importedPartner.bpn
            .let { bpn -> requestAddressesOfLegalEntity(bpn).content.single().bpn }
            .let { bpnAddress -> requestAddress(bpnAddress) }
            .let { addressResponse ->
                assertThat(addressResponse.bpnLegalEntity).isEqualTo(importedPartner.bpn)
            }
    }

    /**
     * Given partners in db
     * When requesting an address by non-existent bpn-a
     * Then a "not found" response is sent
     */
    @Test
    fun `get address by bpn-a, not found`() {
        val partnersToImport = listOf(CdqValues.businessPartner1)
        testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer)

        webTestClient.get()
            .uri(EndpointValues.CATENA_ADDRESSES_PATH + "/NONEXISTENT_BPN")
            .exchange().expectStatus().isNotFound
    }

    /**
     * Given multiple addresses of business partners
     * When searching addresses with BPNL
     * Then return addresses belonging to those legal entities
     */
    @Test
    fun `search addresses by BPNL`() {
        val newAddress = CdqValues.address1.copy(localities = listOf(CdqValues.locality1.copy(value = "New Value")))
        val newPartner = CdqValues.businessPartner1.copy(addresses = listOf(CdqValues.address1, newAddress))
        val givenPartners = listOf(newPartner, CdqValues.businessPartner2, CdqValues.businessPartner3)

        val importedBusinessPartners = testHelpers.importAndGetResponse(givenPartners, webTestClient, wireMockServer).content.map { it.businessPartner }

        val bpnL1 = getMatchingFromCandidates(newPartner, importedBusinessPartners).bpn
        val bpnL2 = getMatchingFromCandidates(CdqValues.businessPartner2, importedBusinessPartners).bpn

        val searchRequest = AddressSearchRequest(listOf(bpnL1, bpnL2), emptyList())
        val searchResult =
            webTestClient.invokePostEndpoint<PageResponse<AddressWithReferenceResponse>>(EndpointValues.CATENA_ADDRESSES_SEARCH_PATH, searchRequest)

        val expectedAddress1 = ResponseValues.address1
        val expectedAddress2 = ResponseValues.address1.copy(localities = listOf(ResponseValues.locality1.copy(value = "New Value")))
        val expectedAddress3 = ResponseValues.address2

        val expectedAddressWithReferences1 = AddressWithReferenceResponse(expectedAddress1, bpnL1, null)
        val expectedAddressWithReferences2 = AddressWithReferenceResponse(expectedAddress2, bpnL1, null)
        val expectedAddressWithReferences3 = AddressWithReferenceResponse(expectedAddress3, bpnL2, null)

        assertThat(searchResult.content)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*uuid", ".*${AddressResponse::bpn.name}")
            .ignoringAllOverriddenEquals()
            .ignoringCollectionOrder()
            .isEqualTo(listOf(expectedAddressWithReferences1, expectedAddressWithReferences2, expectedAddressWithReferences3))
    }


    /**
     * Given multiple addresses of business partners
     * When searching addresses with BPNS
     * Then return addresses belonging to those sites
     */
    @Test
    fun `search addresses by BPNS`() {
        val newAddress1 = RequestValues.addressRequest1.copy(premises = listOf(RequestValues.premiseRequest1.copy(value = "New Value")))
        val newAddress2 = RequestValues.addressRequest1.copy(premises = listOf(RequestValues.premiseRequest1.copy(value = "Another Value")))
        val newSite1 = RequestValues.siteRequest1.copy(addresses = listOf(RequestValues.addressRequest1, newAddress1))
        val newSite2 = RequestValues.siteRequest2.copy(addresses = listOf(newAddress2))
        val newPartner1 = RequestValues.businessPartnerRequest1.copy(sites = listOf(newSite1))
        val newPartner2 = RequestValues.businessPartnerRequest2.copy(sites = listOf(newSite2))
        val givenPartners = listOf(newPartner1, newPartner2, RequestValues.businessPartnerRequest3)

        val createdPartners = businessPartnerBuildService.upsertBusinessPartners(givenPartners)

        val bpnL1 = getMatchingFromCandidates(newPartner1, createdPartners).bpn
        val bpnL2 = getMatchingFromCandidates(newPartner2, createdPartners).bpn

        val partner1Site =
            webTestClient.invokeGetEndpoint<PageResponse<SiteResponse>>("${EndpointValues.CATENA_BUSINESS_PARTNER_PATH}/$bpnL1/${EndpointValues.CATENA_SITES_PATH_POSTFIX}").content.single()
        val partner2Site =
            webTestClient.invokeGetEndpoint<PageResponse<SiteResponse>>("${EndpointValues.CATENA_BUSINESS_PARTNER_PATH}/$bpnL2/${EndpointValues.CATENA_SITES_PATH_POSTFIX}").content.single()

        val bpnS1 = partner1Site.bpn
        val bpnS2 = partner2Site.bpn

        val searchRequest = AddressSearchRequest(emptyList(), listOf(bpnS1, bpnS2))
        val searchResult =
            webTestClient.invokePostEndpoint<PageResponse<AddressWithReferenceResponse>>(EndpointValues.CATENA_ADDRESSES_SEARCH_PATH, searchRequest)

        val expectedPremise = PremiseResponse(CommonValues.uuid1, CommonValues.premise6, null, null, ResponseValues.premiseType1, ResponseValues.language0)
        val expectedAddress1 = AddressResponse(
            CommonValues.uuid1,
            CommonValues.bpn1,
            ResponseValues.version1,
            country = ResponseValues.country1,
            premises = listOf(expectedPremise)
        )
        val expectedAddress2 = expectedAddress1.copy(premises = listOf(expectedPremise.copy(value = "New Value")))
        val expectedAddress3 = expectedAddress1.copy(premises = listOf(expectedPremise.copy(value = "Another Value")))

        val expectedAddressWithReferences1 = AddressWithReferenceResponse(expectedAddress1, null, bpnS1)
        val expectedAddressWithReferences2 = AddressWithReferenceResponse(expectedAddress2, null, bpnS1)
        val expectedAddressWithReferences3 = AddressWithReferenceResponse(expectedAddress3, null, bpnS2)

        assertThat(searchResult.content)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*uuid", ".*${AddressResponse::bpn.name}")
            .ignoringAllOverriddenEquals()
            .ignoringCollectionOrder()
            .isEqualTo(listOf(expectedAddressWithReferences1, expectedAddressWithReferences2, expectedAddressWithReferences3))
    }


    private fun requestAddress(bpnAddress: String) =
        webTestClient.invokeGetEndpoint<AddressWithReferenceResponse>(EndpointValues.CATENA_ADDRESSES_PATH + "/${bpnAddress}")

    private fun requestAddressesOfLegalEntity(bpn: String) =
        webTestClient.invokeGetEndpoint<PageResponse<AddressResponse>>(EndpointValues.CATENA_BUSINESS_PARTNER_PATH + "/${bpn}" + EndpointValues.CATENA_ADDRESSES_PATH_POSTFIX)


    private fun getMatchingFromCandidates(cdqPartner: BusinessPartnerCdq, candidates: Collection<BusinessPartnerResponse>) =
        candidates.single { bp -> bp.identifiers.any { id -> id.value == cdqPartner.id } }

    private fun getMatchingFromCandidates(partnerRequest: BusinessPartnerRequest, candidates: Collection<BusinessPartnerResponse>) =
        candidates.single { bp -> bp.names.any { id -> partnerRequest.names.map { it.value }.contains(id.value) } }

}