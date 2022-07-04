package org.eclipse.tractusx.bpdm.pool.component.elastic.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.component.cdq.dto.BusinessPartnerCollectionCdq
import org.eclipse.tractusx.bpdm.pool.component.cdq.service.ImportStarterService
import org.eclipse.tractusx.bpdm.pool.component.elastic.impl.service.ElasticSyncStarterService
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerPropertiesSearchRequest
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


/**
 * Integration tests for the data synch endpoints in the ElasticSearchController
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, ElasticsearchContextInitializer::class])
class ElasticSearchControllerIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val importService: ImportStarterService,
    val elasticSyncService: ElasticSyncStarterService,
    val objectMapper: ObjectMapper,
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

    val partnerDocs = listOf(
        CdqValues.businessPartner1,
        CdqValues.businessPartner2,
        CdqValues.businessPartner3
    )

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        elasticSyncService.clearElastic()

        val importCollection = BusinessPartnerCollectionCdq(
            partnerDocs.size,
            null,
            null,
            partnerDocs.size,
            partnerDocs
        )

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathMatching(EndpointValues.CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(importCollection))
                )
        )

        importService.import()
    }


    /**
     * Given partners in database already exported
     * When export
     * Then partners are not exported to Elasticsearch
     */
    @Test
    fun `export only new partners`() {
        //export once to get partners into elasticsearch for given system state
        var exportResponse = testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.ELASTIC_SYNC_PATH)

        assertThat(exportResponse.count).isEqualTo(3)
        assertSearchableByNames(partnerDocs.map { it.names.first().value })

        //export now to check behaviour
        exportResponse = testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.ELASTIC_SYNC_PATH)

        assertThat(exportResponse.count).isEqualTo(0)
    }

    /**
     * Given new partners in database
     * When export
     * Then new partners can be searched
     */
    @Test
    fun `can search exported partners`() {
        val exportResponse = testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.ELASTIC_SYNC_PATH)

        assertThat(exportResponse.count).isEqualTo(3)
        assertSearchableByNames(partnerDocs.map { it.names.first().value })
    }

    /**
     * Given partners in Elasticsearch
     * When delete index
     * Then partners can't be searched anymore
     */
    @Test
    fun `empty index`() {
        val names = partnerDocs.map { it.names.first().value }

        // fill the elasticsearch index
        val exportResponse = testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.ELASTIC_SYNC_PATH)

        assertThat(exportResponse.count).isEqualTo(3)
        assertSearchableByNames(names)

        //clear the index
        webTestClient.delete().uri(EndpointValues.ELASTIC_SYNC_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful

        //check that the partners can really not be searched anymore
        names.forEach { assertThat(searchBusinessPartnerByName(it)).matches { it.contentSize == 0 } }
    }

    /**
     * Given partners in Elasticsearch
     * When delete index and export
     * Then partners again in Elasticsearch
     */
    @Test
    fun `export all partners after empty index`() {

        // fill the elasticsearch index
        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.ELASTIC_SYNC_PATH)


        //clear the index
        webTestClient.delete().uri(EndpointValues.ELASTIC_SYNC_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful

        //export partners again
        val exportResponse = testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.ELASTIC_SYNC_PATH)

        assertThat(exportResponse.count).isEqualTo(3)
        assertSearchableByNames(partnerDocs.map { it.names.first().value })

    }

    private fun searchBusinessPartnerByName(name: String): PageResponse<BusinessPartnerSearchResponse> {
        return webTestClient.get().uri { builder ->
            builder.path(EndpointValues.CATENA_BUSINESS_PARTNER_PATH)
                .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, name)
                .build()
        }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<BusinessPartnerSearchResponse>>()
            .responseBody
            .blockFirst()!!
    }

    private fun assertSearchableByNames(names: Collection<String>) {
        names.forEach { name ->
            val pageResult = searchBusinessPartnerByName(name)

            assertThat(pageResult.content).isNotEmpty
            assertThat(pageResult.content.first()).matches { it.businessPartner.names.any { n -> n.value == name } }
        }
    }


}