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

package org.eclipse.tractusx.bpdm.pool.v7.metadata

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierBusinessPartnerTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierTypeCategoryV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierTypeDetailV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierTypeV7Dto
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.web.reactive.function.client.WebClientResponseException
import kotlin.random.Random

class IdentifierTypeCreateV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * WHEN operator creates valid identifier type
     * THEN created identifier type is returned
     */
    @ParameterizedTest(name = "{displayName} - {arguments}")
    @EnumSource(IdentifierBusinessPartnerTypeV7::class)
    fun `create new identifier type v7`(type: IdentifierBusinessPartnerTypeV7) {
        //WHEN
        val request = createIdentifierTypeRequest(testName, type)
        val response = poolClient.metadata.createIdentifierType(request)

        //THEN
        Assertions.assertThat(response).isEqualTo(request)
    }

    /**
     * WHEN operator creates valid identifier type
     * THEN operator can find new identifier type in available metadata
     */
    @ParameterizedTest(name = "{displayName} - {arguments}")
    @EnumSource(IdentifierBusinessPartnerTypeV7::class)
    fun `create new identifier type and find it v7`(type: IdentifierBusinessPartnerTypeV7) {
        //WHEN
        val request = createIdentifierTypeRequest(testName, type)
        poolClient.metadata.createIdentifierType(request)

        //THEN
        var currentPage = 0
        var found = false
        do {
            val response = poolClient.metadata.getIdentifierTypes(PaginationRequest(currentPage, 100), type, null)

            val foundIdType = response.content.find { it.technicalKey == request.technicalKey }
            if (foundIdType != null) {
                Assertions.assertThat(foundIdType)
                    .usingRecursiveComparison()
                    .ignoringCollectionOrder()
                    .isEqualTo(request)
                found = true
            }

            currentPage++
        } while (response.content.isNotEmpty())

        Assertions.assertThat(found).isTrue
    }

    /**
     * WHEN operator creates identifier type with categories
     * THEN categories are persisted and returned
     */
    @ParameterizedTest(name = "{displayName} - {arguments}")
    @EnumSource(IdentifierBusinessPartnerTypeV7::class)
    fun `create new identifier type with categories v7`(type: IdentifierBusinessPartnerTypeV7) {
        //WHEN
        val request = createIdentifierTypeRequest(testName, type)
            .copy(categories = sortedSetOf(IdentifierTypeCategoryV7.VAT, IdentifierTypeCategoryV7.TIN))
        val response = poolClient.metadata.createIdentifierType(request)

        //THEN
        Assertions.assertThat(response.categories).containsExactlyInAnyOrder(IdentifierTypeCategoryV7.VAT, IdentifierTypeCategoryV7.TIN)
    }

    /**
     * WHEN operator tries to create identifier type with existing technical key
     * THEN 409 CONFLICT returned
     */
    @ParameterizedTest(name = "{displayName} - {arguments}")
    @EnumSource(IdentifierBusinessPartnerTypeV7::class)
    fun `try create new identifier type with duplicate technical key v7`(type: IdentifierBusinessPartnerTypeV7) {
        //WHEN
        val request = createIdentifierTypeRequest(testName, type)
        poolClient.metadata.createIdentifierType(request)

        //THEN
        Assertions.assertThatThrownBy { poolClient.metadata.createIdentifierType(request) }
            .isInstanceOf(WebClientResponseException.Conflict::class.java)
    }

    private fun createIdentifierTypeRequest(seed: String, type: IdentifierBusinessPartnerTypeV7): IdentifierTypeV7Dto {
        val random = Random(seed.hashCode())

        return IdentifierTypeV7Dto(
            technicalKey = seed,
            businessPartnerType = type,
            name = "$seed ${IdentifierTypeV7Dto::name.name}",
            abbreviation = "$seed ${IdentifierTypeV7Dto::abbreviation.name}",
            transliteratedName = "$seed ${IdentifierTypeV7Dto::transliteratedName.name}",
            transliteratedAbbreviation = "$seed ${IdentifierTypeV7Dto::transliteratedAbbreviation.name}",
            format = "$seed-format",
            categories = sortedSetOf(IdentifierTypeCategoryV7.entries.random(random)),
            details = (1..random.nextInt(3, 10))
                .map { listOf("DE", "US", "FR", "CN", "PL").random(random) }
                .toSet()
                .map { IdentifierTypeDetailV7Dto(it, random.nextBoolean()) }
        )
    }
}
