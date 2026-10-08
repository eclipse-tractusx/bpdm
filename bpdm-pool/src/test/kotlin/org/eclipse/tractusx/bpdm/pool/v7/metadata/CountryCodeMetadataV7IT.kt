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

import com.neovisionaries.i18n.CountryCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.QualityLevel
import org.eclipse.tractusx.bpdm.pool.api.v6.client.PoolV6ApiClient
import org.eclipse.tractusx.bpdm.pool.entity.CountryDb
import org.eclipse.tractusx.bpdm.pool.entity.FieldQualityRuleDb
import org.eclipse.tractusx.bpdm.pool.entity.RegionDb
import org.eclipse.tractusx.bpdm.pool.repository.CountryRepository
import org.eclipse.tractusx.bpdm.pool.repository.FieldQualityRuleRepository
import org.eclipse.tractusx.bpdm.pool.repository.RegionRepository
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.UUID

class CountryCodeMetadataV7IT : UnscheduledPoolTestBaseV7() {

    @Autowired
    lateinit var poolV6Client: PoolV6ApiClient

    @Autowired
    lateinit var countryRepository: CountryRepository

    @Autowired
    lateinit var fieldQualityRuleRepository: FieldQualityRuleRepository

    @Autowired
    lateinit var regionRepository: RegionRepository

    @Test
    fun `field quality rules reject country codes outside the maintained catalogue in V6 and V7`() {
        val unknownCountryCode = unusedNonEnumCountryCode()

        val v6Exception = assertThrows<WebClientResponseException.NotFound> {
            poolV6Client.metadata.getFieldQualityRules(unknownCountryCode)
        }
        assertThat(v6Exception.responseBodyAsString).contains(unknownCountryCode)

        val v7Exception = assertThrows<WebClientResponseException.NotFound> {
            poolClient.metadata.getFieldQualityRules(unknownCountryCode)
        }
        assertThat(v7Exception.responseBodyAsString).contains(unknownCountryCode)
    }

    @Test
    fun `newly maintained non-enum country uses null-country default field quality rules in V6 and V7`() {
        val countryCode = unusedNonEnumCountryCode()
        val country = countryRepository.save(CountryDb(countryCode, "Maintained test country", null))
        var rule: FieldQualityRuleDb? = null

        try {
            val savedRule = fieldQualityRuleRepository.save(FieldQualityRuleDb(
                countryCode = null,
                schemaName = "country-code-regression",
                fieldPath = "fallback",
                qualityLevel = QualityLevel.MANDATORY
            ))
            rule = savedRule

            assertThat(CountryCode.entries.none { it.alpha2 == countryCode }).isTrue()

            val v6Rule = poolV6Client.metadata.getFieldQualityRules(countryCode).body!!
                .single { it.schemaName == savedRule.schemaName && it.fieldPath == savedRule.fieldPath }
            val v7Rule = poolClient.metadata.getFieldQualityRules(countryCode).body!!
                .single { it.schemaName == savedRule.schemaName && it.fieldPath == savedRule.fieldPath }

            assertThat(v6Rule.country).isEqualTo(countryCode)
            assertThat(v6Rule.qualityLevel).isEqualTo(QualityLevel.MANDATORY)
            assertThat(v7Rule.country).isEqualTo(countryCode)
            assertThat(v7Rule.qualityLevel).isEqualTo(QualityLevel.MANDATORY)
        } finally {
            rule?.let(fieldQualityRuleRepository::delete)
            countryRepository.delete(country)
        }
    }

    @Test
    fun `regions reference maintained country codes and administrative area responses preserve them`() {
        val countryCode = unusedNonEnumCountryCode()
        val country = countryRepository.save(CountryDb(countryCode, "Maintained test country", null))
        val regionCode = "T${UUID.randomUUID().toString().replace("-", "")}"
        var region: RegionDb? = null
        var unknownRegion: RegionDb? = null

        try {
            region = regionRepository.save(RegionDb(countryCode, regionCode, "Test region"))

            assertThat(regionRepository.findByRegionCodeIn(setOf(regionCode)))
                .anySatisfy { assertThat(it.countryCode).isEqualTo(countryCode) }

            val v6Area = administrativeAreas(poolV6Client.metadata::getAdminAreasLevel1)
                .single { it.code == regionCode }
            val v7Area = administrativeAreas(poolClient.metadata::getAdminAreasLevel1)
                .single { it.code == regionCode }
            assertThat(v6Area.countryCode).isEqualTo(countryCode)
            assertThat(v7Area.countryCode).isEqualTo(countryCode)

            val unknownCountryCode = unusedNonEnumCountryCode(excluding = countryCode)
            assertThatThrownBy {
                unknownRegion = regionRepository.save(
                    RegionDb(unknownCountryCode, "$regionCode-unknown", "Unknown-country region")
                )
            }.isInstanceOf(DataIntegrityViolationException::class.java)
            assertThatThrownBy {
                fieldQualityRuleRepository.save(
                    FieldQualityRuleDb(unknownCountryCode, "country-code-regression", "unknown", QualityLevel.OPTIONAL)
                )
            }.isInstanceOf(DataIntegrityViolationException::class.java)
        } finally {
            unknownRegion?.let(regionRepository::delete)
            region?.let(regionRepository::delete)
            countryRepository.delete(country)
        }
    }

    private fun <T> administrativeAreas(fetch: (PaginationRequest) -> PageDto<T>): List<T> {
        val firstPage = fetch(PaginationRequest(size = 100))
        return firstPage.content.toList() + (1 until firstPage.totalPages).flatMap {
            fetch(PaginationRequest(page = it, size = 100)).content
        }
    }

    private fun unusedNonEnumCountryCode(excluding: String? = null): String =
        listOf("ZZ", "QZ", "XZ", "QQ", "QY").first { candidate ->
            candidate != excluding &&
                CountryCode.entries.none { it.alpha2 == candidate } &&
                countryRepository.findByCountryCode(candidate) == null
        }
}
