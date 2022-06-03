package com.catenax.gpdm.component.elastic

import com.catenax.gpdm.Application
import com.catenax.gpdm.component.elastic.doc.InvalidBusinessPartnerDoc
import com.catenax.gpdm.dto.response.BusinessPartnerSearchResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.service.BpnIssuingService
import com.catenax.gpdm.util.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
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
@TestMethodOrder(OrderAnnotation::class)
class InvalidIndexStartupIT @Autowired constructor(
    private val webTestClient: WebTestClient,
    private val testHelpers: TestHelpers,
    private val elasticTemplate: ElasticsearchRestTemplate,
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
     * Not a real test but prepares the Elasticsearch container for the next test that will be run in a fresh Spring-Boot context
     * Create an invalid Elasticsearch index and fill it with bogus content
     */
    @Test
    @Order(0)
    @DirtiesContext
    fun setupIndexForNextTest() {
        testHelpers.truncateDbTables()
        //Clear and setup an invalid Elasticsearch context
        val bpIndex = elasticTemplate.indexOps(IndexCoordinates.of("business-partner"))

        bpIndex.delete()
        bpIndex.create()

        //Create a bogus document with a valid BPN
        val firstBpn = bpnIssuingService.issueLegalEntityBpns(1).first()
        val invalidBp = InvalidBusinessPartnerDoc(firstBpn, "outdated")

        elasticTemplate.save(invalidBp)

        //Check whether it really is inside the index
        assertThat(elasticTemplate.get(firstBpn, InvalidBusinessPartnerDoc::class.java)).isNotNull
    }

    /**
     * Given non-empty Elasticsearch index with outdated/invalid document structure
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
        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.ELASTIC_SYNC_PATH)

        searchResult = webTestClient.invokeGetEndpoint(EndpointValues.CATENA_BUSINESS_PARTNER_PATH)
        assertThat(searchResult.content).isNotEmpty
        assertThat(searchResult.contentSize).isEqualTo(1)
    }
}