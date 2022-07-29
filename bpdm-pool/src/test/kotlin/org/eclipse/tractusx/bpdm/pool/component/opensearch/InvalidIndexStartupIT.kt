package org.eclipse.tractusx.bpdm.pool.component.opensearch

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.BUSINESS_PARTNER_INDEX_NAME
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerSearchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.service.BpnIssuingService
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.RegisterExtension
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.Refresh
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
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OpenSearchContextInitializer::class])
@TestMethodOrder(OrderAnnotation::class)
class InvalidIndexStartupIT @Autowired constructor(
    private val webTestClient: WebTestClient,
    private val testHelpers: TestHelpers,
    private val openSearchClient: OpenSearchClient,
    private val bpnIssuingService: BpnIssuingService
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
     * Not a real test but prepares the OpenSearch container for the next test that will be run in a fresh Spring-Boot context
     * Create an invalid OpenSearch index and fill it with bogus content
     */
    @Test
    @Order(0)
    @DirtiesContext
    fun setupIndexForNextTest() {
        testHelpers.truncateDbTables()
        //Clear and set up an invalid OpenSearch context
        openSearchClient.indices().delete { it.index(BUSINESS_PARTNER_INDEX_NAME) }
        openSearchClient.indices().create { createRequestIndex ->
            createRequestIndex.index(BUSINESS_PARTNER_INDEX_NAME).mappings { mapping ->
                mapping.properties("outdatedField") { property ->
                    property.searchAsYouType { it }
                }
            }
        }

        //Create a bogus document with a valid BPN
        val firstBpn = bpnIssuingService.issueLegalEntityBpns(1).first()
        val invalidBp = InvalidBusinessPartnerDoc("outdated")

        openSearchClient.index { indexRequest -> indexRequest.index(BUSINESS_PARTNER_INDEX_NAME).id(firstBpn).document(invalidBp).refresh(Refresh.True) }

        //Check whether it really is inside the index
        val getResponse =
            openSearchClient.get({ getRequest -> getRequest.index(BUSINESS_PARTNER_INDEX_NAME).id(firstBpn) }, InvalidBusinessPartnerDoc::class.java)
        assertThat(getResponse.found()).isTrue
    }

    /**
     * Given non-empty OpenSearch index with outdated/invalid document structure
     * When application starts
     * Then index deleted and recreated with up-to-date document structure
     */
    @Test
    @Order(1)
    fun recreateOutdatedIndexOnStartup() {
        //in case bogus document is still there we should find it by importing a business partner to DB and search it
        testHelpers.importAndGetResponse(listOf(CdqValues.businessPartner1), webTestClient, wireMockServer)

        var searchResult = webTestClient.invokeGetEndpoint<PageResponse<BusinessPartnerSearchResponse>>(EndpointValues.CATENA_BUSINESS_PARTNER_PATH)
        //should not find anything otherwise the bogus document is still there
        assertThat(searchResult.content).isEmpty()

        //Now export to index again and check whether the imported business partner can be found as normal
        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.OPENSEARCH_SYNC_PATH)

        searchResult = webTestClient.invokeGetEndpoint(EndpointValues.CATENA_BUSINESS_PARTNER_PATH)
        assertThat(searchResult.content).isNotEmpty
        assertThat(searchResult.contentSize).isEqualTo(1)
    }

    private data class InvalidBusinessPartnerDoc(
        val outdatedField: String = ""
    )
}