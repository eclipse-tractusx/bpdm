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

package org.eclipse.tractusx.bpdm.pool.v6.sharingmember.bpn

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.request.IdentifiersSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.BpnIdentifierMappingDto
import org.eclipse.tractusx.bpdm.pool.v6.sharingmember.SharingMemberTest
import org.junit.jupiter.api.Test

class BpnSearchIT: SharingMemberTest() {


    /**
     * GIVEN legal entity with identifier X
     * WHEN sharing member search for BPN by identifier X
     * THEN sharing member sees BPNL of legal entity
     */
    @Test
    fun `search BPN by legal identifiers`(){
        //GIVEN
        val legalEntityResponseA = testDataClient.createLegalEntity("$testName A")
        testDataClient.createLegalEntity("$testName B")
        testDataClient.createLegalEntity("$testName C")

        val identifierX = legalEntityResponseA.legalEntity.identifiers.first()

        //WHEN
        val searchResponse = poolClient.bpns.findBpnsByIdentifiers(IdentifiersSearchRequest(IdentifierBusinessPartnerType.LEGAL_ENTITY, identifierX.type, listOf(identifierX.value))).body

        //THEN
        val expectedBpns = setOf(BpnIdentifierMappingDto(identifierX.value, legalEntityResponseA.legalEntity.bpnl))
        Assertions.assertThat(searchResponse).isEqualTo(expectedBpns)
    }

    /**
     * GIVEN address with identifier X
     * WHEN sharing member search for BPN by identifier X
     * THEN sharing member sees BPNA of address
     */
    @Test
    fun `search BPN by address identifiers`(){
        //GIVEN
        val legalEntityResponseA = testDataClient.createLegalEntity("$testName A")
        testDataClient.createLegalEntity("$testName B")
        testDataClient.createLegalEntity("$testName C")

        val identifierX = legalEntityResponseA.legalAddress.identifiers.first()

        //WHEN
        val searchResponse = poolClient.bpns.findBpnsByIdentifiers(IdentifiersSearchRequest(IdentifierBusinessPartnerType.ADDRESS, identifierX.type, listOf(identifierX.value))).body

        //THEN
        val expectedBpns = setOf(BpnIdentifierMappingDto(identifierX.value, legalEntityResponseA.legalAddress.bpna))
        Assertions.assertThat(searchResponse).isEqualTo(expectedBpns)
    }

}