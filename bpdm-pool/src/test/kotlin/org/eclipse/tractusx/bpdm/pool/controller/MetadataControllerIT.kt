/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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
import com.neovisionaries.i18n.CountryCode.*
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierTypeDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalFormDto
import org.eclipse.tractusx.bpdm.pool.api.model.QualityLevel
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalFormRequest
import org.eclipse.tractusx.bpdm.pool.entity.FieldQualityRuleDb
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDetailDb
import org.eclipse.tractusx.bpdm.pool.repository.FieldQualityRuleRepository
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.service.toDto
import org.eclipse.tractusx.bpdm.pool.util.EndpointValues
import org.eclipse.tractusx.bpdm.pool.util.TestHelpers
import org.eclipse.tractusx.bpdm.pool.util.invokeGetEndpoint
import org.eclipse.tractusx.bpdm.pool.util.invokePostEndpoint
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
private typealias GetFunction = (client: WebTestClient, page: Int, size: Int) -> PageDto<Any>

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles("test-no-auth", "test-scheduling-disabled")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class MetadataControllerIT @Autowired constructor(
    private val dbTestHelpers: DbTestHelpers,
    private val webTestClient: WebTestClient,
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val fieldQualityRuleRepository: FieldQualityRuleRepository,
    val poolClient: PoolApiClient
) {
    companion object {

        private val identifierTypeDtos = listOf(
            BusinessPartnerNonVerboseValues.identifierTypeDto1,
            BusinessPartnerNonVerboseValues.identifierTypeDto2,
            BusinessPartnerNonVerboseValues.identifierTypeDto3
        )

        private fun postIdentifierType(client: WebTestClient, type: IdentifierTypeDto) =
            postMetadataSameResponseType(client, type, EndpointValues.CATENA_METADATA_IDENTIFIER_TYPE_PATH)

        private fun getIdentifierTypes(client: WebTestClient, page: Int, size: Int): PageDto<IdentifierTypeDto> =
//            getMetadata<PageResponse<IdentifierTypeDto>>(client, page, size, EndpointValues.CATENA_METADATA_IDENTIFIER_TYPE_PATH)
            client.invokeGetEndpoint(
                EndpointValues.CATENA_METADATA_IDENTIFIER_TYPE_PATH,
                Pair("businessPartnerType", IdentifierBusinessPartnerType.LEGAL_ENTITY.name),
                Pair(PaginationRequest::page.name, page.toString()),
                Pair(PaginationRequest::size.name, size.toString())
            )

        private fun postIdentifierTypeWithoutExpectation(client: WebTestClient, type: IdentifierTypeDto) =
            postMetadataWithoutExpectation(client, type, EndpointValues.CATENA_METADATA_IDENTIFIER_TYPE_PATH)

        private fun postLegalForm(client: WebTestClient, type: LegalFormRequest) =
            postMetadata<LegalFormRequest, LegalFormDto>(client, type, EndpointValues.CATENA_METADATA_LEGAL_FORM_PATH)

        private fun getLegalForms(client: WebTestClient, page: Int, size: Int): PageDto<LegalFormDto> =
//            getMetadata<PageResponse<LegalFormResponse>>(client, page, size, EndpointValues.CATENA_METADATA_LEGAL_FORM_PATH)
            client.invokeGetEndpoint(
                EndpointValues.CATENA_METADATA_LEGAL_FORM_PATH,
                Pair(PaginationRequest::page.name, page.toString()),
                Pair(PaginationRequest::size.name, size.toString())
            )

        private fun postLegalFormWithoutExpectation(client: WebTestClient, type: LegalFormRequest) =
            postMetadataWithoutExpectation(client, type, EndpointValues.CATENA_METADATA_LEGAL_FORM_PATH)


        private inline fun <reified T : Any> postMetadataSameResponseType(client: WebTestClient, metadata: T, endpointPath: String) =
            postMetadata<T, T>(client, metadata, endpointPath)

        private inline fun <reified S : Any, reified T : Any> postMetadata(client: WebTestClient, metadata: S, endpointPath: String): T {
            println("endpoint $endpointPath")
            return client.invokePostEndpoint(endpointPath, metadata)
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
                    BusinessPartnerNonVerboseValues.identifierTypeDto1,
                    BusinessPartnerNonVerboseValues.identifierTypeDto1,
                    ::postIdentifierType
                )
            )

        @JvmStatic
        fun conflictTestArguments(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    BusinessPartnerNonVerboseValues.identifierTypeDto1,
                    BusinessPartnerNonVerboseValues.identifierTypeDto1,
                    ::postIdentifierTypeWithoutExpectation,
                    ::getIdentifierTypes
                )
            )

        @JvmStatic
        fun paginationTestArguments(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    identifierTypeDtos,
                    identifierTypeDtos,
                    ::postIdentifierType,
                    ::getIdentifierTypes
                )
            )
    }


    @BeforeEach
    fun beforeEach() {
        dbTestHelpers.truncateDbTables()

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
     * When creating new legalForm
     * Then created legalForm returned
     */
    @Test
    fun createLegalForm() {
        val technicalKey = "CREATE_TEST_UNIQUE"
        val createdLegalForm = poolClient.metadata.createLegalForm(BusinessPartnerNonVerboseValues.legalForm1.copy(technicalKey = technicalKey))

        assertThat(createdLegalForm).isEqualTo(BusinessPartnerVerboseValues.legalForm1.copy(technicalKey = technicalKey))
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
     * Given legalForm with technical key
     * When trying to create new legalForm with that technical key
     * Then legalForm is not created and error returned
     */
    @Test
    fun createLegalform_Conflict() {
        val legalFormToCreate = BusinessPartnerNonVerboseValues.legalForm2.copy(technicalKey = "CONFLICT_TEST_UNIQUE")
        poolClient.metadata.createLegalForm(legalFormToCreate)
        //Expect Error when posting legal form with same technical key
        assertThrows<Throwable>{ poolClient.metadata.createLegalForm(legalFormToCreate) }
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
        val expectedPage = PageDto(expected.size.toLong(), 1, 0, expected.size, expected)

        assertThat(returnedPage).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedPage)
    }

    /**
     * Given legalForms
     * When asking for a page of legalForms
     * Then a full page of legalForms is returned
     */
    @Test
    fun getLegalForm_FullPage() {
        val paginationRequest = PaginationRequest()
        val returnedLegalForms = poolClient.metadata.getLegalForms(paginationRequest)

        assertThat(returnedLegalForms.content.size).isEqualTo(paginationRequest.size)
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
        val expectedPage = PageDto(expected.size.toLong(), 1, 0, expected.size, expected)

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
        val expectedPage = PageDto(expected.size.toLong(), expected.size, expected.size, 0, emptyList<Any>())

        assertThat(returnedPage).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedPage)
    }

    /**
     * Given identifier types in db, some "common", others for specific countries
     * When requesting the identifiers for a country
     * Then the "common" identifiers and the identifiers of the requested country are returned
     */
    @Test
    fun getValidIdentifiersForCountry() {
        val identifierType1 = IdentifierTypeDb(
            technicalKey = BusinessPartnerNonVerboseValues.identifierType1.technicalKey,
            businessPartnerType = IdentifierBusinessPartnerType.LEGAL_ENTITY,
            name = BusinessPartnerNonVerboseValues.identifierType1.name,
        )
        identifierType1.details.add(IdentifierTypeDetailDb(identifierType1, null, true))

        val identifierType2 = IdentifierTypeDb(
            technicalKey = BusinessPartnerNonVerboseValues.identifierType2.technicalKey,
            businessPartnerType = IdentifierBusinessPartnerType.LEGAL_ENTITY,
            name = BusinessPartnerNonVerboseValues.identifierType2.name
        )
        identifierType2.details.add(IdentifierTypeDetailDb(identifierType2, UK, false))

        val identifierType3 = IdentifierTypeDb(
            technicalKey = BusinessPartnerNonVerboseValues.identifierType3.technicalKey,
            businessPartnerType = IdentifierBusinessPartnerType.LEGAL_ENTITY,
            name = BusinessPartnerNonVerboseValues.identifierType3.name
        )
        identifierType3.details.add(IdentifierTypeDetailDb(identifierType3, PL, false))

        val givenIdentifierTypes = listOf(identifierType1, identifierType2, identifierType3)

        identifierTypeRepository.saveAll(givenIdentifierTypes)

        val expected = listOf(
            identifierType1.toDto(),
            identifierType3.toDto()
        )

        val result = webTestClient.invokeGetEndpoint<PageDto<IdentifierTypeDto>>(
            EndpointValues.CATENA_METADATA_IDENTIFIER_TYPE_PATH,
            Pair("businessPartnerType", IdentifierBusinessPartnerType.LEGAL_ENTITY.name),
            Pair("country", PL.alpha2)
        )

        assertThat(result.content).containsExactlyInAnyOrderElementsOf(expected)
    }

    @Test
    fun `get field quality rules`() {

        val rulePL1 = addressRuleMandatory(country = PL, field = "path1.field1")
        val rulePL2 = addressRuleMandatory(country = PL, field = "path1.field2")
        val ruleDE1 = addressRuleMandatory(country = DE, field = "path1.field1")
        fieldQualityRuleRepository.saveAll(listOf(rulePL1, rulePL2, ruleDE1))

        val resultPl = poolClient.metadata.getFieldQualityRules(PL).body
        assertThat(resultPl?.size).isEqualTo(2)
        assertThat(resultPl?.map { it.fieldPath }).containsExactlyInAnyOrder("path1.field1", "path1.field2")

        val resultDe = poolClient.metadata.getFieldQualityRules(DE).body
        assertThat(resultDe?.size).isEqualTo(1)
        assertThat(resultDe?.map { it.fieldPath }).containsExactlyInAnyOrder("path1.field1")

        val resultUs = poolClient.metadata.getFieldQualityRules(US).body
        assertThat(resultUs?.size).isEqualTo(0)
    }

    @Test
    fun `check merge field quality rules `() {

        val ruleDefault1 = addressRuleMandatory(null, field = "path1.field1")
        val ruleDefault2 = addressRuleMandatory(null, field = "path1.field2")
        val ruleDefault3 = addressRuleOptional(null, field = "path1.field3")
        val ruleDefault4 = addressRuleOptional(null, field = "path1.field4")

        val rulePL1 = addressRuleOptional(country = PL, field = "path1.field1")
        val rulePL2 = addressRuleForbidden(country = PL, field = "path1.field4")
        val ruleDE1 = addressRuleMandatory(country = DE, field = "path1.field1")
        fieldQualityRuleRepository.saveAll(listOf(ruleDefault1, ruleDefault2, ruleDefault3, ruleDefault4, rulePL1, rulePL2, ruleDE1))

        val resultPl = poolClient.metadata.getFieldQualityRules(PL).body
        assertThat(resultPl?.size).isEqualTo(3)
        assertThat(resultPl?.map { it.fieldPath }).containsExactlyInAnyOrder("path1.field1", "path1.field2", "path1.field3")
        assertThat(resultPl?.filter { it.fieldPath == "path1.field1" }?.map { it.qualityLevel }).describedAs("PL optional overwrites default mandatory")
            .containsExactlyInAnyOrder(QualityLevel.OPTIONAL)
        assertThat(resultPl?.filter { it.fieldPath == "path1.field2" }?.map { it.qualityLevel }).describedAs("no PL rule use default mandatory")
            .containsExactlyInAnyOrder(QualityLevel.MANDATORY)
        assertThat(resultPl?.filter { it.fieldPath == "path1.field3" }?.map { it.qualityLevel }).describedAs("no PL rule use default optional")
            .containsExactlyInAnyOrder(QualityLevel.OPTIONAL)
        assertThat(resultPl?.filter { it.fieldPath == "path1.field4" }).describedAs("PL rule forbidden overwrites default")
            .isEmpty()
    }


    private fun addressRuleMandatory(country: CountryCode?, field: String): FieldQualityRuleDb {
        return FieldQualityRuleDb(
            countryCode = country,
            fieldPath = field,
            schemaName = "address",
            qualityLevel = QualityLevel.MANDATORY
        )
    }

    private fun addressRuleForbidden(country: CountryCode?, field: String): FieldQualityRuleDb {
        return FieldQualityRuleDb(
            countryCode = country,
            fieldPath = field,
            schemaName = "address",
            qualityLevel = QualityLevel.FORBIDDEN
        )
    }

    private fun addressRuleOptional(country: CountryCode?, field: String): FieldQualityRuleDb {
        return FieldQualityRuleDb(
            countryCode = country,
            fieldPath = field,
            schemaName = "address",
            qualityLevel = QualityLevel.OPTIONAL
        )
    }

    @Test
    fun `get all administrative-areas-level1`() {
        val firstPage = poolClient.metadata.getAdminAreasLevel1(PaginationRequest())
        assertThat(firstPage.totalElements).isEqualTo(3478L)
        assertThat(firstPage.totalPages).isEqualTo(348)
        assertThat(firstPage.content.size).isEqualTo(10)

        with(firstPage.content.first()) {
            assertThat(countryCode).isEqualTo(AD)
            assertThat(code).isEqualTo("AD-02")
            assertThat(name).isEqualTo("Canillo")
        }

        val lastPage = poolClient.metadata.getAdminAreasLevel1(PaginationRequest(347))
        assertThat(lastPage.totalElements).isEqualTo(3478L)
        assertThat(lastPage.totalPages).isEqualTo(348)
        assertThat(lastPage.content.size).isEqualTo(8)

        with(lastPage.content.last()) {
            assertThat(countryCode).isEqualTo(ZW)
            assertThat(code).isEqualTo("ZW-MW")
            assertThat(name).isEqualTo("Mashonaland West")
        }
    }
}
