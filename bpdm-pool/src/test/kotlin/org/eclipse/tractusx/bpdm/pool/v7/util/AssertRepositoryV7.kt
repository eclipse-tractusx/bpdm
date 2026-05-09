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
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityHeaderVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressInvariantVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.test.util.InstantSecondsComparator
import org.eclipse.tractusx.bpdm.test.util.LocalDatetimeSecondsComparator
import java.time.Instant
import java.time.LocalDateTime

class AssertRepositoryV7(
    instantSecondsComparator: InstantSecondsComparator,
    localDatetimeSecondsComparator: LocalDatetimeSecondsComparator
) {

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

    fun assertLegalEntityCreateResponseWrapperIsEqual(actual: LegalEntityPartnerCreateResponseWrapper, expected: LegalEntityPartnerCreateResponseWrapper){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForCreated)
            .isEqualTo(expected.sortContent())
    }

    fun assertLegalEntityUpdateResponseWrapperIsEqual(actual: LegalEntityPartnerUpdateResponseWrapper, expected: LegalEntityPartnerUpdateResponseWrapper){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForExisting)
            .isEqualTo(expected.sortContent())
    }

    fun assertLegalEntitySearchResponse(actual: PageDto<LegalEntityWithLegalAddressVerboseDto>, expected: PageDto<LegalEntityWithLegalAddressVerboseDto>){
        Assertions.assertThat(actual.sortLegalEntityWithAddress())
            .usingRecursiveComparison(comparisonConfigurationForExisting)
            .isEqualTo(expected.sortLegalEntityWithAddress())
    }

    fun assertLegalEntityWithLegalAddressIsEqual(actual: LegalEntityWithLegalAddressVerboseDto, expected: LegalEntityWithLegalAddressVerboseDto){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForExisting)
            .isEqualTo(expected.sortContent())
    }

    fun assertSiteCreateResponseWrapperIsEqual(actual: SitePartnerCreateResponseWrapper, expected: SitePartnerCreateResponseWrapper){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForCreated)
            .isEqualTo(expected.sortContent())
    }

    fun assertSiteSearchResponse(actual: PageDto<SiteWithMainAddressVerboseDto>, expected: PageDto<SiteWithMainAddressVerboseDto>){
        Assertions.assertThat(actual.sortSiteWithAddress())
            .usingRecursiveComparison(comparisonConfigurationForExisting)
            .isEqualTo(expected.sortSiteWithAddress())
    }

    fun assertSiteUpdateResponseWrapperIsEqual(actual: SitePartnerUpdateResponseWrapper, expected: SitePartnerUpdateResponseWrapper){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForExisting)
            .isEqualTo(expected.sortContent())
    }

    fun assertSiteGetIsEqual(actual: SiteWithMainAddressVerboseDto, expected: SiteWithMainAddressVerboseDto){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForExisting)
            .isEqualTo(expected.sortContent())
    }

    fun assertLegalEntitySitesResponse(actual: PageDto<SiteVerboseDto>, expected: PageDto<SiteVerboseDto>){
        Assertions.assertThat(actual.sortSite())
            .usingRecursiveComparison(comparisonConfigurationForExisting)
            .isEqualTo(expected.sortSite())
    }

    fun assertAddressCreateResponseWrapperIsEqual(actual: AddressPartnerCreateResponseWrapper, expected: AddressPartnerCreateResponseWrapper){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForCreated)
            .isEqualTo(expected.sortContent())
    }

    fun assertAddressUpdateResponseWrapperIsEqual(actual: AddressPartnerUpdateResponseWrapper, expected: AddressPartnerUpdateResponseWrapper){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForExisting)
            .isEqualTo(expected.sortContent())
    }

    fun assertAddressSearch(actual: PageDto<LogisticAddressVerboseDto>, expected: PageDto<LogisticAddressVerboseDto>){
        Assertions.assertThat(actual.sortLogisticAddress())
            .usingRecursiveComparison(comparisonConfigurationForExisting)
            .isEqualTo(expected.sortLogisticAddress())
    }

    fun assertAddressGet(actual: LogisticAddressVerboseDto, expected: LogisticAddressVerboseDto){
        Assertions.assertThat(actual.sortContent())
            .usingRecursiveComparison(comparisonConfigurationForExisting)
            .isEqualTo(expected.sortContent())
    }

    fun assertParticipantAddressSearch(actual: PageDto<LogisticAddressInvariantVerboseDto>, expected: PageDto<LogisticAddressInvariantVerboseDto>){
        Assertions.assertThat(actual.sortLogisticAddressInvariant())
            .usingRecursiveComparison(comparisonConfigurationForExisting)
            .isEqualTo(expected.sortLogisticAddressInvariant())
    }

    private fun LegalEntityPartnerCreateResponseWrapper.sortContent() =
        copy(entities = entities.map { it.sortContent() })

    private fun LegalEntityPartnerUpdateResponseWrapper.sortContent() =
        copy(entities = entities.map { it.sortContent() })

    private fun LegalEntityPartnerCreateVerboseDto.sortContent() =
        copy(legalEntity = legalEntity.sortContent())

    private fun SitePartnerCreateResponseWrapper.sortContent() =
        copy(entities = entities.map { it.sortContent() })

    private fun SitePartnerUpdateResponseWrapper.sortContent() =
        copy(entities = entities.map { it.sortContent() })

    private fun SitePartnerCreateVerboseDto.sortContent() =
        copy(site = site.sortContent(), mainAddress = mainAddress.sortContent())

    private fun AddressPartnerCreateResponseWrapper.sortContent() =
        copy(entities = entities.map { it.sortContent() })

    private fun AddressPartnerUpdateResponseWrapper.sortContent() =
        copy(entities = entities.map { it.sortContent() })

    private fun AddressPartnerCreateVerboseDto.sortContent() =
        copy(address = address.sortContent())

    private fun AddressPartnerUpdateVerboseDto.sortContent() =
        copy(address = address.sortContent())

    private fun LegalEntityWithLegalAddressVerboseDto.sortContent() =
        copy(header = header.sortContent(), legalAddress = legalAddress.sortContent(), scriptVariants = scriptVariants.sortedBy { it.scriptCode })

    private fun LegalEntityHeaderVerboseDto.sortContent() =
        copy(states = states.sortedBy { it.validFrom }, identifiers = identifiers.sortedBy { it.value })

    private fun SiteWithMainAddressVerboseDto.sortContent() =
        copy(site = site.sortContent(), mainAddress = mainAddress.sortContent())

    private fun SiteVerboseDto.sortContent() =
        copy(states = states.sortedBy { it.validFrom }, scriptVariants = scriptVariants.sortedBy { it.scriptCode })

    private fun LogisticAddressVerboseDto.sortContent() =
        copy(address = address.sortContent(), scriptVariants = scriptVariants.sortedBy { it.scriptCode })

    private fun LogisticAddressInvariantVerboseDto.sortContent() =
        copy(states = states.sortedBy { it.validFrom }, identifiers = identifiers.sortedBy { it.value })

    private fun PageDto<LogisticAddressInvariantVerboseDto>.sortLogisticAddressInvariant() =
        copy(content = content.sortedBy { it.bpna }.map { it.sortContent() })

    private fun PageDto<LogisticAddressVerboseDto>.sortLogisticAddress() =
        copy(content = content.sortedBy { it.address.bpna }.map { it.sortContent() })

    private fun PageDto<SiteVerboseDto>.sortSite() =
        copy(content = content.sortedBy { it.bpns }.map { it.sortContent() })

    private fun PageDto<SiteWithMainAddressVerboseDto>.sortSiteWithAddress() =
        copy(content = content.sortedBy { it.site.bpns }.map { it.sortContent() })

    private fun PageDto<LegalEntityWithLegalAddressVerboseDto>.sortLegalEntityWithAddress() =
        copy(content = content.sortedBy { it.header.bpnl }.map { it.sortContent() })

}