/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.controller

import com.neovisionaries.i18n.CountryCode
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.LegalFormResponse
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalFormRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.CountryIdentifierTypeResponse
import org.eclipse.tractusx.bpdm.pool.entity.CountryIdentifierType
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierType
import org.eclipse.tractusx.bpdm.pool.repository.CountryIdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.util.stream.Stream
import kotlin.math.ceil


private typealias PostFunction = (client: WebTestClient, metaData: Any) -> Any
private typealias PostFunctionWithoutExpectation = (client: WebTestClient, metaData: Any) -> WebTestClient.ResponseSpec
private typealias GetFunction = (client: WebTestClient, page: Int, size: Int) -> PageResponse<Any>

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class MetadataControllerIT @Autowired constructor(
    private val testHelpers: TestHelpers,
    private val webTestClient: WebTestClient,
    private val countryIdentifierTypeRepository: CountryIdentifierTypeRepository,
    private val identifierTypeRepository: IdentifierTypeRepository
) {
    companion object {

        private val identifierTypes = listOf(RequestValues.identifierType1, RequestValues.identifierType2, RequestValues.identifierType3)
        private val legalFormRequests = listOf(
            RequestValues.legalForm1,
            RequestValues.legalForm2,
            RequestValues.legalForm3
        )
        private val legalFormExpected = listOf(
            ResponseValues.legalForm1,
            ResponseValues.legalForm2,
            ResponseValues.legalForm3
        )

        private fun postIdentifierType(client: WebTestClient, type: TypeKeyNameDto<String>) =
            postMetadataSameResponseType(client, type, EndpointValues.CATENA_METADATA_IDENTIFIER_TYPE_PATH)

        private fun getIdentifierTypes(client: WebTestClient, page: Int, size: Int) =
            getMetadata<PageResponse<TypeKeyNameDto<String>>>(client, page, size, EndpointValues.CATENA_METADATA_IDENTIFIER_TYPE_PATH)

        private fun postIdentifierTypeWithoutExpectation(client: WebTestClient, type: TypeKeyNameDto<String>) =
            postMetadataWithoutExpectation(client, type, EndpointValues.CATENA_METADATA_IDENTIFIER_TYPE_PATH)

        private fun postLegalForm(client: WebTestClient, type: LegalFormRequest) =
            postMetadata<LegalFormRequest, LegalFormResponse>(client, type, EndpointValues.CATENA_METADATA_LEGAL_FORM_PATH)

        private fun getLegalForms(client: WebTestClient, page: Int, size: Int) =
            getMetadata<PageResponse<LegalFormResponse>>(client, page, size, EndpointValues.CATENA_METADATA_LEGAL_FORM_PATH)

        private fun postLegalFormWithoutExpectation(client: WebTestClient, type: LegalFormRequest) =
            postMetadataWithoutExpectation(client, type, EndpointValues.CATENA_METADATA_LEGAL_FORM_PATH)


        private inline fun <reified T : Any> postMetadataSameResponseType(client: WebTestClient, metadata: T, endpointPath: String) =
            postMetadata<T, T>(client, metadata, endpointPath)

        private inline fun <reified S : Any, reified T : Any> postMetadata(client: WebTestClient, metadata: S, endpointPath: String): T {
            println("endpoint $endpointPath")
            return client.invokePostEndpoint(endpointPath, metadata)
        }

        private inline fun <reified T : Any> getMetadata(client: WebTestClient, page: Int, size: Int, endpointPath: String): T {
            return client.invokeGetEndpoint(
                endpointPath,
                Pair(PaginationRequest::page.name, page.toString()),
                Pair(PaginationRequest::size.name, size.toString())
            )
        }

        private inline fun <reified T : Any> postMetadataWithoutExpectation(client: WebTestClient, type: T, endpointPath: String): WebTestClient.ResponseSpec {
            return client.post().uri(endpointPath)
                .body(BodyInserters.fromValue(type))
                .exchange()
        }

        @JvmStatic
        fun creationTestArguments(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    RequestValues.identifierType1,
                    RequestValues.identifierType1,
                    ::postIdentifierType
                ),
                Arguments.of(
                    RequestValues.legalForm1,
                    ResponseValues.legalForm1,
                    ::postLegalForm
                )
            )

        @JvmStatic
        fun conflictTestArguments(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    RequestValues.identifierType1,
                    RequestValues.identifierType1,
                    ::postIdentifierTypeWithoutExpectation,
                    ::getIdentifierTypes
                ),
                Arguments.of(
                    RequestValues.legalForm1,
                    ResponseValues.legalForm1,
                    ::postLegalFormWithoutExpectation,
                    ::getLegalForms
                )
            )

        @JvmStatic
        fun paginationTestArguments(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    identifierTypes,
                    identifierTypes,
                    ::postIdentifierType,
                    ::getIdentifierTypes
                ),
                Arguments.of(
                    legalFormRequests,
                    legalFormExpected,
                    ::postLegalForm,
                    ::getLegalForms
                )
            )
    }


    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
    }

    /**
     * Given no metadata
     * When creating new metadata
     * Then created metadata returned
     */
    @ParameterizedTest
    @MethodSource("creationTestArguments")
    fun createMetadata(metadata: Any, expected: Any, postMetadata: PostFunction) {
        val returnedType = postMetadata(webTestClient, metadata)

        assertThat(returnedType).isEqualTo(expected)
    }

    /**
     * Given metadata with technical key
     * When trying to create new metadata with that technical key
     * Then metadata is not created and error returned
     */
    @ParameterizedTest
    @MethodSource("conflictTestArguments")
    fun createMetadata_Conflict(metadata: Any, expected: Any, postMetadata: PostFunctionWithoutExpectation, getMetadataPage: GetFunction) {
        postMetadata(webTestClient, metadata).expectStatus().is2xxSuccessful

        //Expect Error when posting identifier type with same technical key
        postMetadata(webTestClient, metadata).expectStatus().is4xxClientError

        //Check type is really not created
        val returnedPage = getMetadataPage(webTestClient, 0, Int.MAX_VALUE)
        assertThat(returnedPage.content).isEqualTo(listOf(expected))
    }

    /**
     * Given metadata
     * When asking for metadata entries
     * Then that metadata returned
     */
    @ParameterizedTest
    @MethodSource("paginationTestArguments")
    fun getMetadata_FullPage(metadata: Collection<Any>, expected: Collection<Any>, postMetadata: PostFunction, getMetadataPage: GetFunction) {
        metadata.forEach { postMetadata(webTestClient, it) }

        val returnedPage = getMetadataPage(webTestClient, 0, metadata.size)
        val expectedPage = PageResponse(expected.size.toLong(), 1, 0, expected.size, expected)

        assertThat(returnedPage).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedPage)
    }

    /**
     * Given entries of metadata
     * When paginating through all entries
     * Then all entries returned
     */
    @ParameterizedTest
    @MethodSource("paginationTestArguments")
    fun getMetadata_Paginated(metadata: Collection<Any>, expected: Collection<Any>, postMetadata: PostFunction, getMetadataPage: GetFunction) {
        metadata.forEach { postMetadata(webTestClient, it) }

        val returnedMetadata = (metadata.indices).map {
            val returnedPage = getMetadataPage(webTestClient, it, 1)
            assertThat(returnedPage.content.size).isEqualTo(1)

            returnedPage.content.first()
        }

        assertThat(returnedMetadata).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected)
    }

    /**
     * Given several metadata entries
     * When asking for a page of multiple entries
     * Then get a page of multiple entries
     */
    @ParameterizedTest
    @MethodSource("paginationTestArguments")
    fun getMetadata_Multiple(metadata: Collection<Any>, expected: Collection<Any>, postMetadata: PostFunction, getMetadataPage: GetFunction) {
        metadata.forEach { postMetadata(webTestClient, it) }

        val returnedPage = getMetadataPage(webTestClient, 0, 2)

        assertThat(returnedPage.totalElements).isEqualTo(expected.size.toLong())
        assertThat(returnedPage.totalPages).isEqualTo(ceil(expected.size / 2f).toInt())
        assertThat(returnedPage.page).isEqualTo(0)
        assertThat(returnedPage.contentSize).isEqualTo(2)
        assertThat(returnedPage.content.size).isEqualTo(2)
        assertThat(returnedPage.content).isSubsetOf(expected)
    }

    /**
     * Given several metadata entries
     * When asking for a page exceeding the size of total entries
     * Then get a page of all entries
     */
    @ParameterizedTest
    @MethodSource("paginationTestArguments")
    fun getMetadata_ExceedingSize(metadata: Collection<Any>, expected: Collection<Any>, postMetadata: PostFunction, getMetadataPage: GetFunction) {
        metadata.forEach { postMetadata(webTestClient, it) }

        val returnedPage = getMetadataPage(webTestClient, 0, metadata.size + 1)
        val expectedPage = PageResponse(expected.size.toLong(), 1, 0, expected.size, expected)

        assertThat(returnedPage).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedPage)
    }

    /**
     * Given several metadata entries
     * When asking for a page exceeding total number of pages
     * Then get an empty page
     */
    @ParameterizedTest
    @MethodSource("paginationTestArguments")
    fun getMetadata_ExceedingPage(metadata: Collection<Any>, expected: Collection<Any>, postMetadata: PostFunction, getMetadataPage: GetFunction) {
        metadata.forEach { postMetadata(webTestClient, it) }

        val returnedPage = getMetadataPage(webTestClient, metadata.size, 1)
        val expectedPage = PageResponse(expected.size.toLong(), expected.size, expected.size, 0, emptyList<Any>())

        assertThat(returnedPage).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedPage)
    }

    /**
     * Given identifier types in db, some "common", others for specific countries
     * When requesting the identifiers for a country
     * Then the "common" identifiers and the identifiers of the requested country are returned
     */
    @Test
    fun getValidIdentifiersForCountry() {
        val identifierType1 = IdentifierType(name = CommonValues.identifierTypeName1, technicalKey = CommonValues.identifierTypeTechnicalKey1)
        val identifierType2 = IdentifierType(name = CommonValues.identifierTypeName2, technicalKey = CommonValues.identifierTypeTechnicalKey2)
        val identifierType3 = IdentifierType(name = CommonValues.identifierTypeName3, technicalKey = CommonValues.identifierTypeTechnicalKey3)
        val givenIdentifierTypes = listOf(identifierType1, identifierType2, identifierType3)

        val countryIdentifierType1 = CountryIdentifierType(null, identifierType1, true)
        val countryIdentifierType2 = CountryIdentifierType(CountryCode.UK, identifierType2, false)
        val countryIdentifierType3 = CountryIdentifierType(CountryCode.PL, identifierType3, false)
        val givenCountryIdentifierTypes = listOf(countryIdentifierType1, countryIdentifierType2, countryIdentifierType3)

        val expected = listOf(
            CountryIdentifierTypeResponse(TypeKeyNameDto(CommonValues.identifierTypeTechnicalKey1, CommonValues.identifierTypeName1), true),
            CountryIdentifierTypeResponse(TypeKeyNameDto(CommonValues.identifierTypeTechnicalKey3, CommonValues.identifierTypeName3), false)
        )

        identifierTypeRepository.saveAll(givenIdentifierTypes)
        countryIdentifierTypeRepository.saveAll(givenCountryIdentifierTypes)

        val resultCountryIdentifierTypes = webTestClient.invokeGetEndpointWithArrayResponse<CountryIdentifierTypeResponse>(
            EndpointValues.CATENA_METADATA_IDENTIFIER_TYPES_FOR_COUNTRY_PATH,
            Pair("country", CountryCode.PL.alpha2)
        )

        assertThat(resultCountryIdentifierTypes).containsExactlyInAnyOrderElementsOf(expected)
    }
}