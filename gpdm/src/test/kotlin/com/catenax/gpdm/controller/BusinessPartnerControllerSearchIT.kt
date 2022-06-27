package com.catenax.gpdm.controller

import com.catenax.gpdm.Application
import com.catenax.gpdm.component.elastic.impl.service.ElasticSyncStarterService
import com.catenax.gpdm.dto.request.SitePropertiesSearchRequest
import com.catenax.gpdm.dto.response.BusinessPartnerSearchResponse
import com.catenax.gpdm.dto.response.PageResponse
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
import org.springframework.test.web.reactive.server.returnResult

/**
 * Integration tests for the search endpoint of the business partner controller
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles(value = ["test"])
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, ElasticsearchContextInitializer::class])
class BusinessPartnerControllerSearchIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val elasticSyncService: ElasticSyncStarterService,
    val testHelpers: TestHelpers,
    val businessPartnerBuildService: BusinessPartnerBuildService
) {
    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        elasticSyncService.clearElastic()

        businessPartnerBuildService.upsertBusinessPartners(
            listOf(RequestValues.businessPartnerRequest1, RequestValues.businessPartnerRequest2)
        )

        elasticSyncService.export()
    }

    /**
     * Given partners in Elasticsearch
     * When searching by site name
     * Then business partner is found
     */
    @Test
    fun `search business partner by site name, result found`() {
        val foundPartners = searchBusinessPartnerBySiteName(RequestValues.businessPartnerRequest1.sites.first().name).content
        assertThat(foundPartners).hasSize(1)
        assertThat(foundPartners.single().businessPartner.names.first().value).isEqualTo(RequestValues.businessPartnerRequest1.names.first().value)
    }

    /**
     * Given partners in Elasticsearch
     * When searching by nonexistent site name
     * Then no business partner is found
     */
    @Test
    fun `search business partner by site name, no result found`() {
        val foundPartners = searchBusinessPartnerBySiteName("nonexistent name").content
        assertThat(foundPartners).isEmpty()
    }

    private fun searchBusinessPartnerBySiteName(siteName: String): PageResponse<BusinessPartnerSearchResponse> {
        return webTestClient.get().uri { builder ->
            builder.path(EndpointValues.CATENA_BUSINESS_PARTNER_PATH)
                .queryParam(SitePropertiesSearchRequest::siteName.name, siteName)
                .build()
        }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<BusinessPartnerSearchResponse>>()
            .responseBody
            .blockFirst()!!
    }
}