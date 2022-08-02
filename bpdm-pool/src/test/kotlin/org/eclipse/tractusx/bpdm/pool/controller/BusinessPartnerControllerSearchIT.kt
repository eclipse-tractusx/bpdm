package org.eclipse.tractusx.bpdm.pool.controller

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service.OpenSearchSyncStarterService
import org.eclipse.tractusx.bpdm.pool.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.SitePropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerSearchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
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
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OpenSearchContextInitializer::class])
class BusinessPartnerControllerSearchIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val openSearchSyncStarterService: OpenSearchSyncStarterService,
    val testHelpers: TestHelpers,
    val businessPartnerBuildService: BusinessPartnerBuildService
) {
    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        openSearchSyncStarterService.clearOpenSearch()

        businessPartnerBuildService.upsertBusinessPartners(
            listOf(RequestValues.businessPartnerRequest1, RequestValues.businessPartnerRequest2)
        )

        openSearchSyncStarterService.export()
    }

    /**
     * Given partners with same siteName in OpenSearch
     * When searching by site name and requesting page with multiple items
     * Then response contains correct pagination values
     */
    @Test
    fun `search business partner with pagination, multiple items in page`() {
        // insert partner again, so we get multiple search results
        businessPartnerBuildService.upsertBusinessPartners(
            listOf(RequestValues.businessPartnerRequest1)
        )
        openSearchSyncStarterService.export()

        val pageResponse = searchBusinessPartnerBySiteName(RequestValues.businessPartnerRequest1.sites.first().name, page = 0, size = 100)
        assertThat(pageResponse.contentSize).isEqualTo(2)
        assertThat(pageResponse.page).isEqualTo(0)
        assertThat(pageResponse.totalElements).isEqualTo(2)
        assertThat(pageResponse.totalPages).isEqualTo(1)
    }

    /**
     * Given partners with same siteName in OpenSearch
     * When searching by site name and requesting multiple pages
     * Then responses contains correct pagination values
     */
    @Test
    fun `search business partner with pagination, multiple pages`() {
        // insert partner again, so we get multiple search results
        businessPartnerBuildService.upsertBusinessPartners(
            listOf(RequestValues.businessPartnerRequest1)
        )
        openSearchSyncStarterService.export()

        val firstPage = searchBusinessPartnerBySiteName(RequestValues.businessPartnerRequest1.sites.first().name, page = 0, size = 1)
        assertThat(firstPage.contentSize).isEqualTo(1)
        assertThat(firstPage.page).isEqualTo(0)
        assertThat(firstPage.totalElements).isEqualTo(2)
        assertThat(firstPage.totalPages).isEqualTo(2)

        val secondPage = searchBusinessPartnerBySiteName(RequestValues.businessPartnerRequest1.sites.first().name, page = 1, size = 1)
        assertThat(secondPage.contentSize).isEqualTo(1)
        assertThat(secondPage.page).isEqualTo(1)
        assertThat(secondPage.totalElements).isEqualTo(2)
        assertThat(secondPage.totalPages).isEqualTo(2)
    }

    /**
     * Given partners in OpenSearch
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
     * Given partners in OpenSearch
     * When searching by nonexistent site name
     * Then no business partner is found
     */
    @Test
    fun `search business partner by site name, no result found`() {
        val foundPartners = searchBusinessPartnerBySiteName("nonexistent name").content
        assertThat(foundPartners).isEmpty()
    }

    private fun searchBusinessPartnerBySiteName(siteName: String, page: Int? = null, size: Int? = null): PageResponse<BusinessPartnerSearchResponse> {
        return webTestClient.invokeGetEndpoint(
            EndpointValues.CATENA_BUSINESS_PARTNER_PATH,
            *(listOfNotNull(
                SitePropertiesSearchRequest::siteName.name to siteName,
                if (page != null) PaginationRequest::page.name to page.toString() else null,
                if (size != null) PaginationRequest::size.name to size.toString() else null
            ).toTypedArray())
        )
    }
}