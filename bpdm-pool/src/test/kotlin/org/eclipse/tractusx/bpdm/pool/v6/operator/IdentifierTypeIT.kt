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

package org.eclipse.tractusx.bpdm.pool.v6.operator

import com.neovisionaries.i18n.CountryCode
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierTypeDetailDto
import org.eclipse.tractusx.bpdm.pool.api.v6.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierTypeDto
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.client.WebClientResponseException
import kotlin.random.Random

class IdentifierTypeIT @Autowired constructor(
    private val poolApiClient: PoolApiClient
): OperatorTest() {

    /**
     * WHEN operator creates valid identifier type
     * THEN created identifier type returned
     */
    @ParameterizedTest(name = "{displayName} - {arguments}")
    @EnumSource(IdentifierBusinessPartnerType::class)
    fun `create new identifier type`(type: IdentifierBusinessPartnerType){
        val request = createIdentifierTypeRequest(testName, type)
        val response = poolApiClient.metadata.createIdentifierType(request)

        Assertions.assertThat(response).isEqualTo(request)
    }

    /**
     * WHEN operator creates valid identifier type
     * THEN operator can find new identifier type in available metadata
     */
    @ParameterizedTest(name = "{displayName} - {arguments}")
    @EnumSource(IdentifierBusinessPartnerType::class)
    fun `create new identifier type and find it`(type: IdentifierBusinessPartnerType){
        val request = createIdentifierTypeRequest(testName, type)
        poolApiClient.metadata.createIdentifierType(request)

        var currentPage = 0
        var found = false
        do{
            val response = poolApiClient.metadata.getIdentifierTypes(PaginationRequest(currentPage, 100), type, null)

            val foundIdType = response.content.find { it.technicalKey == request.technicalKey }
            if(foundIdType != null){
                Assertions.assertThat(foundIdType)
                    .usingRecursiveComparison()
                    .ignoringCollectionOrder()
                    .isEqualTo(request)
                found = true
            }

            currentPage++
        }while (response.content.isNotEmpty())

        Assertions.assertThat(found).isTrue
    }

    /**
     * WHEN operator tries to create identifier type with existing technical key
     * THEN 409 CONFLICT returned
     */
    @ParameterizedTest(name = "{displayName} - {arguments}")
    @EnumSource(IdentifierBusinessPartnerType::class)
    fun `try create new identifier type with duplicate technical key`(type: IdentifierBusinessPartnerType){
        val request = createIdentifierTypeRequest(testName, type)
        poolApiClient.metadata.createIdentifierType(request)

        Assertions.assertThatThrownBy { poolApiClient.metadata.createIdentifierType(request) }
            .isInstanceOf(WebClientResponseException.Conflict::class.java)
    }

    private fun createIdentifierTypeRequest(seed: String, type: IdentifierBusinessPartnerType): IdentifierTypeDto{
       val random = Random(seed.hashCode())

        return IdentifierTypeDto(
            technicalKey = seed,
            businessPartnerType = type,
            name = "$seed ${IdentifierTypeDto::name.name}",
            abbreviation = "$seed ${IdentifierTypeDto::abbreviation.name}",
            transliteratedName = "$seed ${IdentifierTypeDto::transliteratedName.name}",
            transliteratedAbbreviation = "$seed ${IdentifierTypeDto::transliteratedAbbreviation.name}",
            details = (1 .. random.nextInt(3, 10))
                .map { CountryCode.entries.random(random) }
                .toSet()
                .map { IdentifierTypeDetailDto(it, random.nextBoolean()) }
        )
    }
}