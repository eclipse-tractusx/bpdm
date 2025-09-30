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

package org.eclipse.tractusx.bpdm.pool.v6.participant.metadata

import com.neovisionaries.i18n.CountryCode
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierTypeDetailDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierTypeDto
import org.eclipse.tractusx.bpdm.pool.v6.participant.ParticipantTest
import org.junit.jupiter.api.Test

class IdentifierTypeGetIT: ParticipantTest() {

    /**
     * GIVEN identifier types
     * WHEN participant searches for page of legal identifier types
     * THEN participant gets page of legal identifier types
     */
    @Test
    fun `find legal identifier types`(){
        //WHEN
        val fetchResponse = poolClient.metadata.getIdentifierTypes(PaginationRequest(), IdentifierBusinessPartnerType.LEGAL_ENTITY, null)

        //THEN
        fetchResponse.content.forEach { Assertions.assertThat(it.businessPartnerType).isEqualTo(IdentifierBusinessPartnerType.LEGAL_ENTITY) }
    }

    /**
     * GIVEN identifier types
     * WHEN participant searches for page of address identifier types
     * THEN participant gets page of address identifier types
     */
    @Test
    fun `find address identifier types`(){
        //WHEN
        val fetchResponse = poolClient.metadata.getIdentifierTypes(PaginationRequest(), IdentifierBusinessPartnerType.ADDRESS, null)

        //THEN
        fetchResponse.content.forEach { Assertions.assertThat(it.businessPartnerType).isEqualTo(IdentifierBusinessPartnerType.ADDRESS) }
    }

    /**
     * GIVEN identifier type mandatory for country X
     * WHEN participant searches for identifier types for country
     * THEN participant gets identifier type
     */
    @Test
    fun `find mandatory identifier types for country`(){
        //GIVEN
        val countryX = CountryCode.DE
        val identifierType = IdentifierTypeDto(testName, IdentifierBusinessPartnerType.LEGAL_ENTITY, testName, testName, testName, testName,
            listOf(IdentifierTypeDetailDto(countryX, true)))
        operatorClient.metadata.createIdentifierType(identifierType)

        //WHEN
        val fetchResponse = poolClient.metadata.getIdentifierTypes(PaginationRequest(), IdentifierBusinessPartnerType.LEGAL_ENTITY, countryX)

        //THEN
        Assertions.assertThat(fetchResponse.content).contains(identifierType)
    }

    /**
     * GIVEN identifier type optional for country X
     * WHEN participant searches for identifier types for country
     * THEN participant gets identifier type
     */
    @Test
    fun `find optional identifier types for country`(){
        //GIVEN
        val countryX = CountryCode.US
        val identifierType = IdentifierTypeDto(testName, IdentifierBusinessPartnerType.LEGAL_ENTITY, testName, testName, testName, testName,
            listOf(IdentifierTypeDetailDto(countryX, false)))
        operatorClient.metadata.createIdentifierType(identifierType)

        //WHEN
        val fetchResponse = poolClient.metadata.getIdentifierTypes(PaginationRequest(), IdentifierBusinessPartnerType.LEGAL_ENTITY, countryX)

        //THEN
        Assertions.assertThat(fetchResponse.content).contains(identifierType)
    }

}