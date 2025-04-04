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

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.BpnRequestIdentifierSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.IdentifiersSearchRequest
import org.eclipse.tractusx.bpdm.pool.entity.BpnRequestIdentifierMappingDb
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.util.TestHelpers
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.pool.LegalEntityStructureRequest
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.test.util.PoolDataHelpers
import org.junit.Assert
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class],
    properties = ["bpdm.controller.search-request-limit=2"]
)
@ActiveProfiles("test-no-auth")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class BpnControllerIT @Autowired constructor(
    val testHelpers: TestHelpers,
    val poolClient: PoolClientImpl,
    val dbTestHelpers: DbTestHelpers,
    val poolDataHelpers: PoolDataHelpers,
    val bpnRequestIdentifierRepository: BpnRequestIdentifierRepository
) {

    val identifierType = BusinessPartnerNonVerboseValues.legalEntityCreate1.legalEntity.identifiers.first().type
    val identifierValue1 = BusinessPartnerNonVerboseValues.legalEntityCreate1.legalEntity.identifiers.first().value
    val identifierValue2 = BusinessPartnerNonVerboseValues.legalEntityCreate2.legalEntity.identifiers.first().value
    val identifierValue3 = BusinessPartnerNonVerboseValues.legalEntityCreate3.legalEntity.identifiers.first().value

    @BeforeEach
    fun beforeEach() {
        // ensure LE1 and 2 have same identifierType
        val legalEntityCreate1 = with(BusinessPartnerNonVerboseValues.legalEntityCreate1) { copy(
            legalEntity = legalEntity.copy(
                identifiers = listOf(LegalEntityIdentifierDto(identifierValue1, identifierType, null))
            )
        ) }
        val legalEntityCreate2 = with(BusinessPartnerNonVerboseValues.legalEntityCreate2) { copy(
            legalEntity = legalEntity.copy(
                identifiers = listOf(LegalEntityIdentifierDto(identifierValue2, identifierType, null))
            )
        ) }
        val legalEntityCreate3 = BusinessPartnerNonVerboseValues.legalEntityCreate3

        dbTestHelpers.truncateDbTables()
        poolDataHelpers.createPoolMetadata()
        testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(legalEntity = legalEntityCreate1),
                LegalEntityStructureRequest(legalEntity = legalEntityCreate2),
                LegalEntityStructureRequest(legalEntity = legalEntityCreate3),
            )
        )

        bpnRequestIdentifierRepository.saveAll(
            mutableListOf(
                BpnRequestIdentifierMappingDb(
                    BusinessPartnerVerboseValues.bpnLRequestMapping.requestedIdentifier,
                    BusinessPartnerVerboseValues.bpnLRequestMapping.bpn
                ),
                BpnRequestIdentifierMappingDb(
                    BusinessPartnerVerboseValues.bpnSRequestMapping.requestedIdentifier,
                    BusinessPartnerVerboseValues.bpnSRequestMapping.bpn
                ),
                BpnRequestIdentifierMappingDb(
                    BusinessPartnerVerboseValues.bpnARequestMapping.requestedIdentifier,
                    BusinessPartnerVerboseValues.bpnARequestMapping.bpn
                )
            )
        )
    }

    /**
     * Given some business partners imported
     * When requesting bpn to SaaS id mappings and all the requested SaaS ids exist in the db
     * Then all the requested mappings are returned
     */
    @Test
    fun `find bpns by identifiers, all found`() {
        val identifiersSearchRequest =
            IdentifiersSearchRequest(IdentifierBusinessPartnerType.LEGAL_ENTITY, identifierType, listOf(identifierValue1, identifierValue2))

        val bpnIdentifierMappings = poolClient.bpns.findBpnsByIdentifiers(identifiersSearchRequest).body

        assertThat(bpnIdentifierMappings!!.map { it.idValue }).containsExactlyInAnyOrder(identifierValue1, identifierValue2)
    }

    /**
     * Given some business partners imported
     * When requesting bpn to SaaS id mappings and only some of the requested SaaS ids exist in the db
     * Then only the requested mappings that exist in the db are returned
     */
    @Test
    fun `find bpns by identifiers, only some found`() {
        val identifiersSearchRequest =
            IdentifiersSearchRequest(IdentifierBusinessPartnerType.LEGAL_ENTITY, identifierType, listOf(identifierValue1, "someNonexistentSaasId"))

        val bpnIdentifierMappings = poolClient.bpns.findBpnsByIdentifiers(identifiersSearchRequest).body

        assertThat(bpnIdentifierMappings!!.map { it.idValue }).containsExactlyInAnyOrder(identifierValue1)
    }

    /**
     * Given some business partners imported
     * When requesting too many bpn to SaaS id mappings in a single request, so that the requested number exceeds the configured limit
     * Then a "bad request" response is sent
     */
    @Test
    fun `find bpns by identifiers, bpn request limit exceeded`() {
        val identifiersSearchRequest =
            IdentifiersSearchRequest(IdentifierBusinessPartnerType.LEGAL_ENTITY, identifierType, listOf(identifierValue1, identifierValue2, identifierValue3))

        testHelpers.`find bpns by identifiers, bpn request limit exceeded`(identifiersSearchRequest)
    }

    /**
     * Given some business partners imported
     * When requested identifier type not found
     * Then a "not found" response is sent
     */
    @Test
    fun `find bpns by nonexistent identifier type`() {
        val identifiersSearchRequest =
            IdentifiersSearchRequest(IdentifierBusinessPartnerType.LEGAL_ENTITY, "NONEXISTENT_IDENTIFIER_TYPE", listOf(identifierValue1))

        testHelpers.`find bpns by nonexistent identifier type`(identifiersSearchRequest)
    }

    /**
     * Fetch the BPNL/S/A based on the provided request identifier
     */
    @Test
    fun `find bpn by requested identifier`() {
        val requestedIdentifiers = setOf(
            BusinessPartnerVerboseValues.bpnLRequestMapping.requestedIdentifier,
            BusinessPartnerVerboseValues.bpnSRequestMapping.requestedIdentifier,
        )
        val response = poolClient.bpns.findBpnByRequestedIdentifiers(BpnRequestIdentifierSearchRequest(requestedIdentifiers))
        response.body?.let { Assertions.assertEquals(requestedIdentifiers.size, it.size) }
    }

    /**
     * Find BPN based on the requested identifiers but with the set more than the search limit.
     */
    @Test
    fun `find bpn by requested identifier with extended search limit`() {
        val requestedIdentifiers = setOf(
            BusinessPartnerVerboseValues.bpnLRequestMapping.requestedIdentifier,
            BusinessPartnerVerboseValues.bpnSRequestMapping.requestedIdentifier,
            BusinessPartnerVerboseValues.bpnARequestMapping.requestedIdentifier,
        )
        try {
            val result = poolClient.bpns.findBpnByRequestedIdentifiers(BpnRequestIdentifierSearchRequest(requestedIdentifiers))
            assertThrows<WebClientResponseException> { result }
        } catch (e: WebClientResponseException) {
            Assert.assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }
    }

    /**
     * Fetch the BPNL/S/A based on the invalid request identifier
     */
    @Test
    fun `find bpn by invalid requested identifier`() {
        val requestedIdentifiers = setOf(UUID.randomUUID().toString())
        val response = poolClient.bpns.findBpnByRequestedIdentifiers(BpnRequestIdentifierSearchRequest(requestedIdentifiers))
        response.body?.let { Assertions.assertNotEquals(requestedIdentifiers.size, it.size) }
        response.body?.let { Assertions.assertTrue(it.isEmpty()) }
    }
}