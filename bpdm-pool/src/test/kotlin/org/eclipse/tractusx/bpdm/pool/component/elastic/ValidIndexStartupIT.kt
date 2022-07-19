package org.eclipse.tractusx.bpdm.pool.component.elastic

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerSearchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, ElasticsearchContextInitializer::class])
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ValidIndexStartupIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val testHelpers: TestHelpers
) {

    companion object {
        @RegisterExtension
        var wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.cdq.host") { wireMockServer.baseUrl() }
        }
    }

    /**
     * Not a real test but prepares the Elasticsearch container for the next test that will be run in a fresh Spring-Boot context
     * Create a valid Elasticsearch index and fill it with content
     */
    @Test
    @Order(0)
    @DirtiesContext
    fun setupIndexForNextTest() {
        testHelpers.truncateDbTables()
        //Clear and setup a fresh valid Elasticsearch context
        webTestClient.invokeDeleteEndpointWithoutResponse(EndpointValues.ELASTIC_SYNC_PATH)

        //Import values to DB
        val partnersToImport = listOf(CdqValues.businessPartner1, CdqValues.businessPartner2, CdqValues.businessPartner3)
        testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer)
        //Export to Elasticsearch index
        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.ELASTIC_SYNC_PATH)
        //Make sure entries are indeed there
        val searchResult = webTestClient.invokeGetEndpoint<PageResponse<BusinessPartnerSearchResponse>>(EndpointValues.CATENA_BUSINESS_PARTNER_PATH)
        Assertions.assertThat(searchResult.content).isNotEmpty
        Assertions.assertThat(searchResult.contentSize).isEqualTo(3)
    }

    /**
     * Given non-empty Elasticsearch index with up-to-date document structure
     * When application starts
     * Then index not cleared
     */
    @Test
    @Order(1)
    fun acceptValidIndexOnStartup() {
        val searchResult = webTestClient.invokeGetEndpoint<PageResponse<BusinessPartnerSearchResponse>>(EndpointValues.CATENA_BUSINESS_PARTNER_PATH)

        Assertions.assertThat(searchResult.content).isNotEmpty
        Assertions.assertThat(searchResult.contentSize).isEqualTo(3)
    }
}