/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.v7.bpn

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.request.IdentifiersSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.BpnIdentifierMappingDto
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withLegalIdentifiers
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException

class BpnSearchV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN legal entity with identifier X
     * WHEN sharing member searches for BPN by identifier X
     * THEN sharing member sees BPNL of legal entity
     */
    @Test
    fun `search BPN by legal entity identifier`() {
        //GIVEN
        val legalEntityResponseA = testDataClient.createLegalEntity("$testName A")
        testDataClient.createLegalEntity("$testName B")
        testDataClient.createLegalEntity("$testName C")

        val identifierX = legalEntityResponseA.header.identifiers.first()

        //WHEN
        val searchResponse = poolClient.bpns.findBpnsByIdentifiers(
            IdentifiersSearchRequest(IdentifierBusinessPartnerType.LEGAL_ENTITY, identifierX.type, listOf(identifierX.value))
        ).body

        //THEN
        val expectedBpns = setOf(BpnIdentifierMappingDto(identifierX.value, legalEntityResponseA.header.bpnl))
        Assertions.assertThat(searchResponse).isEqualTo(expectedBpns)
    }

    /**
     * GIVEN legal address with identifier X
     * WHEN sharing member searches for BPN by identifier X
     * THEN sharing member sees BPNA of legal address
     */
    @Test
    fun `search BPN by legal address identifier`() {
        //GIVEN
        val legalEntityResponseA = testDataClient.createLegalEntity("$testName A")
        testDataClient.createLegalEntity("$testName B")
        testDataClient.createLegalEntity("$testName C")

        val identifierX = legalEntityResponseA.legalAddress.identifiers.first()

        //WHEN
        val searchResponse = poolClient.bpns.findBpnsByIdentifiers(
            IdentifiersSearchRequest(IdentifierBusinessPartnerType.ADDRESS, identifierX.type, listOf(identifierX.value))
        ).body

        //THEN
        val expectedBpns = setOf(BpnIdentifierMappingDto(identifierX.value, legalEntityResponseA.legalAddress.bpna))
        Assertions.assertThat(searchResponse).isEqualTo(expectedBpns)
    }

    /**
     * GIVEN additional address with identifier X
     * WHEN sharing member searches for BPN by identifier X
     * THEN sharing member sees BPNA of additional address
     */
    @Test
    fun `search BPN by additional address identifier`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val additionalAddress = testDataClient.createAdditionalAddress(legalEntityResponse, testName)

        val identifierX = additionalAddress.address.identifiers.first()

        //WHEN
        val searchResponse = poolClient.bpns.findBpnsByIdentifiers(
            IdentifiersSearchRequest(IdentifierBusinessPartnerType.ADDRESS, identifierX.type, listOf(identifierX.value))
        ).body

        //THEN
        val expectedBpns = setOf(BpnIdentifierMappingDto(identifierX.value, additionalAddress.address.bpna))
        Assertions.assertThat(searchResponse).isEqualTo(expectedBpns)
    }

    /**
     * GIVEN multiple legal entities with distinct identifiers
     * WHEN sharing member searches for BPNs by multiple identifier values at once
     * THEN sharing member sees all matching BPNL mappings
     */
    @Test
    fun `search BPNs by multiple legal entity identifier values`() {
        //GIVEN
        val legalIdentifierA = requestFactory.buildLegalEntityIdentifier("$testName A")
        val legalIdentifierB = requestFactory.buildLegalEntityIdentifier("$testName B").copy(type = legalIdentifierA.type)

        val legalEntityResponseA = testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName A").withLegalIdentifiers(legalIdentifierA))
        val legalEntityResponseB = testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName B").withLegalIdentifiers(legalIdentifierB))
        testDataClient.createLegalEntity("$testName C")

        //WHEN
        val searchResponse = poolClient.bpns.findBpnsByIdentifiers(
            IdentifiersSearchRequest(IdentifierBusinessPartnerType.LEGAL_ENTITY, legalIdentifierA.type, listOf(legalIdentifierA.value, legalIdentifierB.value))
        ).body

        //THEN
        val expectedBpns = setOf(
            BpnIdentifierMappingDto(legalIdentifierA.value, legalEntityResponseA.header.bpnl),
            BpnIdentifierMappingDto(legalIdentifierB.value, legalEntityResponseB.header.bpnl)
        )
        Assertions.assertThat(searchResponse).isEqualTo(expectedBpns)
    }

    /**
     * GIVEN legal entities
     * WHEN sharing member searches for BPN by non-existent identifier value
     * THEN sharing member receives empty result
     */
    @Test
    fun `search BPN by unknown identifier returns empty`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val identifierX = legalEntityResponse.header.identifiers.first()

        //WHEN
        val searchResponse = poolClient.bpns.findBpnsByIdentifiers(
            IdentifiersSearchRequest(IdentifierBusinessPartnerType.LEGAL_ENTITY, identifierX.type, listOf("UNKNOWN_VALUE"))
        ).body

        //THEN
        Assertions.assertThat(searchResponse).isEmpty()
    }

    /**
     * GIVEN legal entity with identifier X of type LEGAL_ENTITY
     * WHEN sharing member searches for BPN by identifier X using type ADDRESS
     * THEN sharing member receives 404 NOT FOUND error
     */
    @Test
    fun `search BPN by legal entity identifier with wrong business partner type returns empty`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val identifierX = legalEntityResponse.header.identifiers.first()

        //WHEN
        Assertions.assertThatThrownBy {  poolClient.bpns.findBpnsByIdentifiers(
            IdentifiersSearchRequest(IdentifierBusinessPartnerType.ADDRESS, identifierX.type, listOf(identifierX.value))
        )}
            .isInstanceOf(WebClientResponseException.NotFound::class.java)
    }
}
