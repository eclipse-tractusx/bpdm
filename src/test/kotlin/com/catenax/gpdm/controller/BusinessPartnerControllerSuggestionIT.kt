package com.catenax.gpdm.controller

import com.catenax.gpdm.Application
import com.catenax.gpdm.component.cdq.dto.BusinessPartnerCollectionCdq
import com.catenax.gpdm.component.cdq.service.PartnerImportService
import com.catenax.gpdm.component.elastic.impl.service.ElasticSyncService
import com.catenax.gpdm.dto.request.BusinessPartnerPropertiesSearchRequest
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.dto.response.SuggestionResponse
import com.catenax.gpdm.util.CdqTestValues
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
 * Integration tests for the look-ahead endpoints of the business partner controller
 *
 * Tests for the following acceptance criteria:
 *
 * Suggest property values
 * Given partner with property value
 * When ask suggestion for property
 * Then show property value
 *
 * Suggest by phrase
 * Given partner with property value
 * When ask suggestion for that value
 * Then show that value
 *
 * Suggest by word
 * Given partner with property value that is several words
 * When ask suggestion for a word in value
 * Then show that value
 *
 * Suggest by prefix
 * Given partner with property value
 * When ask suggestion for a prefix of that value
 * Then show that value
 *
 * Suggest by non-latin characters
 * Given partner with property value in non-latin characters
 * When ask suggestion for that value
 * Then show that value
 *
 * Don't suggest by different
 * Given partner with property value
 * When ask suggestion for text that doesn't have a word or prefix in value
 * Then don't show that value
 *
 * Suggest filtered suggestions
 * Given partner with property 1 value and property 2 value
 * When ask suggestion property 1 with filter by property 2 value
 * Then show property 1 value 1
 *
 * Suggest by word in filtered suggestions
 * Given partner with property 1 value and property 2 value
 * When ask suggestion for a word in property 1 value with filter by property 2 value
 * Then show property 1 value
 *
 * Don't suggest by word when filtered out
 * Given partner with property 1 value and property 2 value
 * When ask suggestion for a word in property 1 value with filter by other than property 2 value
 * Then don't show property 1 value
 *
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
class BusinessPartnerControllerSuggestionIT @Autowired constructor(
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

        const val CDQ_MOCK_URL = "/test-cdq-api/storages/test-cdq-storage"

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.elastic.enabled") { true }
            registry.add("spring.elasticsearch.uris", elasticsearchContainer::getHttpHostAddress)
            registry.add("bpdm.cdq.host") { wireMockServer.baseUrl() }
        }

        const val queryTextParameterName = "text"
        const val businessPartnerPath = "/api/catena/business-partner"
        const val namePath = "$businessPartnerPath/name"
        const val legalFormPath = "$businessPartnerPath/legal-form"
        const val statusPath = "$businessPartnerPath/status"
        const val classificationPath = "$businessPartnerPath/classification"

        const val addressPath = "$businessPartnerPath/address"
        const val adminAreaPath = "$addressPath/administrative-area"
        const val postCodePath = "$addressPath/postcode"
        const val localityPath = "$addressPath/locality"
        const val thoroughfarePath = "$addressPath/thoroughfare"
        const val premisePath = "$addressPath/premise"
        const val postalDeliveryPointPath = "$addressPath/postal-delivery-point"
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
            WireMock.get(WireMock.urlPathMatching("$CDQ_MOCK_URL/businesspartners")).willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(objectMapper.writeValueAsString(importCollection))
            )
        )

        importService.import()
        elasticSyncService.exportPartnersToElastic()
    }

    @AfterEach
    fun afterEach() {
        testHelpers.truncateH2()
        elasticSyncService.clearElastic()
    }


    @Test
    fun `name__Suggest property values`() {
        val expectedName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get().uri(namePath)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedName }
    }

    @Test
    fun `name__Suggest by phrase`() {
        val expectedName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(namePath)
                    .queryParam(queryTextParameterName, expectedName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedName }
    }

    @Test
    fun `name__Suggest by prefix`() {
        val expectedName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(namePath)
                    .queryParam(queryTextParameterName, expectedName.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedName }
    }

    @Test
    fun `name__Suggest by word`() {
        val expectedName = CdqTestValues.businessPartner1.names.first().value
        val queryText = expectedName.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(namePath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedName }
    }

    @Test
    fun `name__Don't suggest by different`() {
        val expectedName = CdqTestValues.businessPartner1.names.first().value
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(namePath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedName }
    }

    @Test
    fun `name__Suggest filtered suggestions`() {
        val expectedName = CdqTestValues.businessPartner1.names.first().value
        val filterLegalForm = CdqTestValues.businessPartner1.legalForm!!.name

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(namePath)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::legalForm.name, filterLegalForm)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedName }
    }

    @Test
    fun `name__Suggest by word in filtered suggestions`() {
        val expectedName = CdqTestValues.businessPartner1.names.first().value
        val filterLegalForm = CdqTestValues.businessPartner1.legalForm!!.name

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(namePath)
                    .queryParam(queryTextParameterName, expectedName)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::legalForm.name, filterLegalForm)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedName }
    }

    @Test
    fun `name__Don't suggest by word when filtered out`() {
        val expectedName = CdqTestValues.businessPartner1.names.first().value
        val filterLegalForm = CdqTestValues.businessPartner2.legalForm!!.name

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(namePath)
                    .queryParam(queryTextParameterName, expectedName)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::legalForm.name, filterLegalForm)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedName }
    }

    @Test
    fun `name__Suggest by non-latin characters`() {
        val expectedName = CdqTestValues.businessPartner3.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(namePath)
                    .queryParam(queryTextParameterName, expectedName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedName }
    }

    @Test
    fun `legalForm__Suggest property values`() {
        val expectedLegalForm = CdqTestValues.businessPartner1.legalForm!!.name

        val page = webTestClient.get().uri(legalFormPath)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLegalForm }
    }

    @Test
    fun `legalForm__Suggest by phrase`() {
        val expectedLegalForm = CdqTestValues.businessPartner1.legalForm!!.name

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(legalFormPath)
                    .queryParam(queryTextParameterName, expectedLegalForm)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLegalForm }
    }

    @Test
    fun `legalForm__Suggest by prefix`() {
        val expectedLegalForm = CdqTestValues.businessPartner1.legalForm!!.name

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(legalFormPath)
                    .queryParam(queryTextParameterName, expectedLegalForm.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLegalForm }
    }

    @Test
    fun `legalForm__Suggest by word`() {
        val expectedLegalForm = CdqTestValues.businessPartner1.legalForm!!.name
        val queryText = expectedLegalForm.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(legalFormPath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLegalForm }
    }

    @Test
    fun `legalForm__Don't suggest by different`() {
        val expectedLegalForm = CdqTestValues.businessPartner1.legalForm!!.name
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(legalFormPath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedLegalForm }
    }

    @Test
    fun `legalForm__Suggest filtered suggestions`() {
        val expectedLegalForm = CdqTestValues.businessPartner1.legalForm!!.name
        val filterLegalForm = CdqTestValues.businessPartner1.legalForm!!.name

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(legalFormPath)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::legalForm.name, filterLegalForm)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLegalForm }
    }

    @Test
    fun `legalForm__Suggest by word in filtered suggestions`() {
        val expectedLegalForm = CdqTestValues.businessPartner1.legalForm!!.name
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(legalFormPath)
                    .queryParam(queryTextParameterName, expectedLegalForm)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLegalForm }
    }

    @Test
    fun `legalForm__Don't suggest by word when filtered out`() {
        val expectedLegalForm = CdqTestValues.businessPartner1.legalForm!!.name
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(legalFormPath)
                    .queryParam(queryTextParameterName, expectedLegalForm)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedLegalForm }
    }

    @Test
    fun `legalForm__Suggest by non-latin characters`() {
        val expectedLegalForm = CdqTestValues.businessPartner3.legalForm!!.name

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(legalFormPath)
                    .queryParam(queryTextParameterName, expectedLegalForm)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLegalForm }
    }

    @Test
    fun `status__Suggest by phrase`() {
        val expectedStatus = CdqTestValues.businessPartner1.status!!.officialDenotation

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(statusPath)
                    .queryParam(queryTextParameterName, expectedStatus)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedStatus }
    }

    @Test
    fun `status__Suggest by prefix`() {
        val expectedStatus = CdqTestValues.businessPartner1.status!!.officialDenotation

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(statusPath)
                    .queryParam(queryTextParameterName, expectedStatus.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedStatus }
    }

    @Test
    fun `status__Suggest by word`() {
        val expectedStatus = CdqTestValues.businessPartner1.status!!.officialDenotation
        val queryText = expectedStatus.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(statusPath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedStatus }
    }

    @Test
    fun `status__Don't suggest by different`() {
        val expectedStatus = CdqTestValues.businessPartner1.status!!.officialDenotation
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(statusPath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedStatus }
    }

    @Test
    fun `status__Suggest filtered suggestions`() {
        val expectedStatus = CdqTestValues.businessPartner1.status!!.officialDenotation
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(statusPath)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedStatus }
    }

    @Test
    fun `status__Suggest by word in filtered suggestions`() {
        val expectedStatus = CdqTestValues.businessPartner1.status!!.officialDenotation
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(statusPath)
                    .queryParam(queryTextParameterName, expectedStatus)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedStatus }
    }

    @Test
    fun `status__Don't suggest by word when filtered out`() {
        val expectedStatus = CdqTestValues.businessPartner1.status!!.officialDenotation
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(statusPath)
                    .queryParam(queryTextParameterName, expectedStatus)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedStatus }
    }

    @Test
    fun `status__Suggest by non-latin characters`() {
        val expectedStatus = CdqTestValues.businessPartner3.status!!.officialDenotation

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(statusPath)
                    .queryParam(queryTextParameterName, expectedStatus)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedStatus }
    }

    @Test
    fun `classification__Suggest property values`() {
        val expectedClassification = CdqTestValues.businessPartner1.profile!!.classifications.first().value

        val page = webTestClient.get().uri(classificationPath)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedClassification }
    }

    @Test
    fun `classification__Suggest by phrase`() {
        val expectedClassification = CdqTestValues.businessPartner1.profile!!.classifications.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(classificationPath)
                    .queryParam(queryTextParameterName, expectedClassification)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedClassification }
    }

    @Test
    fun `classification__Suggest by prefix`() {
        val expectedClassification = CdqTestValues.businessPartner1.profile!!.classifications.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(classificationPath)
                    .queryParam(queryTextParameterName, expectedClassification.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedClassification }
    }

    @Test
    fun `classification__Suggest by word`() {
        val expectedClassification = CdqTestValues.businessPartner1.profile!!.classifications.first().value
        val queryText = expectedClassification.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(classificationPath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedClassification }
    }

    @Test
    fun `classification__Don't suggest by different`() {
        val expectedClassification = CdqTestValues.businessPartner1.profile!!.classifications.first().value
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(classificationPath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedClassification }
    }

    @Test
    fun `classification__Suggest filtered suggestions`() {
        val expectedClassification = CdqTestValues.businessPartner1.profile!!.classifications.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(classificationPath)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedClassification }
    }

    @Test
    fun `classification__Suggest by word in filtered suggestions`() {
        val expectedClassification = CdqTestValues.businessPartner1.profile!!.classifications.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(classificationPath)
                    .queryParam(queryTextParameterName, expectedClassification)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedClassification }
    }

    @Test
    fun `classification__Don't suggest by word when filtered out`() {
        val expectedClassification = CdqTestValues.businessPartner1.profile!!.classifications.first().value
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(classificationPath)
                    .queryParam(queryTextParameterName, expectedClassification)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedClassification }
    }

    @Test
    fun `classification__Suggest by non-latin characters`() {
        val expectedClassification = CdqTestValues.businessPartner3.profile!!.classifications.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(classificationPath)
                    .queryParam(queryTextParameterName, expectedClassification)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedClassification }
    }

    @Test
    fun `adminArea__Suggest property values`() {
        val expectedAdminArea = CdqTestValues.businessPartner1.addresses.first().administrativeAreas.first().value

        val page = webTestClient.get().uri(adminAreaPath)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedAdminArea }
    }

    @Test
    fun `adminArea__Suggest by phrase`() {
        val expectedAdminArea = CdqTestValues.businessPartner1.addresses.first().administrativeAreas.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(adminAreaPath)
                    .queryParam(queryTextParameterName, expectedAdminArea)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedAdminArea }
    }

    @Test
    fun `adminArea__Suggest by prefix`() {
        val expectedAdminArea = CdqTestValues.businessPartner1.addresses.first().administrativeAreas.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(adminAreaPath)
                    .queryParam(queryTextParameterName, expectedAdminArea.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedAdminArea }
    }

    @Test
    fun `adminArea__Suggest by word`() {
        val expectedAdminArea = CdqTestValues.businessPartner1.addresses.first().administrativeAreas.first().value
        val queryText = expectedAdminArea.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(adminAreaPath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedAdminArea }
    }

    @Test
    fun `adminArea__Don't suggest by different`() {
        val expectedAdminArea = CdqTestValues.businessPartner1.addresses.first().administrativeAreas.first().value
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(adminAreaPath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedAdminArea }
    }

    @Test
    fun `adminArea__Suggest filtered suggestions`() {
        val expectedAdminArea = CdqTestValues.businessPartner1.addresses.first().administrativeAreas.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(adminAreaPath)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedAdminArea }
    }

    @Test
    fun `adminArea__Suggest by word in filtered suggestions`() {
        val expectedAdminArea = CdqTestValues.businessPartner1.addresses.first().administrativeAreas.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(adminAreaPath)
                    .queryParam(queryTextParameterName, expectedAdminArea)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedAdminArea }
    }

    @Test
    fun `adminArea__Don't suggest by word when filtered out`() {
        val expectedAdminArea = CdqTestValues.businessPartner1.addresses.first().administrativeAreas.first().value
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(adminAreaPath)
                    .queryParam(queryTextParameterName, expectedAdminArea)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedAdminArea }
    }

    @Test
    fun `adminArea__Suggest by non-latin characters`() {
        val expectedAdminArea = CdqTestValues.businessPartner3.addresses.first().administrativeAreas.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(adminAreaPath)
                    .queryParam(queryTextParameterName, expectedAdminArea)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedAdminArea }
    }

    @Test
    fun `postCode__Suggest property values`() {
        val expectedPostCode = CdqTestValues.businessPartner1.addresses.first().postCodes.first().value

        val page = webTestClient.get().uri(postCodePath)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostCode }
    }

    @Test
    fun `postCode__Suggest by phrase`() {
        val expectedPostCode = CdqTestValues.businessPartner1.addresses.first().postCodes.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(postCodePath)
                    .queryParam(queryTextParameterName, expectedPostCode)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostCode }
    }

    @Test
    fun `postCode__Suggest by prefix`() {
        val expectedPostCode = CdqTestValues.businessPartner1.addresses.first().postCodes.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(postCodePath)
                    .queryParam(queryTextParameterName, expectedPostCode.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostCode }
    }

    @Test
    fun `postCode__Suggest by word`() {
        val expectedPostCode = CdqTestValues.businessPartner1.addresses.first().postCodes.first().value
        val queryText = expectedPostCode.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(postCodePath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostCode }
    }

    @Test
    fun `postCode__Don't suggest by different`() {
        val expectedPostCode = CdqTestValues.businessPartner1.addresses.first().postCodes.first().value
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(postCodePath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedPostCode }
    }

    @Test
    fun `postCode__Suggest filtered suggestions`() {
        val expectedPostCode = CdqTestValues.businessPartner1.addresses.first().postCodes.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(postCodePath)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostCode }
    }

    @Test
    fun `postCode__Suggest by word in filtered suggestions`() {
        val expectedPostCode = CdqTestValues.businessPartner1.addresses.first().postCodes.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(postCodePath)
                    .queryParam(queryTextParameterName, expectedPostCode)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostCode }
    }

    @Test
    fun `postCode__Don't suggest by word when filtered out`() {
        val expectedPostCode = CdqTestValues.businessPartner1.addresses.first().postCodes.first().value
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(postCodePath)
                    .queryParam(queryTextParameterName, expectedPostCode)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedPostCode }
    }

    @Test
    fun `postCode__Suggest by non-latin characters`() {
        val expectedPostCode = CdqTestValues.businessPartner3.addresses.first().postCodes.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(postCodePath)
                    .queryParam(queryTextParameterName, expectedPostCode)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostCode }
    }

    @Test
    fun `locality__Suggest property values`() {
        val expectedLocality = CdqTestValues.businessPartner1.addresses.first().localities.first().value

        val page = webTestClient.get().uri(localityPath)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLocality }
    }

    @Test
    fun `locality__Suggest by phrase`() {
        val expectedLocality = CdqTestValues.businessPartner1.addresses.first().localities.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(localityPath)
                    .queryParam(queryTextParameterName, expectedLocality)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLocality }
    }

    @Test
    fun `locality__Suggest by prefix`() {
        val expectedLocality = CdqTestValues.businessPartner1.addresses.first().localities.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(localityPath)
                    .queryParam(queryTextParameterName, expectedLocality.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLocality }
    }

    @Test
    fun `locality__Suggest by word`() {
        val expectedLocality = CdqTestValues.businessPartner1.addresses.first().localities.first().value
        val queryText = expectedLocality.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(localityPath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLocality }
    }

    @Test
    fun `locality__Don't suggest by different`() {
        val expectedLocality = CdqTestValues.businessPartner1.addresses.first().localities.first().value
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(localityPath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedLocality }
    }

    @Test
    fun `locality__Suggest filtered suggestions`() {
        val expectedLocality = CdqTestValues.businessPartner1.addresses.first().localities.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(localityPath)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLocality }
    }

    @Test
    fun `locality__Suggest by word in filtered suggestions`() {
        val expectedLocality = CdqTestValues.businessPartner1.addresses.first().localities.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(localityPath)
                    .queryParam(queryTextParameterName, expectedLocality)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLocality }
    }

    @Test
    fun `locality__Don't suggest by word when filtered out`() {
        val expectedLocality = CdqTestValues.businessPartner1.addresses.first().localities.first().value
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(localityPath)
                    .queryParam(queryTextParameterName, expectedLocality)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedLocality }
    }

    @Test
    fun `locality__Suggest by non-latin characters`() {
        val expectedLocality = CdqTestValues.businessPartner3.addresses.first().localities.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(localityPath)
                    .queryParam(queryTextParameterName, expectedLocality)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLocality }
    }

    @Test
    fun `thoroughfare__Suggest property values`() {
        val expectedThoroughfare = CdqTestValues.businessPartner1.addresses.first().thoroughfares.first().value

        val page = webTestClient.get().uri(thoroughfarePath)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedThoroughfare }
    }

    @Test
    fun `thoroughfare__Suggest by phrase`() {
        val expectedThoroughfare = CdqTestValues.businessPartner1.addresses.first().thoroughfares.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(thoroughfarePath)
                    .queryParam(queryTextParameterName, expectedThoroughfare)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedThoroughfare }
    }

    @Test
    fun `thoroughfare__Suggest by prefix`() {
        val expectedThoroughfare = CdqTestValues.businessPartner1.addresses.first().thoroughfares.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(thoroughfarePath)
                    .queryParam(queryTextParameterName, expectedThoroughfare.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedThoroughfare }
    }

    @Test
    fun `thoroughfare__Suggest by word`() {
        val expectedThoroughfare = CdqTestValues.businessPartner1.addresses.first().thoroughfares.first().value
        val queryText = expectedThoroughfare.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(thoroughfarePath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedThoroughfare }
    }

    @Test
    fun `thoroughfare__Don't suggest by different`() {
        val expectedThoroughfare = CdqTestValues.businessPartner1.addresses.first().thoroughfares.first().value
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(thoroughfarePath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedThoroughfare }
    }

    @Test
    fun `thoroughfare__Suggest filtered suggestions`() {
        val expectedThoroughfare = CdqTestValues.businessPartner1.addresses.first().thoroughfares.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(thoroughfarePath)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedThoroughfare }
    }

    @Test
    fun `thoroughfare__Suggest by word in filtered suggestions`() {
        val expectedThoroughfare = CdqTestValues.businessPartner1.addresses.first().thoroughfares.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(thoroughfarePath)
                    .queryParam(queryTextParameterName, expectedThoroughfare)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedThoroughfare }
    }

    @Test
    fun `thoroughfare__Don't suggest by word when filtered out`() {
        val expectedThoroughfare = CdqTestValues.businessPartner1.addresses.first().thoroughfares.first().value
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(thoroughfarePath)
                    .queryParam(queryTextParameterName, expectedThoroughfare)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedThoroughfare }
    }

    @Test
    fun `thoroughfare__Suggest by non-latin characters`() {
        val expectedThoroughfare = CdqTestValues.businessPartner3.addresses.first().thoroughfares.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(thoroughfarePath)
                    .queryParam(queryTextParameterName, expectedThoroughfare)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedThoroughfare }
    }

    @Test
    fun `premise__Suggest property values`() {
        val expectedPremise = CdqTestValues.businessPartner1.addresses.first().premises.first().value

        val page = webTestClient.get().uri(premisePath)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPremise }
    }

    @Test
    fun `premise__Suggest by phrase`() {
        val expectedPremise = CdqTestValues.businessPartner1.addresses.first().premises.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(premisePath)
                    .queryParam(queryTextParameterName, expectedPremise)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPremise }
    }

    @Test
    fun `premise__Suggest by prefix`() {
        val expectedPremise = CdqTestValues.businessPartner1.addresses.first().premises.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(premisePath)
                    .queryParam(queryTextParameterName, expectedPremise.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPremise }
    }

    @Test
    fun `premise__Suggest by word`() {
        val expectedPremise = CdqTestValues.businessPartner1.addresses.first().premises.first().value
        val queryText = expectedPremise.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(premisePath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPremise }
    }

    @Test
    fun `premise__Don't suggest by different`() {
        val expectedPremise = CdqTestValues.businessPartner1.addresses.first().premises.first().value
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(premisePath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedPremise }
    }

    @Test
    fun `premise__Suggest filtered suggestions`() {
        val expectedPremise = CdqTestValues.businessPartner1.addresses.first().premises.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(premisePath)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPremise }
    }

    @Test
    fun `premise__Suggest by word in filtered suggestions`() {
        val expectedPremise = CdqTestValues.businessPartner1.addresses.first().premises.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(premisePath)
                    .queryParam(queryTextParameterName, expectedPremise)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPremise }
    }

    @Test
    fun `premise__Don't suggest by word when filtered out`() {
        val expectedPremise = CdqTestValues.businessPartner1.addresses.first().premises.first().value
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(premisePath)
                    .queryParam(queryTextParameterName, expectedPremise)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedPremise }
    }

    @Test
    fun `premise__Suggest by non-latin characters`() {
        val expectedPremise = CdqTestValues.businessPartner3.addresses.first().premises.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(premisePath)
                    .queryParam(queryTextParameterName, expectedPremise)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPremise }
    }

    @Test
    fun `postalDeliveryPoint__Suggest by phrase`() {
        val expectedPostalDeliveryPoint = CdqTestValues.businessPartner1.addresses.first().postalDeliveryPoints.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(postalDeliveryPointPath)
                    .queryParam(queryTextParameterName, expectedPostalDeliveryPoint)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostalDeliveryPoint }
    }

    @Test
    fun `postalDeliveryPoint__Suggest by prefix`() {
        val expectedPostalDeliveryPoint = CdqTestValues.businessPartner1.addresses.first().postalDeliveryPoints.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(postalDeliveryPointPath)
                    .queryParam(queryTextParameterName, expectedPostalDeliveryPoint.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostalDeliveryPoint }
    }

    @Test
    fun `postalDeliveryPoint__Suggest by word`() {
        val expectedPostalDeliveryPoint = CdqTestValues.businessPartner1.addresses.first().postalDeliveryPoints.first().value
        val queryText = expectedPostalDeliveryPoint.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(postalDeliveryPointPath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostalDeliveryPoint }
    }

    @Test
    fun `postalDeliveryPoint__Don't suggest by different`() {
        val expectedPostalDeliveryPoint = CdqTestValues.businessPartner1.addresses.first().postalDeliveryPoints.first().value
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(postalDeliveryPointPath)
                    .queryParam(queryTextParameterName, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedPostalDeliveryPoint }
    }

    @Test
    fun `postalDeliveryPoint__Suggest filtered suggestions`() {
        val expectedPostalDeliveryPoint = CdqTestValues.businessPartner1.addresses.first().postalDeliveryPoints.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(postalDeliveryPointPath)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostalDeliveryPoint }
    }

    @Test
    fun `postalDeliveryPoint__Suggest by word in filtered suggestions`() {
        val expectedPostalDeliveryPoint = CdqTestValues.businessPartner1.addresses.first().postalDeliveryPoints.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(postalDeliveryPointPath)
                    .queryParam(queryTextParameterName, expectedPostalDeliveryPoint)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostalDeliveryPoint }
    }

    @Test
    fun `postalDeliveryPoint__Don't suggest by word when filtered out`() {
        val expectedPostalDeliveryPoint = CdqTestValues.businessPartner1.addresses.first().postalDeliveryPoints.first().value
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(postalDeliveryPointPath)
                    .queryParam(queryTextParameterName, expectedPostalDeliveryPoint)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedPostalDeliveryPoint }
    }

    @Test
    fun `postalDeliveryPoint__Suggest by non-latin characters`() {
        val expectedPostalDeliveryPoint = CdqTestValues.businessPartner3.addresses.first().postalDeliveryPoints.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(postalDeliveryPointPath)
                    .queryParam(queryTextParameterName, expectedPostalDeliveryPoint)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostalDeliveryPoint }
    }
}