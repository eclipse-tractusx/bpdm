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

package org.eclipse.tractusx.bpdm.pool.v6.sharingmember.legalentity

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.pool.v6.sharingmember.SharingMemberTest
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException

class LegalEntityGetIT: SharingMemberTest() {

    /**
     *
     * GIVEN legal entity with BPNL
     * WHEN sharing member requests legal entity by BPNL
     * THEN sharing member sees legal entity
     */
    @Test
    fun `get legal entity by BPNL`(){
        //GIVEN
        val legalEntityResponseA =  testDataClient.createLegalEntity("$testName A")

        //WHEN
        val getResponse = poolClient.legalEntities.getLegalEntity(legalEntityResponseA.legalEntity.bpnl)

        //THEN
        val expectedLegalEntity = testDataFactory.result.buildExpectedLegalEntitySearchResponse(legalEntityResponseA)
        assertRepository.assertLegalEntityGet(getResponse, expectedLegalEntity)
    }

    /**
     * GIVEN legal entities with identifier X
     * WHEN sharing member requests legal entities by identifier X
     * THEN sharing member sees legal entity
     */
    @Test
    fun `get legal entity by identifier`(){
        //GIVEN
        val legalEntityResponseA =  testDataClient.createLegalEntity("$testName A")
        val identifierX = legalEntityResponseA.legalEntity.identifiers.first()

        //WHEN
        val getResponse = poolClient.legalEntities.getLegalEntity(identifierX.value, identifierX.type)

        //THEN
        val expectedLegalEntity = testDataFactory.result.buildExpectedLegalEntitySearchResponse(legalEntityResponseA)
        assertRepository.assertLegalEntityGet(getResponse, expectedLegalEntity)
    }

    /**
     * WHEN sharing member requests legal entity by unknown BPNL
     * THEN sharing member sees 404 not found error
     */
    @Test
    fun `try get legal entity by unknown BPNL`(){
        //WHEN
        val unknownGet = {  poolClient.legalEntities.getLegalEntity("UNKNOWN"); Unit }

        //THEN
        Assertions.assertThatExceptionOfType(WebClientResponseException.NotFound::class.java).isThrownBy(unknownGet)
    }

    /**
     * WHEN sharing member requests legal entity by unknown identifier
     * THEN sharing member sees 404 not found error
     */
    @Test
    fun `try get legal entity by unknown identifier`(){
        //WHEN
        val unknownGet = {  poolClient.legalEntities.getLegalEntity("UNKNOWN", "UNKNOWN"); Unit }

        //THEN
        Assertions.assertThatExceptionOfType(WebClientResponseException.NotFound::class.java).isThrownBy(unknownGet)
    }

}