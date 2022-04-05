package com.catenax.gpdm.component.elastic.controller

import com.catenax.gpdm.Application
import com.catenax.gpdm.component.cdq.dto.BusinessPartnerCollectionCdq
import com.catenax.gpdm.component.cdq.service.PartnerImportService
import com.catenax.gpdm.component.elastic.impl.service.ElasticSyncService
import com.catenax.gpdm.dto.elastic.ExportResponse
import com.catenax.gpdm.dto.request.BusinessPartnerPropertiesSearchRequest
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.catenax.gpdm.dto.response.BusinessPartnerSearchResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.util.CdqTestValues
import com.catenax.gpdm.util.EndpointValues
import com.catenax.gpdm.util.TestHelpers
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.junit.jupiter.Testcontainers


/**
 * Integration tests for the data synch endpoints in the ElasticSearchController
 *
 * The tests implement the following acceptance criteria:
 *
 * Can search exported partners
 * Given new partners in database
 * When export
 * Then new partners can be searched
 *
 * Export only new partners
 * Given partners in database already exported
 * When export
 * Then partners are not exported to Elasticsearch
 *
 *
 * Empty index
 * Given partners in Elasticsearch
 * When delete index
 * Then partners can't be searched anymore
 *
 * Export all partners after empty index
 * Given partners in Elasticsearch
 * When delete index and export
 * Then partners again in Elasticsearch
 *
 *
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
class ElasticSearchControllerIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val importService: PartnerImportService,
    val elasticSyncService: ElasticSyncService,
    val objectMapper: ObjectMapper,
    val testHelpers: TestHelpers
) {

    companion object Container {
        @org.testcontainers.junit.jupiter.Container
        val elasticsearchContainer: ElasticsearchContainer =
            ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.0")
                .withEnv("discovery.type", "single-node")

        @RegisterExtension
        var wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.elastic.enabled") { true }
            registry.add("spring.elasticsearch.uris", elasticsearchContainer::getHttpHostAddress)
            registry.add("bpdm.cdq.host") { wireMockServer.baseUrl() }
        }
    }

    @BeforeEach
    fun beforeEach() {
        val partnerDocs = listOf(
            CdqTestValues.businessPartner1,
            CdqTestValues.businessPartner2,
            CdqTestValues.businessPartner3
        )

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

    @AfterEach
    fun afterEach() {
        testHelpers.truncateH2()
        elasticSyncService.clearElastic()
    }


    @Test
    fun `export only new partners`() {

        //for updatetAfter condition to work
        Thread.sleep(1000)

        //export once to get partners into elasticsearch for given system state
        val fullExport = webTestClient.post().uri(EndpointValues.ELASTIC_EXPORT_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody(ExportResponse::class.java)
            .returnResult()
            .responseBody!!


        assertThat(fullExport.exportedSize).isEqualTo(3)
        assertThat(fullExport.exportedBpns.size).isEqualTo(3)

        //export now to check behaviour
        val emptyExport = webTestClient.post().uri(EndpointValues.ELASTIC_EXPORT_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody(ExportResponse::class.java)
            .returnResult()
            .responseBody!!


        assertThat(emptyExport.exportedSize).isEqualTo(0)
        assertThat(emptyExport.exportedBpns.size).isEqualTo(0)
    }

    @Test
    fun `can search exported partners`() {
        val exportResponse = webTestClient.post().uri(EndpointValues.ELASTIC_EXPORT_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody(ExportResponse::class.java)
            .returnResult()
            .responseBody!!


        assertThat(exportResponse.exportedSize).isEqualTo(3)
        assertThat(exportResponse.exportedBpns.size).isEqualTo(3)
        assertThat(exportResponse.exportedBpns).doesNotHaveDuplicates()

        findBusinessPartnersByBpn(exportResponse.exportedBpns)
            .forEach { assertThatCanSearchBusinessPartnerByName(it.names.first().value, it.bpn) }

    }

    @Test
    fun `empty index`() {

        val names = listOf(
            CdqTestValues.businessPartner1.names.first().value,
            CdqTestValues.businessPartner2.names.first().value,
            CdqTestValues.businessPartner3.names.first().value,
        )

        // fill the elasticsearch index
        webTestClient.post().uri(EndpointValues.ELASTIC_EXPORT_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful


        //clear the index
        webTestClient.delete().uri(EndpointValues.ELASTIC_EXPORT_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful

        //check that the partners can really not be searched anymore
        names.forEach { assertThat(searchBusinessPartnerByName(it)).matches { it.contentSize == 0 } }
    }

    @Test
    fun `export all partners after empty index`() {

        // fill the elasticsearch index
        webTestClient.post().uri(EndpointValues.ELASTIC_EXPORT_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful


        //clear the index
        webTestClient.delete().uri(EndpointValues.ELASTIC_EXPORT_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful

        //export partners again
        val exportResponse = webTestClient.post().uri(EndpointValues.ELASTIC_EXPORT_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody(ExportResponse::class.java)
            .returnResult()
            .responseBody!!

        assertThat(exportResponse.exportedSize).isEqualTo(3)
        assertThat(exportResponse.exportedBpns.size).isEqualTo(3)
        assertThat(exportResponse.exportedBpns).doesNotHaveDuplicates()

        //check that exported partners are now indeed searchable
        findBusinessPartnersByBpn(exportResponse.exportedBpns)
            .forEach { bp ->
                assertThat(searchBusinessPartnerByName(bp.names.first().value).content)
                    .anyMatch { it.businessPartner.bpn == bp.bpn }
            }

    }


    private fun findBusinessPartnersByBpn(bpns: Collection<String>): Collection<BusinessPartnerResponse> {
        return bpns.map { bpn ->
            webTestClient.get().uri("${EndpointValues.CATENA_BUSINESS_PARTNER_PATH}/$bpn")
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody(BusinessPartnerResponse::class.java)
                .returnResult()
                .responseBody!!
        }
    }


    private fun assertThatCanSearchBusinessPartnerByName(name: String, bpn: String) {
        assertThat(searchBusinessPartnerByName(name).content).anyMatch { it.businessPartner.bpn == bpn }
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


}