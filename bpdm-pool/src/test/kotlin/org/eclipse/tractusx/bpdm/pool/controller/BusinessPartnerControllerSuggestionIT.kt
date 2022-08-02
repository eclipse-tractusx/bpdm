package org.eclipse.tractusx.bpdm.pool.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.cdq.BusinessPartnerCollectionCdq
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.component.cdq.service.ImportStarterService
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service.OpenSearchSyncStarterService
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SuggestionResponse
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import java.util.stream.Stream

/**
 * Integration tests for the look-ahead endpoints of the business partner controller
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles(value = ["test"])
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OpenSearchContextInitializer::class])
class BusinessPartnerControllerSuggestionIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val importService: ImportStarterService,
    val openSearchSyncService: OpenSearchSyncStarterService,
    val objectMapper: ObjectMapper,
    val testHelpers: TestHelpers,
    val businessPartnerBuildService: BusinessPartnerBuildService
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

        @JvmStatic
        fun argumentsSuggestPropertyValues(): Stream<Arguments> =
            Stream.of(
                Arguments.of(CdqValues.businessPartner1.names.first().value, EndpointValues.CATENA_NAME_PATH, CdqValues.businessPartner1.names.first().value),
                Arguments.of(
                    CdqValues.businessPartner1.legalForm!!.name,
                    EndpointValues.CATENA_LEGAL_FORM_PATH,
                    CdqValues.businessPartner1.names.first().value
                ),
                Arguments.of(
                    CdqValues.businessPartner1.status!!.officialDenotation,
                    EndpointValues.CATENA_STATUS_PATH,
                    CdqValues.businessPartner1.names.first().value
                ),
                Arguments.of(
                    CdqValues.businessPartner1.profile!!.classifications.first().value,
                    EndpointValues.CATENA_CLASSIFICATION_PATH,
                    CdqValues.businessPartner1.names.first().value
                ),
                Arguments.of(
                    RequestValues.businessPartnerRequest1.sites.first().name,
                    EndpointValues.CATENA_SITE_PATH,
                    RequestValues.businessPartnerRequest1.names.first().value
                ),
                Arguments.of(
                    CdqValues.businessPartner1.addresses.first().administrativeAreas.first().value,
                    EndpointValues.CATENA_ADMIN_AREA_PATH,
                    CdqValues.businessPartner1.names.first().value
                ),
                Arguments.of(
                    CdqValues.businessPartner1.addresses.first().postCodes.first().value,
                    EndpointValues.CATENA_POST_CODE_PATH,
                    CdqValues.businessPartner1.names.first().value
                ),
                Arguments.of(
                    CdqValues.businessPartner1.addresses.first().localities.first().value,
                    EndpointValues.CATENA_LOCALITY_PATH,
                    CdqValues.businessPartner1.names.first().value
                ),
                Arguments.of(
                    CdqValues.businessPartner1.addresses.first().thoroughfares.first().value,
                    EndpointValues.CATENA_THOROUGHFARE_PATH,
                    CdqValues.businessPartner1.names.first().value
                ),
                Arguments.of(
                    CdqValues.businessPartner1.addresses.first().premises.first().value,
                    EndpointValues.CATENA_PREMISE_PATH,
                    CdqValues.businessPartner1.names.first().value
                ),
                // search for some property of address of site, not directly on business partner
                Arguments.of(
                    RequestValues.businessPartnerRequest1.sites.first().addresses.first().premises.first().value,
                    EndpointValues.CATENA_PREMISE_PATH,
                    RequestValues.businessPartnerRequest1.names.first().value
                ),
                Arguments.of(
                    CdqValues.businessPartner1.addresses.first().postalDeliveryPoints.first().value,
                    EndpointValues.CATENA_POSTAL_DELIVERY_POINT_PATH,
                    CdqValues.businessPartner1.names.first().value
                )
            )

        @JvmStatic
        fun argumentsSuggestPropertyValuesNonLatin(): Stream<Arguments> =
            Stream.of(
                Arguments.of(CdqValues.businessPartner3.names.first().value, EndpointValues.CATENA_NAME_PATH),
                Arguments.of(CdqValues.businessPartner3.legalForm!!.name, EndpointValues.CATENA_LEGAL_FORM_PATH),
                Arguments.of(RequestValues.businessPartnerRequest3.sites.first().name, EndpointValues.CATENA_SITE_PATH),
                Arguments.of(CdqValues.businessPartner3.addresses.first().administrativeAreas.first().value, EndpointValues.CATENA_ADMIN_AREA_PATH),
                Arguments.of(CdqValues.businessPartner3.addresses.first().postCodes.first().value, EndpointValues.CATENA_POST_CODE_PATH),
                Arguments.of(CdqValues.businessPartner3.addresses.first().localities.first().value, EndpointValues.CATENA_LOCALITY_PATH),
                Arguments.of(CdqValues.businessPartner3.addresses.first().thoroughfares.first().value, EndpointValues.CATENA_THOROUGHFARE_PATH),
                Arguments.of(CdqValues.businessPartner3.addresses.first().premises.first().value, EndpointValues.CATENA_PREMISE_PATH),
                Arguments.of(CdqValues.businessPartner3.addresses.first().postalDeliveryPoints.first().value, EndpointValues.CATENA_POSTAL_DELIVERY_POINT_PATH)
            )
    }


    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        openSearchSyncService.clearOpenSearch()

        val partnerDocs = listOf(
            CdqValues.businessPartner1,
            CdqValues.businessPartner2,
            CdqValues.businessPartner3
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

        importService.import()

        // import partners with sites via service since they can't be imported from cdq data model
        businessPartnerBuildService.upsertBusinessPartners(
            listOf(RequestValues.businessPartnerRequest1, RequestValues.businessPartnerRequest2)
        )

        openSearchSyncService.export()
    }

    /**
     * Given partner with property value
     * When asking for a suggestion for that property
     * Then show that property value
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValues")
    fun `suggest property values`(expectedSuggestionValue: String, endpointPath: String) {

        val page = webTestClient.get().uri(endpointPath)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedSuggestionValue }
    }

    /**
     * Given partner with property value
     * When asking for a suggestion for that property value
     * Then show that property value
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValues")
    fun `suggest by phrase`(expectedSuggestionValue: String, endpointPath: String) {

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(endpointPath)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedSuggestionValue)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedSuggestionValue }
    }

    /**
     * Given partner with property value
     * When ask suggestion for a prefix of that value
     * Then show that value
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValues")
    fun `suggest by prefix`(expectedSuggestionValue: String, endpointPath: String) {

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(endpointPath)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedSuggestionValue.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedSuggestionValue }
    }

    /**
     * Given partner with property value that is several words
     * When ask suggestion for a word in value
     * Then show that value
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValues")
    fun `suggest by word`(expectedSuggestionValue: String, endpointPath: String) {
        val queryText = expectedSuggestionValue.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(endpointPath)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedSuggestionValue }
    }

    /**
     * Given partner with property value
     * When ask suggestion for text that doesn't have a word or prefix in value
     * Then don't show that value
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValues")
    fun `don't suggest by different`(expectedSuggestionValue: String, endpointPath: String) {
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(endpointPath)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedSuggestionValue }
    }

    /**
     * Given partner with property 1 value and property 2 value
     * When ask suggestion property 1 with filter by property 2 value
     * Then show property 1 value 1
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValues")
    fun `suggest filtered suggestions`(expectedSuggestionValue: String, endpointPath: String, filterName: String) {
        val page = webTestClient.get()
            .uri { builder ->
                builder.path(endpointPath)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedSuggestionValue }
    }

    /**
     * Given partner with property 1 value and property 2 value
     * When ask suggestion for a word in property 1 value with filter by property 2 value
     * Then show property 1 value
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValues")
    fun `suggest by word in filtered suggestions`(expectedSuggestionValue: String, endpointPath: String, filterName: String) {
        val page = webTestClient.get()
            .uri { builder ->
                builder.path(endpointPath)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedSuggestionValue)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedSuggestionValue }
    }

    /**
     * Given partner with property 1 value and property 2 value
     * When ask suggestion for a word in property 1 value with filter by other than property 2 value
     * Then don't show property 1 value
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValues")
    fun `don't suggest by word when filtered out`(expectedSuggestionValue: String, endpointPath: String) {
        val filterName = CdqValues.businessPartner2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(endpointPath)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedSuggestionValue)
                    .queryParam(BusinessPartnerPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedSuggestionValue }
    }

    /**
     * Given partner with property value in non-latin characters
     * When ask suggestion for that value
     * Then show that value
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValuesNonLatin")
    fun `suggest by non-latin characters`(expectedSuggestionValue: String, endpointPath: String) {
        val expectedName = CdqValues.businessPartner3.names.first().value

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
}