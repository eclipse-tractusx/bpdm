package org.eclipse.tractusx.bpdm.pool.controller

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SiteResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SiteWithReferenceResponse
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class SiteControllerIT @Autowired constructor(
    val testHelpers: TestHelpers,
    val webTestClient: WebTestClient,
    val businessPartnerBuildService: BusinessPartnerBuildService
) {
    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
    }

    /**
     * Given partners in db
     * When requesting a site by bpn-s
     * Then site is returned
     */
    @Test
    fun `get site by bpn-s`() {
        val importedBusinessPartners = businessPartnerBuildService.upsertBusinessPartners(
            listOf(RequestValues.businessPartnerRequest1)
        )

        val importedPartner = importedBusinessPartners.single()
        importedPartner.bpn
            .let { bpn -> requestSitesOfLegalEntity(bpn).content.single().bpn }
            .let { bpnSite -> requestSite(bpnSite) }
            .let { siteResponse ->
                assertThat(siteResponse.bpnLegalEntity).isEqualTo(importedPartner.bpn)
            }
    }

    /**
     * Given partners in db
     * When requesting an site by non-existent bpn-s
     * Then a "not found" response is sent
     */
    @Test
    fun `get site by bpn-s, not found`() {
        businessPartnerBuildService.upsertBusinessPartners(listOf(RequestValues.businessPartnerRequest1))

        webTestClient.get()
            .uri(EndpointValues.CATENA_SITES_PATH + "/NONEXISTENT_BPN")
            .exchange().expectStatus().isNotFound
    }

    /**
     * Given sites of business partners
     * When searching for sites via BPNL
     * Then return sites that belong to those legal entities
     */
    @Test
    fun `search sites by BPNL`() {
        val newSite = RequestValues.siteRequest1.copy(name = "New Name")
        val newBusinessPartner = RequestValues.businessPartnerRequest1.copy(sites = listOf(newSite, RequestValues.siteRequest1))
        val givenPartners = listOf(newBusinessPartner, RequestValues.businessPartnerRequest2, RequestValues.businessPartnerRequest3)

        val createdPartners = businessPartnerBuildService.upsertBusinessPartners(givenPartners)

        val bpnL1 = getMatchingFromCandidates(newBusinessPartner, createdPartners).bpn
        val bpnL2 = getMatchingFromCandidates(RequestValues.businessPartnerRequest2, createdPartners).bpn

        val siteSearchRequest = SiteSearchRequest(listOf(bpnL1, bpnL2))
        val searchResult = webTestClient.invokePostEndpoint<PageResponse<SiteWithReferenceResponse>>(EndpointValues.CATENA_SITE_SEARCH_PATH, siteSearchRequest)

        val expectedSiteWithReference1 = SiteWithReferenceResponse(ResponseValues.site1.copy(name = "New Name"), bpnL1)
        val expectedSiteWithReference2 = SiteWithReferenceResponse(ResponseValues.site1, bpnL1)
        val expectedSiteWithReference3 = SiteWithReferenceResponse(ResponseValues.site2, bpnL2)

        assertThat(searchResult.content)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*uuid", ".*${SiteResponse::bpn.name}")
            .ignoringAllOverriddenEquals()
            .ignoringCollectionOrder()
            .isEqualTo(listOf(expectedSiteWithReference1, expectedSiteWithReference2, expectedSiteWithReference3))
    }

    private fun requestSite(bpnSite: String) =
        webTestClient.invokeGetEndpoint<SiteWithReferenceResponse>(EndpointValues.CATENA_SITES_PATH + "/${bpnSite}")

    private fun requestSitesOfLegalEntity(bpn: String) =
        webTestClient.invokeGetEndpoint<PageResponse<SiteResponse>>(EndpointValues.CATENA_BUSINESS_PARTNER_PATH + "/${bpn}" + EndpointValues.CATENA_SITES_PATH_POSTFIX)

    private fun getMatchingFromCandidates(partnerRequest: BusinessPartnerRequest, candidates: Collection<BusinessPartnerResponse>) =
        candidates.single { bp -> bp.names.any { id -> partnerRequest.names.map { it.value }.contains(id.value) } }

}