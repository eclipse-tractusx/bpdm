package com.catenax.gpdm.controller

import com.catenax.gpdm.Application
import com.catenax.gpdm.component.cdq.dto.BusinessPartnerCollectionCdq
import com.catenax.gpdm.component.cdq.service.PartnerImportService
import com.catenax.gpdm.component.elastic.impl.service.ElasticSyncService
import com.catenax.gpdm.dto.request.BusinessPartnerPropertiesSearchRequest
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.dto.response.SuggestionResponse
import com.catenax.gpdm.util.CdqTestValues
import com.catenax.gpdm.util.ElasticsearchContainer
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
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class],
    properties = ["bpdm.elastic.enabled=true"]
)
@ActiveProfiles(value = ["test"])
class BusinessPartnerControllerSuggestionIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val importService: PartnerImportService,
    val elasticSyncService: ElasticSyncService,
    val objectMapper: ObjectMapper,
    val testHelpers: TestHelpers
) {

    companion object {

        private val elasticsearchContainer = ElasticsearchContainer.instance

        @RegisterExtension
        var wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.cdq.host") { wireMockServer.baseUrl() }
            registry.add("spring.elasticsearch.uris", elasticsearchContainer::getHttpHostAddress)
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
            WireMock.get(WireMock.urlPathMatching(EndpointValues.CDQ_MOCK_BUSINESS_PARTNER_PATH)).willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(objectMapper.writeValueAsString(importCollection))
            )
        )

        importService.importAsync()
        elasticSyncService.exportPartnersToElastic()
    }

    @AfterEach
    fun afterEach() {
        testHelpers.truncateH2()
        elasticSyncService.clearElastic()
    }


    /**
     * Given partner with name value
     * When ask suggestion for name
     * Then show name value
     */
    @Test
    fun `name__Suggest property values`() {
        val expectedName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get().uri(EndpointValues.CATENA_NAME_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedName }
    }

    /**
     * Given partner with name value
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `name__Suggest by phrase`() {
        val expectedName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_NAME_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedName }
    }

    /**
     * Given partner with name value
     * When ask suggestion for a prefix of that value
     * Then show that value
     */
    @Test
    fun `name__Suggest by prefix`() {
        val expectedName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_NAME_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedName.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedName }
    }

    /**
     * Given partner with name value that is several words
     * When ask suggestion for a word in value
     * Then show that value
     */
    @Test
    fun `name__Suggest by word`() {
        val expectedName = CdqTestValues.businessPartner1.names.first().value
        val queryText = expectedName.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_NAME_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedName }
    }

    /**
     * Given partner with name value
     * When ask suggestion for text that doesn't have a word or prefix in value
     * Then don't show that value
     */
    @Test
    fun `name__Don't suggest by different`() {
        val expectedName = CdqTestValues.businessPartner1.names.first().value
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_NAME_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedName }
    }

    /**
     * Given partner with name value and legal form value
     * When ask suggestion name with filter by legal form value
     * Then show name value
     */
    @Test
    fun `name__Suggest filtered suggestions`() {
        val expectedName = CdqTestValues.businessPartner1.names.first().value
        val filterLegalForm = CdqTestValues.businessPartner1.legalForm!!.name

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_NAME_PATH)
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

    /**
     * Given partner with name value and legal form value
     * When ask suggestion for a word in name value with filter by legal form value
     * Then show name value
     */
    @Test
    fun `name__Suggest by word in filtered suggestions`() {
        val expectedName = CdqTestValues.businessPartner1.names.first().value
        val filterLegalForm = CdqTestValues.businessPartner1.legalForm!!.name

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_NAME_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedName)
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

    /**
     * Given partner with name value and legal form value
     * When ask suggestion for a word in name value with filter by other than legal form value
     * Then don't show name value
     */
    @Test
    fun `name__Don't suggest by word when filtered out`() {
        val expectedName = CdqTestValues.businessPartner1.names.first().value
        val filterLegalForm = CdqTestValues.businessPartner2.legalForm!!.name

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_NAME_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedName)
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

    /**
     * Given partner with name value in non-latin characters
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `name__Suggest by non-latin characters`() {
        val expectedName = CdqTestValues.businessPartner3.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_NAME_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedName }
    }


    /**
     * Given partner with legal form value
     * When ask suggestion for legal form
     * Then show legal form value
     */
    @Test
    fun `legalForm__Suggest property values`() {
        val expectedLegalForm = CdqTestValues.businessPartner1.legalForm!!.name

        val page = webTestClient.get().uri(EndpointValues.CATENA_LEGAL_FORM_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLegalForm }
    }

    /**
     * Given partner with legal form value
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `legalForm__Suggest by phrase`() {
        val expectedLegalForm = CdqTestValues.businessPartner1.legalForm!!.name

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_LEGAL_FORM_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedLegalForm)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLegalForm }
    }

    /**
     * Given partner with legal form value
     * When ask suggestion for a prefix of that value
     * Then show that value
     */
    @Test
    fun `legalForm__Suggest by prefix`() {
        val expectedLegalForm = CdqTestValues.businessPartner1.legalForm!!.name

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_LEGAL_FORM_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedLegalForm.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLegalForm }
    }

    /**
     * Given partner with legal form value that is several words
     * When ask suggestion for a word in value
     * Then show that value
     */
    @Test
    fun `legalForm__Suggest by word`() {
        val expectedLegalForm = CdqTestValues.businessPartner1.legalForm!!.name
        val queryText = expectedLegalForm.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_LEGAL_FORM_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLegalForm }
    }

    /**
     * Given partner with legal form value
     * When ask suggestion for text that doesn't have a word or prefix in value
     * Then don't show that value
     */
    @Test
    fun `legalForm__Don't suggest by different`() {
        val expectedLegalForm = CdqTestValues.businessPartner1.legalForm!!.name
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_LEGAL_FORM_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedLegalForm }
    }

    /**
     * Given partner with legal form value and name value
     * When ask suggestion legal form with filter by name value
     * Then show legal form value
     */
    @Test
    fun `legalForm__Suggest filtered suggestions`() {
        val expectedLegalForm = CdqTestValues.businessPartner1.legalForm!!.name
        val filterLegalForm = CdqTestValues.businessPartner1.legalForm!!.name

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_LEGAL_FORM_PATH)
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

    /**
     * Given partner with legal form value and name value
     * When ask suggestion for a word in legal form value with filter by name value
     * Then show legal form value
     */
    @Test
    fun `legalForm__Suggest by word in filtered suggestions`() {
        val expectedLegalForm = CdqTestValues.businessPartner1.legalForm!!.name
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_LEGAL_FORM_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedLegalForm)
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

    /**
     * Given partner with legal form value and name value
     * When ask suggestion for a word in legal form value with filter by other than name value
     * Then don't show legal form value
     */
    @Test
    fun `legalForm__Don't suggest by word when filtered out`() {
        val expectedLegalForm = CdqTestValues.businessPartner1.legalForm!!.name
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_LEGAL_FORM_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedLegalForm)
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

    /**
     * Given partner with legal form value in non-latin characters
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `legalForm__Suggest by non-latin characters`() {
        val expectedLegalForm = CdqTestValues.businessPartner3.legalForm!!.name

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_LEGAL_FORM_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedLegalForm)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLegalForm }
    }

    /**
     * Given partner with status value
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `status__Suggest by phrase`() {
        val expectedStatus = CdqTestValues.businessPartner1.status!!.officialDenotation

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_STATUS_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedStatus)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedStatus }
    }

    /**
     * Given partner with status value
     * When ask suggestion for a prefix of that value
     * Then show that value
     */
    @Test
    fun `status__Suggest by prefix`() {
        val expectedStatus = CdqTestValues.businessPartner1.status!!.officialDenotation

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_STATUS_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedStatus.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedStatus }
    }

    /**
     * Given partner with status value that is several words
     * When ask suggestion for a word in value
     * Then show that value
     */
    @Test
    fun `status__Suggest by word`() {
        val expectedStatus = CdqTestValues.businessPartner1.status!!.officialDenotation
        val queryText = expectedStatus.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_STATUS_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedStatus }
    }

    /**
     * Given partner with status value
     * When ask suggestion for text that doesn't have a word or prefix in value
     * Then don't show that value
     */
    @Test
    fun `status__Don't suggest by different`() {
        val expectedStatus = CdqTestValues.businessPartner1.status!!.officialDenotation
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_STATUS_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedStatus }
    }

    /**
     * Given partner with status value and name value
     * When ask suggestion status with filter by name value
     * Then show status value
     */
    @Test
    fun `status__Suggest filtered suggestions`() {
        val expectedStatus = CdqTestValues.businessPartner1.status!!.officialDenotation
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_STATUS_PATH)
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

    /**
     * Given partner with status value and name value
     * When ask suggestion for a word in status value with filter by name value
     * Then show status value
     */
    @Test
    fun `status__Suggest by word in filtered suggestions`() {
        val expectedStatus = CdqTestValues.businessPartner1.status!!.officialDenotation
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_STATUS_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedStatus)
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

    /**
     * Given partner with status value and name value
     * When ask suggestion for a word in status value with filter by other than name value
     * Then don't show status value
     */
    @Test
    fun `status__Don't suggest by word when filtered out`() {
        val expectedStatus = CdqTestValues.businessPartner1.status!!.officialDenotation
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_STATUS_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedStatus)
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

    /**
     * Given partner with status value in non-latin characters
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `status__Suggest by non-latin characters`() {
        val expectedStatus = CdqTestValues.businessPartner3.status!!.officialDenotation

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_STATUS_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedStatus)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedStatus }
    }

    /**
     * Given partner with classification value
     * When ask suggestion for classification
     * Then show classification value
     */
    @Test
    fun `classification__Suggest property values`() {
        val expectedClassification = CdqTestValues.businessPartner1.profile!!.classifications.first().value

        val page = webTestClient.get().uri(EndpointValues.CATENA_CLASSIFICATION_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedClassification }
    }

    /**
     * Given partner with classification value
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `classification__Suggest by phrase`() {
        val expectedClassification = CdqTestValues.businessPartner1.profile!!.classifications.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_CLASSIFICATION_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedClassification)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedClassification }
    }

    /**
     * Given partner with classification value
     * When ask suggestion for a prefix of that value
     * Then show that value
     */
    @Test
    fun `classification__Suggest by prefix`() {
        val expectedClassification = CdqTestValues.businessPartner1.profile!!.classifications.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_CLASSIFICATION_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedClassification.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedClassification }
    }

    /**
     * Given partner with classification value that is several words
     * When ask suggestion for a word in value
     * Then show that value
     */
    @Test
    fun `classification__Suggest by word`() {
        val expectedClassification = CdqTestValues.businessPartner1.profile!!.classifications.first().value
        val queryText = expectedClassification.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_CLASSIFICATION_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedClassification }
    }

    /**
     * Given partner with classification value
     * When ask suggestion for text that doesn't have a word or prefix in value
     * Then don't show that value
     */
    @Test
    fun `classification__Don't suggest by different`() {
        val expectedClassification = CdqTestValues.businessPartner1.profile!!.classifications.first().value
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_CLASSIFICATION_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedClassification }
    }

    /**
     * Given partner with classification value and name value
     * When ask suggestion classification with filter by name value
     * Then show classification value
     */
    @Test
    fun `classification__Suggest filtered suggestions`() {
        val expectedClassification = CdqTestValues.businessPartner1.profile!!.classifications.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_CLASSIFICATION_PATH)
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

    /**
     * Given partner with classification value and name value
     * When ask suggestion for a word in classification value with filter by name value
     * Then show classification value
     */
    @Test
    fun `classification__Suggest by word in filtered suggestions`() {
        val expectedClassification = CdqTestValues.businessPartner1.profile!!.classifications.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_CLASSIFICATION_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedClassification)
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

    /**
     * Given partner with classification value and name value
     * When ask suggestion for a word in classification value with filter by other than name value
     * Then don't show classification value
     */
    @Test
    fun `classification__Don't suggest by word when filtered out`() {
        val expectedClassification = CdqTestValues.businessPartner1.profile!!.classifications.first().value
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_CLASSIFICATION_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedClassification)
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

    /**
     * Given partner with classification value in non-latin characters
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `classification__Suggest by non-latin characters`() {
        val expectedClassification = CdqTestValues.businessPartner3.profile!!.classifications.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_CLASSIFICATION_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedClassification)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedClassification }
    }

    /**
     * Given partner with administrative area value
     * When ask suggestion for administrative area
     * Then show administrative area value
     */
    @Test
    fun `adminArea__Suggest property values`() {
        val expectedAdminArea = CdqTestValues.businessPartner1.addresses.first().administrativeAreas.first().value

        val page = webTestClient.get().uri(EndpointValues.CATENA_ADMIN_AREA_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedAdminArea }
    }

    /**
     * Given partner with administrative area value
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `adminArea__Suggest by phrase`() {
        val expectedAdminArea = CdqTestValues.businessPartner1.addresses.first().administrativeAreas.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_ADMIN_AREA_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedAdminArea)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedAdminArea }
    }

    /**
     * Given partner with administrative area value
     * When ask suggestion for a prefix of that value
     * Then show that value
     */
    @Test
    fun `adminArea__Suggest by prefix`() {
        val expectedAdminArea = CdqTestValues.businessPartner1.addresses.first().administrativeAreas.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_ADMIN_AREA_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedAdminArea.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedAdminArea }
    }

    /**
     * Given partner with administrative area value that is several words
     * When ask suggestion for a word in value
     * Then show that value
     */
    @Test
    fun `adminArea__Suggest by word`() {
        val expectedAdminArea = CdqTestValues.businessPartner1.addresses.first().administrativeAreas.first().value
        val queryText = expectedAdminArea.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_ADMIN_AREA_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedAdminArea }
    }

    /**
     * Given partner with administrative area value
     * When ask suggestion for text that doesn't have a word or prefix in value
     * Then don't show that value
     */
    @Test
    fun `adminArea__Don't suggest by different`() {
        val expectedAdminArea = CdqTestValues.businessPartner1.addresses.first().administrativeAreas.first().value
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_ADMIN_AREA_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedAdminArea }
    }

    /**
     * Given partner with administrative area value and name value
     * When ask suggestion administrative area with filter by name value
     * Then show administrative area value
     */
    @Test
    fun `adminArea__Suggest filtered suggestions`() {
        val expectedAdminArea = CdqTestValues.businessPartner1.addresses.first().administrativeAreas.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_ADMIN_AREA_PATH)
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

    /**
     * Given partner with administrative area value and name value
     * When ask suggestion for a word in administrative area value with filter by name value
     * Then show administrative area value
     */
    @Test
    fun `adminArea__Suggest by word in filtered suggestions`() {
        val expectedAdminArea = CdqTestValues.businessPartner1.addresses.first().administrativeAreas.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_ADMIN_AREA_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedAdminArea)
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

    /**
     * Given partner with administrative area value and name value
     * When ask suggestion for a word in administrative area value with filter by other than name value
     * Then don't show administrative area value
     */
    @Test
    fun `adminArea__Don't suggest by word when filtered out`() {
        val expectedAdminArea = CdqTestValues.businessPartner1.addresses.first().administrativeAreas.first().value
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_ADMIN_AREA_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedAdminArea)
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

    /**
     * Given partner with administrative area value in non-latin characters
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `adminArea__Suggest by non-latin characters`() {
        val expectedAdminArea = CdqTestValues.businessPartner3.addresses.first().administrativeAreas.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_ADMIN_AREA_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedAdminArea)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedAdminArea }
    }

    /**
     * Given partner with post code value
     * When ask suggestion for post code
     * Then show post code value
     */
    @Test
    fun `postCode__Suggest property values`() {
        val expectedPostCode = CdqTestValues.businessPartner1.addresses.first().postCodes.first().value

        val page = webTestClient.get().uri(EndpointValues.CATENA_POST_CODE_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostCode }
    }

    /**
     * Given partner with post code value
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `postCode__Suggest by phrase`() {
        val expectedPostCode = CdqTestValues.businessPartner1.addresses.first().postCodes.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_POST_CODE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedPostCode)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostCode }
    }

    /**
     * Given partner with post code value
     * When ask suggestion for a prefix of that value
     * Then show that value
     */
    @Test
    fun `postCode__Suggest by prefix`() {
        val expectedPostCode = CdqTestValues.businessPartner1.addresses.first().postCodes.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_POST_CODE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedPostCode.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostCode }
    }

    /**
     * Given partner with post code value that is several words
     * When ask suggestion for a word in value
     * Then show that value
     */
    @Test
    fun `postCode__Suggest by word`() {
        val expectedPostCode = CdqTestValues.businessPartner1.addresses.first().postCodes.first().value
        val queryText = expectedPostCode.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_POST_CODE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostCode }
    }

    /**
     * Given partner with post code value
     * When ask suggestion for text that doesn't have a word or prefix in value
     * Then don't show that value
     */
    @Test
    fun `postCode__Don't suggest by different`() {
        val expectedPostCode = CdqTestValues.businessPartner1.addresses.first().postCodes.first().value
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_POST_CODE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedPostCode }
    }

    /**
     * Given partner with post code value and name value
     * When ask suggestion post code with filter by name value
     * Then show post code value
     */
    @Test
    fun `postCode__Suggest filtered suggestions`() {
        val expectedPostCode = CdqTestValues.businessPartner1.addresses.first().postCodes.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_POST_CODE_PATH)
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

    /**
     * Given partner with post code value and name value
     * When ask suggestion for a word in post code value with filter by name value
     * Then show post code value
     */
    @Test
    fun `postCode__Suggest by word in filtered suggestions`() {
        val expectedPostCode = CdqTestValues.businessPartner1.addresses.first().postCodes.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_POST_CODE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedPostCode)
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

    /**
     * Given partner with post code value and name value
     * When ask suggestion for a word in post code value with filter by other than name value
     * Then don't show post code value
     */
    @Test
    fun `postCode__Don't suggest by word when filtered out`() {
        val expectedPostCode = CdqTestValues.businessPartner1.addresses.first().postCodes.first().value
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_POST_CODE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedPostCode)
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

    /**
     * Given partner with post code value in non-latin characters
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `postCode__Suggest by non-latin characters`() {
        val expectedPostCode = CdqTestValues.businessPartner3.addresses.first().postCodes.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_POST_CODE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedPostCode)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostCode }
    }

    /**
     * Given partner with locality value
     * When ask suggestion for locality
     * Then show locality value
     */
    @Test
    fun `locality__Suggest property values`() {
        val expectedLocality = CdqTestValues.businessPartner1.addresses.first().localities.first().value

        val page = webTestClient.get().uri(EndpointValues.CATENA_LOCALITY_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLocality }
    }

    /**
     * Given partner with locality value
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `locality__Suggest by phrase`() {
        val expectedLocality = CdqTestValues.businessPartner1.addresses.first().localities.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_LOCALITY_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedLocality)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLocality }
    }

    /**
     * Given partner with locality value
     * When ask suggestion for a prefix of that value
     * Then show that value
     */
    @Test
    fun `locality__Suggest by prefix`() {
        val expectedLocality = CdqTestValues.businessPartner1.addresses.first().localities.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_LOCALITY_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedLocality.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLocality }
    }

    /**
     * Given partner with locality value that is several words
     * When ask suggestion for a word in value
     * Then show that value
     */
    @Test
    fun `locality__Suggest by word`() {
        val expectedLocality = CdqTestValues.businessPartner1.addresses.first().localities.first().value
        val queryText = expectedLocality.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_LOCALITY_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLocality }
    }

    /**
     * Given partner with locality value
     * When ask suggestion for text that doesn't have a word or prefix in value
     * Then don't show that value
     */
    @Test
    fun `locality__Don't suggest by different`() {
        val expectedLocality = CdqTestValues.businessPartner1.addresses.first().localities.first().value
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_LOCALITY_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedLocality }
    }

    /**
     * Given partner with locality value and name value
     * When ask suggestion locality with filter by name value
     * Then show locality value
     */
    @Test
    fun `locality__Suggest filtered suggestions`() {
        val expectedLocality = CdqTestValues.businessPartner1.addresses.first().localities.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_LOCALITY_PATH)
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

    /**
     * Given partner with locality value and name value
     * When ask suggestion for a word in locality value with filter by name value
     * Then show locality value
     */
    @Test
    fun `locality__Suggest by word in filtered suggestions`() {
        val expectedLocality = CdqTestValues.businessPartner1.addresses.first().localities.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_LOCALITY_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedLocality)
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

    /**
     * Given partner with locality value and name value
     * When ask suggestion for a word in locality value with filter by other than name value
     * Then don't show locality value
     */
    @Test
    fun `locality__Don't suggest by word when filtered out`() {
        val expectedLocality = CdqTestValues.businessPartner1.addresses.first().localities.first().value
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_LOCALITY_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedLocality)
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

    /**
     * Given partner with locality value in non-latin characters
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `locality__Suggest by non-latin characters`() {
        val expectedLocality = CdqTestValues.businessPartner3.addresses.first().localities.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_LOCALITY_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedLocality)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedLocality }
    }

    /**
     * Given partner with thoroughfare value
     * When ask suggestion for thoroughfare
     * Then show thoroughfare value
     */
    @Test
    fun `thoroughfare__Suggest property values`() {
        val expectedThoroughfare = CdqTestValues.businessPartner1.addresses.first().thoroughfares.first().value

        val page = webTestClient.get().uri(EndpointValues.CATENA_THOROUGHFARE_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedThoroughfare }
    }

    /**
     * Given partner with thoroughfare value
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `thoroughfare__Suggest by phrase`() {
        val expectedThoroughfare = CdqTestValues.businessPartner1.addresses.first().thoroughfares.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_THOROUGHFARE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedThoroughfare)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedThoroughfare }
    }

    /**
     * Given partner with thoroughfare value
     * When ask suggestion for a prefix of that value
     * Then show that value
     */
    @Test
    fun `thoroughfare__Suggest by prefix`() {
        val expectedThoroughfare = CdqTestValues.businessPartner1.addresses.first().thoroughfares.first().value!!

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_THOROUGHFARE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedThoroughfare.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedThoroughfare }
    }

    /**
     * Given partner with thoroughfare value that is several words
     * When ask suggestion for a word in value
     * Then show that value
     */
    @Test
    fun `thoroughfare__Suggest by word`() {
        val expectedThoroughfare = CdqTestValues.businessPartner1.addresses.first().thoroughfares.first().value!!
        val queryText = expectedThoroughfare.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_THOROUGHFARE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedThoroughfare }
    }

    /**
     * Given partner with thoroughfare value
     * When ask suggestion for text that doesn't have a word or prefix in value
     * Then don't show that value
     */
    @Test
    fun `thoroughfare__Don't suggest by different`() {
        val expectedThoroughfare = CdqTestValues.businessPartner1.addresses.first().thoroughfares.first().value!!
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_THOROUGHFARE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedThoroughfare }
    }

    /**
     * Given partner with thoroughfare value and name value
     * When ask suggestion thoroughfare with filter by name value
     * Then show thoroughfare value
     */
    @Test
    fun `thoroughfare__Suggest filtered suggestions`() {
        val expectedThoroughfare = CdqTestValues.businessPartner1.addresses.first().thoroughfares.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_THOROUGHFARE_PATH)
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

    /**
     * Given partner with thoroughfare value and name value
     * When ask suggestion for a word in thoroughfare value with filter by name value
     * Then show thoroughfare value
     */
    @Test
    fun `thoroughfare__Suggest by word in filtered suggestions`() {
        val expectedThoroughfare = CdqTestValues.businessPartner1.addresses.first().thoroughfares.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_THOROUGHFARE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedThoroughfare)
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

    /**
     * Given partner with thoroughfare value and name value
     * When ask suggestion for a word in thoroughfare value with filter by other than name value
     * Then don't show thoroughfare value
     */
    @Test
    fun `thoroughfare__Don't suggest by word when filtered out`() {
        val expectedThoroughfare = CdqTestValues.businessPartner1.addresses.first().thoroughfares.first().value
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_THOROUGHFARE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedThoroughfare)
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

    /**
     * Given partner with thoroughfare value in non-latin characters
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `thoroughfare__Suggest by non-latin characters`() {
        val expectedThoroughfare = CdqTestValues.businessPartner3.addresses.first().thoroughfares.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_THOROUGHFARE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedThoroughfare)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedThoroughfare }
    }

    /**
     * Given partner with premise value
     * When ask suggestion for premise
     * Then show premise value
     */
    @Test
    fun `premise__Suggest property values`() {
        val expectedPremise = CdqTestValues.businessPartner1.addresses.first().premises.first().value

        val page = webTestClient.get().uri(EndpointValues.CATENA_PREMISE_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPremise }
    }

    /**
     * Given partner with premise value
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `premise__Suggest by phrase`() {
        val expectedPremise = CdqTestValues.businessPartner1.addresses.first().premises.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_PREMISE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedPremise)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPremise }
    }

    /**
     * Given partner with premise value
     * When ask suggestion for a prefix of that value
     * Then show that value
     */
    @Test
    fun `premise__Suggest by prefix`() {
        val expectedPremise = CdqTestValues.businessPartner1.addresses.first().premises.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_PREMISE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedPremise.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPremise }
    }

    /**
     * Given partner with premise value that is several words
     * When ask suggestion for a word in value
     * Then show that value
     */
    @Test
    fun `premise__Suggest by word`() {
        val expectedPremise = CdqTestValues.businessPartner1.addresses.first().premises.first().value
        val queryText = expectedPremise.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_PREMISE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPremise }
    }

    /**
     * Given partner with premise value
     * When ask suggestion for text that doesn't have a word or prefix in value
     * Then don't show that value
     */
    @Test
    fun `premise__Don't suggest by different`() {
        val expectedPremise = CdqTestValues.businessPartner1.addresses.first().premises.first().value
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_PREMISE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedPremise }
    }

    /**
     * Given partner with premise value and name value
     * When ask suggestion premise with filter by name value
     * Then show premise value
     */
    @Test
    fun `premise__Suggest filtered suggestions`() {
        val expectedPremise = CdqTestValues.businessPartner1.addresses.first().premises.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_PREMISE_PATH)
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

    /**
     * Given partner with premise value and name value
     * When ask suggestion for a word in premise value with filter by name value
     * Then show premise value
     */
    @Test
    fun `premise__Suggest by word in filtered suggestions`() {
        val expectedPremise = CdqTestValues.businessPartner1.addresses.first().premises.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_PREMISE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedPremise)
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

    /**
     * Given partner with premise value and name value
     * When ask suggestion for a word in premise value with filter by other than name value
     * Then don't show premise value
     */
    @Test
    fun `premise__Don't suggest by word when filtered out`() {
        val expectedPremise = CdqTestValues.businessPartner1.addresses.first().premises.first().value
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_PREMISE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedPremise)
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

    /**
     * Given partner with premise value in non-latin characters
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `premise__Suggest by non-latin characters`() {
        val expectedPremise = CdqTestValues.businessPartner3.addresses.first().premises.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_PREMISE_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedPremise)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPremise }
    }

    /**
     * Given partner with postal delivery pointvalue
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `postalDeliveryPoint__Suggest by phrase`() {
        val expectedPostalDeliveryPoint = CdqTestValues.businessPartner1.addresses.first().postalDeliveryPoints.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_POSTAL_DELIVERY_POINT_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedPostalDeliveryPoint)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostalDeliveryPoint }
    }

    /**
     * Given partner with postal delivery pointvalue
     * When ask suggestion for a prefix of that value
     * Then show that value
     */
    @Test
    fun `postalDeliveryPoint__Suggest by prefix`() {
        val expectedPostalDeliveryPoint = CdqTestValues.businessPartner1.addresses.first().postalDeliveryPoints.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_POSTAL_DELIVERY_POINT_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedPostalDeliveryPoint.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostalDeliveryPoint }
    }

    /**
     * Given partner with postal delivery pointvalue that is several words
     * When ask suggestion for a word in value
     * Then show that value
     */
    @Test
    fun `postalDeliveryPoint__Suggest by word`() {
        val expectedPostalDeliveryPoint = CdqTestValues.businessPartner1.addresses.first().postalDeliveryPoints.first().value
        val queryText = expectedPostalDeliveryPoint.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_POSTAL_DELIVERY_POINT_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedPostalDeliveryPoint }
    }

    /**
     * Given partner with postal delivery pointvalue
     * When ask suggestion for text that doesn't have a word or prefix in value
     * Then don't show that value
     */
    @Test
    fun `postalDeliveryPoint__Don't suggest by different`() {
        val expectedPostalDeliveryPoint = CdqTestValues.businessPartner1.addresses.first().postalDeliveryPoints.first().value
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_POSTAL_DELIVERY_POINT_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedPostalDeliveryPoint }
    }

    /**
     * Given partner with postal delivery point value and name value
     * When ask suggestion postal delivery point with filter by name value
     * Then show postal delivery point value
     */
    @Test
    fun `postalDeliveryPoint__Suggest filtered suggestions`() {
        val expectedPostalDeliveryPoint = CdqTestValues.businessPartner1.addresses.first().postalDeliveryPoints.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_POSTAL_DELIVERY_POINT_PATH)
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

    /**
     * Given partner with postal delivery point value and name value
     * When ask suggestion for a word in postal delivery point value with filter by name value
     * Then show postal delivery point value
     */
    @Test
    fun `postalDeliveryPoint__Suggest by word in filtered suggestions`() {
        val expectedPostalDeliveryPoint = CdqTestValues.businessPartner1.addresses.first().postalDeliveryPoints.first().value
        val filterName = CdqTestValues.businessPartner1.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_POSTAL_DELIVERY_POINT_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedPostalDeliveryPoint)
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

    /**
     * Given partner with postal delivery point value and name value
     * When ask suggestion for a word in postal delivery point value with filter by other than name value
     * Then don't show postal delivery point value
     */
    @Test
    fun `postalDeliveryPoint__Don't suggest by word when filtered out`() {
        val expectedPostalDeliveryPoint = CdqTestValues.businessPartner1.addresses.first().postalDeliveryPoints.first().value
        val filterName = CdqTestValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_POSTAL_DELIVERY_POINT_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedPostalDeliveryPoint)
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

    /**
     * Given partner with postal delivery point value in non-latin characters
     * When ask suggestion for that value
     * Then show that value
     */
    @Test
    fun `postalDeliveryPoint__Suggest by non-latin characters`() {
        val expectedPostalDeliveryPoint = CdqTestValues.businessPartner3.addresses.first().postalDeliveryPoints.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_POSTAL_DELIVERY_POINT_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedPostalDeliveryPoint)
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