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

package org.eclipse.tractusx.bpdm.pool.v7.legalentity

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withParticipantData
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException

class LegalEntityGetV7IT: UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN legal entity with BPNL
     * WHEN sharing member requests legal entity by BPNL
     * THEN sharing member sees legal entity
     */
    @Test
    fun `get legal entity by BPNL`(){
        //GIVEN
        val givenLegalEntity = testDataClient.createLegalEntity(testName)

        //WHEN
        val response = poolClient.legalEntities.getLegalEntity(givenLegalEntity.header.bpnl)

        //THEN
        assertRepository.assertLegalEntityWithLegalAddressIsEqual(givenLegalEntity, response)
    }

    /**
     * GIVEN legal entity with BPNL
     * WHEN sharing member requests legal entity by BPNL in lowercase
     * THEN sharing member sees legal entity
     */
    @Test
    fun `get legal entity by BPNL lowercase`(){
        //GIVEN
        val givenLegalEntity = testDataClient.createLegalEntity(testName)

        //WHEN
        val response = poolClient.legalEntities.getLegalEntity(givenLegalEntity.header.bpnl.lowercase())

        //THEN
        assertRepository.assertLegalEntityWithLegalAddressIsEqual(givenLegalEntity, response)
    }

    /**
     * GIVEN legal entity with identifier X
     * WHEN sharing member requests legal entity by identifier X
     * THEN sharing member sees legal entity
     */
    @Test
    fun `get legal entity by identifier`(){
        //GIVEN
        val givenLegalEntity = testDataClient.createLegalEntity(testName)
        val identifierX = givenLegalEntity.header.identifiers.first()

        //WHEN
        val response = poolClient.legalEntities.getLegalEntity(identifierX.value, identifierX.typeVerbose.technicalKey)

        //THEN
        assertRepository.assertLegalEntityWithLegalAddressIsEqual(givenLegalEntity, response)
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
        val unknownGet = { poolClient.legalEntities.getLegalEntity("UNKNOWN", "UNKNOWN"); Unit }

        //THEN
        Assertions.assertThatExceptionOfType(WebClientResponseException.NotFound::class.java).isThrownBy(unknownGet)
    }

    /**
     * GIVEN participant legal entity with BPNL
     * WHEN sharing member requests legal entity by BPNL
     * THEN sharing member sees legal entity with that values
     */
    @Test
    fun `get participant legal entity by BPNL`(){
        //GIVEN
        val givenLegalEntity = testDataClient.createLegalEntity(requestFactory.buildLegalEntity(testName).withParticipantData(true))

        //WHEN
        val response = poolClient.legalEntities.getLegalEntity(givenLegalEntity.header.bpnl)

        //THEN
        assertRepository.assertLegalEntityWithLegalAddressIsEqual(givenLegalEntity, response)
    }

    /**
     * GIVEN non-participant legal entity with BPNL
     * WHEN sharing member requests legal entity by BPNL
     * THEN sharing member sees legal entity with that values
     */
    @Test
    fun `get non-participant legal entity by BPNL`(){
        //GIVEN
        val givenLegalEntity = testDataClient.createLegalEntity(requestFactory.buildLegalEntity(testName).withParticipantData(false))

        //WHEN
        val response = poolClient.legalEntities.getLegalEntity(givenLegalEntity.header.bpnl)

        //THEN
        assertRepository.assertLegalEntityWithLegalAddressIsEqual(givenLegalEntity, response)
    }

    /**
     * GIVEN legal entity with BPNL that has been updated before
     * WHEN sharing member requests legal entity by BPNL
     * THEN sharing member sees legal entity with that values
     */
    @Test
    fun `get updated legal entity by BPNL`(){
        //GIVEN
        val givenLegalEntity = testDataClient.createLegalEntity(testName)
        val updatedEntity = testDataClient.updateLegalEntity(givenLegalEntity.header.bpnl, "Updated $testName")

        //WHEN
        val response = poolClient.legalEntities.getLegalEntity(givenLegalEntity.header.bpnl)

        //THEN
        assertRepository.assertLegalEntityWithLegalAddressIsEqual(updatedEntity, response)
    }

}