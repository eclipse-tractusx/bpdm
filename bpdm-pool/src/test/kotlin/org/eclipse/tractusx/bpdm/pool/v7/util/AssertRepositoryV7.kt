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

package org.eclipse.tractusx.bpdm.pool.v7.util

import org.assertj.core.api.Assertions
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.test.util.InstantSecondsComparator
import org.eclipse.tractusx.bpdm.test.util.LocalDatetimeSecondsComparator
import java.time.Instant
import java.time.LocalDateTime
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.CountryV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.FieldQualityRuleV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityHeaderVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressInvariantVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerUpdateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerUpdateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto

class AssertRepositoryV7(
    instantSecondsComparator: InstantSecondsComparator,
    localDatetimeSecondsComparator: LocalDatetimeSecondsComparator
) {

    fun assertCountryPage(actual: PageDto<CountryV7Dto>, page: Int, pageSize: Int) {
        Assertions.assertThat(actual.page).isEqualTo(page)
        Assertions.assertThat(actual.totalElements).isGreaterThan((page * pageSize).toLong())
        Assertions.assertThat(actual.totalPages).isEqualTo(((actual.totalElements + pageSize - 1) / pageSize).toInt())
        val expectedSize = minOf(pageSize.toLong(), actual.totalElements - page.toLong() * pageSize).toInt()
        Assertions.assertThat(actual.contentSize).isEqualTo(expectedSize)
        Assertions.assertThat(actual.content).hasSize(expectedSize)
        Assertions.assertThat(actual.content.map { it.countryCode }).isSorted()
    }

    fun assertCountryCatalog(actual: Collection<CountryV7Dto>, expectedCountry: CountryV7Dto) {
        Assertions.assertThat(actual).contains(expectedCountry)
        Assertions.assertThat(actual).allSatisfy {
            Assertions.assertThat(it.countryCode).isNotBlank()
            Assertions.assertThat(it.name).isNotBlank()
        }
    }

    fun assertFieldQualityRules(actual: Collection<FieldQualityRuleV7Dto>, expected: Collection<FieldQualityRuleV7Dto>) {
        Assertions.assertThat(actual).containsExactlyInAnyOrderElementsOf(expected)
    }

    fun assertNotFound(actual: WebClientResponseException.NotFound, expectedDetail: String) {
        Assertions.assertThat(actual.responseBodyAsString).contains(expectedDetail)
    }

    val comparisonConfigurationForCreated: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFieldsMatchingRegexes(
            ".*errors.message",
            ".*updatedAt",
            ".*createdAt",
            ".*currentness",
            ".*bpnl",
            ".*bpns",
            ".*bpna",
            ".*bpnLegalEntity",
            ".*bpnSite"
        )
        .withIgnoreCollectionOrder(false)
        .withComparatorForType(instantSecondsComparator, Instant::class.java)
        .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
        .build()

    val comparisonConfigurationForExisting: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFieldsMatchingRegexes(
            ".*errors.message",
            ".*updatedAt",
            ".*createdAt",
            ".*currentness",
            ".*bpnSite"
        )
        .withIgnoreCollectionOrder(false)
        .withComparatorForType(instantSecondsComparator, Instant::class.java)
        .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
        .build()

    private val comparisonConfigurationForRead: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFieldsMatchingRegexes(".*errors.message", ".*currentness", ".*bpnSite")
        .withIgnoreCollectionOrder(false)
        .withComparatorForType(Comparator.naturalOrder<Instant>(), Instant::class.java)
        .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
        .build()

    fun assertLegalEntityCreateResponseWrapperIsEqual(actual: LegalEntityPartnerCreateV7ResponseWrapper, expected: LegalEntityPartnerCreateV7ResponseWrapper){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForCreated)
            .isEqualTo(expected.sortContent())
    }

    fun assertLegalEntityUpdateResponseWrapperIsEqual(actual: LegalEntityPartnerUpdateV7ResponseWrapper, expected: LegalEntityPartnerUpdateV7ResponseWrapper){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForExisting)
            .isEqualTo(expected.sortContent())
        val expectedByBpn = expected.entities.associateBy { it.legalEntity.header.bpnl }
        actual.entities.forEach {
            val previous = expectedByBpn.getValue(it.legalEntity.header.bpnl).legalEntity
            assertUpdatedTimestamps(it.legalEntity.header.createdAt, it.legalEntity.header.updatedAt, previous.header.createdAt, previous.header.updatedAt)
            assertUpdatedTimestamps(it.legalEntity.legalAddress.createdAt, it.legalEntity.legalAddress.updatedAt, previous.legalAddress.createdAt, previous.legalAddress.updatedAt)
        }
    }

    fun assertLegalEntitySearchResponse(actual: PageDto<LegalEntityWithLegalAddressVerboseV7Dto>, expected: PageDto<LegalEntityWithLegalAddressVerboseV7Dto>){
        Assertions.assertThat(actual.sortLegalEntityWithAddress())
            .usingRecursiveComparison(comparisonConfigurationForRead)
            .isEqualTo(expected.sortLegalEntityWithAddress())
    }

    fun assertLegalEntityWithLegalAddressIsEqual(actual: LegalEntityWithLegalAddressVerboseV7Dto, expected: LegalEntityWithLegalAddressVerboseV7Dto){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForRead)
            .isEqualTo(expected.sortContent())
    }

    fun assertSiteCreateResponseWrapperIsEqual(actual: SitePartnerCreateV7ResponseWrapper, expected: SitePartnerCreateV7ResponseWrapper){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForCreated)
            .isEqualTo(expected.sortContent())
    }

    fun assertSiteSearchResponse(actual: PageDto<SiteWithMainAddressVerboseV7Dto>, expected: PageDto<SiteWithMainAddressVerboseV7Dto>){
        Assertions.assertThat(actual.sortSiteWithAddress())
            .usingRecursiveComparison(comparisonConfigurationForRead)
            .isEqualTo(expected.sortSiteWithAddress())
    }

    fun assertSiteUpdateResponseWrapperIsEqual(actual: SitePartnerUpdateV7ResponseWrapper, expected: SitePartnerUpdateV7ResponseWrapper){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForExisting)
            .isEqualTo(expected.sortContent())
        val expectedByBpn = expected.entities.associateBy { it.site.bpns }
        actual.entities.forEach {
            val previous = expectedByBpn.getValue(it.site.bpns)
            assertUpdatedTimestamps(it.site.createdAt, it.site.updatedAt, previous.site.createdAt, previous.site.updatedAt)
            assertUpdatedTimestamps(it.mainAddress.createdAt, it.mainAddress.updatedAt, previous.mainAddress.createdAt, previous.mainAddress.updatedAt)
        }
    }

    fun assertSiteGetIsEqual(actual: SiteWithMainAddressVerboseV7Dto, expected: SiteWithMainAddressVerboseV7Dto){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForRead)
            .isEqualTo(expected.sortContent())
    }

    fun assertLegalEntitySitesResponse(actual: PageDto<SiteVerboseV7Dto>, expected: PageDto<SiteVerboseV7Dto>){
        Assertions.assertThat(actual.sortSite())
            .usingRecursiveComparison(comparisonConfigurationForRead)
            .isEqualTo(expected.sortSite())
    }

    fun assertAddressCreateResponseWrapperIsEqual(actual: AddressPartnerCreateV7ResponseWrapper, expected: AddressPartnerCreateV7ResponseWrapper){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForCreated)
            .isEqualTo(expected.sortContent())
    }

    fun assertAddressUpdateResponseWrapperIsEqual(actual: AddressPartnerUpdateV7ResponseWrapper, expected: AddressPartnerUpdateV7ResponseWrapper){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForExisting)
            .isEqualTo(expected.sortContent())
        val expectedByBpn = expected.entities.associateBy { it.address.bpna }
        actual.entities.forEach {
            val previous = expectedByBpn.getValue(it.address.bpna).address
            assertUpdatedTimestamps(it.address.createdAt, it.address.updatedAt, previous.createdAt, previous.updatedAt)
        }
    }

    fun assertAddressSearch(actual: PageDto<LogisticAddressVerboseV7Dto>, expected: PageDto<LogisticAddressVerboseV7Dto>){
        Assertions.assertThat(actual.sortLogisticAddress())
            .usingRecursiveComparison(comparisonConfigurationForRead)
            .isEqualTo(expected.sortLogisticAddress())
    }

    fun assertAddressGet(actual: LogisticAddressVerboseV7Dto, expected: LogisticAddressVerboseV7Dto){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForRead)
            .isEqualTo(expected.sortContent())
    }

    fun assertParticipantAddressSearch(actual: PageDto<LogisticAddressInvariantVerboseV7Dto>, expected: PageDto<LogisticAddressInvariantVerboseV7Dto>){
        Assertions.assertThat(actual.sortLogisticAddressInvariant())
            .usingRecursiveComparison(comparisonConfigurationForRead)
            .isEqualTo(expected.sortLogisticAddressInvariant())
    }

    /**
     * Asserts that the address carries no script variant, having carried [previousScriptVariants] before.
     */
    fun assertAddressScriptVariantsCleared(actual: LogisticAddressVerboseV7Dto, previousScriptVariants: Collection<LogisticAddressScriptVariantV7Dto>){
        Assertions.assertThat(previousScriptVariants).isNotEmpty()
        Assertions.assertThat(actual.scriptVariants).isEmpty()
    }

    /**
     * Asserts that the site carries no script variant, having carried [previousScriptVariants] before.
     */
    fun assertSiteScriptVariantsCleared(actual: SiteWithMainAddressVerboseV7Dto, previousScriptVariants: Collection<SiteScriptVariantV7Dto>){
        Assertions.assertThat(previousScriptVariants).isNotEmpty()
        Assertions.assertThat(actual.site.scriptVariants).isEmpty()
    }

    private fun assertUpdatedTimestamps(createdAt: Instant, updatedAt: Instant, previousCreatedAt: Instant, previousUpdatedAt: Instant) {
        Assertions.assertThat(createdAt).isEqualTo(previousCreatedAt)
        Assertions.assertThat(updatedAt).isAfter(previousUpdatedAt)
    }

    private fun LegalEntityPartnerCreateV7ResponseWrapper.sortContent() =
        copy(entities = entities.map { it.sortContent() })

    private fun LegalEntityPartnerUpdateV7ResponseWrapper.sortContent() =
        copy(entities = entities.map { it.sortContent() })

    private fun LegalEntityPartnerCreateVerboseV7Dto.sortContent() =
        copy(legalEntity = legalEntity.sortContent())

    private fun SitePartnerCreateV7ResponseWrapper.sortContent() =
        copy(entities = entities.map { it.sortContent() })

    private fun SitePartnerUpdateV7ResponseWrapper.sortContent() =
        copy(entities = entities.map { it.sortContent() })

    private fun SitePartnerCreateVerboseV7Dto.sortContent() =
        copy(site = site.sortContent(), mainAddress = mainAddress.sortContent())

    private fun AddressPartnerCreateV7ResponseWrapper.sortContent() =
        copy(entities = entities.map { it.sortContent() })

    private fun AddressPartnerUpdateV7ResponseWrapper.sortContent() =
        copy(entities = entities.map { it.sortContent() })

    private fun AddressPartnerCreateVerboseV7Dto.sortContent() =
        copy(address = address.sortContent())

    private fun AddressPartnerUpdateVerboseV7Dto.sortContent() =
        copy(address = address.sortContent())

    private fun LegalEntityWithLegalAddressVerboseV7Dto.sortContent() =
        copy(header = header.sortContent(), legalAddress = legalAddress.sortContent(), scriptVariants = scriptVariants.sortedBy { it.scriptCode })

    private fun LegalEntityHeaderVerboseV7Dto.sortContent() =
        copy(states = states.sortedBy { it.validFrom }, identifiers = identifiers.sortedBy { it.value })

    private fun SiteWithMainAddressVerboseV7Dto.sortContent() =
        copy(site = site.sortContent(), mainAddress = mainAddress.sortContent())

    private fun SiteVerboseV7Dto.sortContent() =
        copy(states = states.sortedBy { it.validFrom }, scriptVariants = scriptVariants.sortedBy { it.scriptCode })

    private fun LogisticAddressVerboseV7Dto.sortContent() =
        copy(address = address.sortContent(), scriptVariants = scriptVariants.sortedBy { it.scriptCode })

    private fun LogisticAddressInvariantVerboseV7Dto.sortContent() =
        copy(states = states.sortedBy { it.validFrom }, identifiers = identifiers.sortedBy { it.value })

    private fun PageDto<LogisticAddressInvariantVerboseV7Dto>.sortLogisticAddressInvariant() =
        copy(content = content.sortedBy { it.bpna }.map { it.sortContent() })

    private fun PageDto<LogisticAddressVerboseV7Dto>.sortLogisticAddress() =
        copy(content = content.sortedBy { it.address.bpna }.map { it.sortContent() })

    private fun PageDto<SiteVerboseV7Dto>.sortSite() =
        copy(content = content.sortedBy { it.bpns }.map { it.sortContent() })

    private fun PageDto<SiteWithMainAddressVerboseV7Dto>.sortSiteWithAddress() =
        copy(content = content.sortedBy { it.site.bpns }.map { it.sortContent() })

    private fun PageDto<LegalEntityWithLegalAddressVerboseV7Dto>.sortLegalEntityWithAddress() =
        copy(content = content.sortedBy { it.header.bpnl }.map { it.sortContent() })

}