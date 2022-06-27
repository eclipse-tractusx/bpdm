package com.catenax.gpdm.controller

import com.catenax.gpdm.Application
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.dto.response.SiteResponse
import com.catenax.gpdm.dto.response.SiteWithReferenceResponse
import com.catenax.gpdm.service.BusinessPartnerBuildService
import com.catenax.gpdm.util.*
import org.assertj.core.api.Assertions.assertThat
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

    private fun requestSite(bpnSite: String) =
        webTestClient.invokeGetEndpoint<SiteWithReferenceResponse>(EndpointValues.CATENA_SITES_PATH + "/${bpnSite}")

    private fun requestSitesOfLegalEntity(bpn: String) =
        webTestClient.invokeGetEndpoint<PageResponse<SiteResponse>>(EndpointValues.CATENA_BUSINESS_PARTNER_PATH + "/${bpn}" + EndpointValues.CATENA_SITES_PATH_POSTFIX)
}