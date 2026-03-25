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

package org.eclipse.tractusx.bpdm.test.util

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityHeaderVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressInvariantVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import java.time.Instant
import java.time.LocalDateTime

/**
 * Provides specialized functionality to assert business partner response objects
 */
class PoolAssertHelper(
    private val instantSecondsComparator: InstantSecondsComparator,
    private val localDatetimeSecondsComparator: LocalDatetimeSecondsComparator,
    private val stringIgnoreComparator: StringIgnoreComparator
) {

    fun assertLegalEntityResponse(
        actual: Collection<LegalEntityWithLegalAddressVerboseDto>,
        expected: Collection<LegalEntityWithLegalAddressVerboseDto>,
        creationTimeframe: Timeframe,
        updateTimeframe: Timeframe = creationTimeframe
    ) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
            .ignoringFields(
                "${LegalEntityWithLegalAddressVerboseDto::header.name}.${LegalEntityHeaderVerboseDto::createdAt.name}",
                "${LegalEntityWithLegalAddressVerboseDto::header.name}.${LegalEntityHeaderVerboseDto::updatedAt.name}",
                "${LegalEntityWithLegalAddressVerboseDto::header.name}.${LegalEntityHeaderVerboseDto::bpnl.name}",
                "${LegalEntityWithLegalAddressVerboseDto::header.name}.${LegalEntityHeaderVerboseDto::currentness.name}",
                "${LegalEntityWithLegalAddressVerboseDto::legalAddress.name}.${LogisticAddressInvariantVerboseDto::bpna.name}",
                "${LegalEntityWithLegalAddressVerboseDto::legalAddress.name}.${LogisticAddressInvariantVerboseDto::bpnLegalEntity.name}",
                "${LegalEntityWithLegalAddressVerboseDto::legalAddress.name}.${LogisticAddressInvariantVerboseDto::createdAt.name}",
                "${LegalEntityWithLegalAddressVerboseDto::legalAddress.name}.${LogisticAddressInvariantVerboseDto::updatedAt.name}"
            )
            .withComparatorForType(instantSecondsComparator, Instant::class.java)
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)

        actual.forEach { Assertions.assertThat(it.header.bpnl).isEqualTo(it.legalAddress.bpnLegalEntity) }
        actual.forEach { Assertions.assertThat(it.header.currentness).isBetween(creationTimeframe.startTime, creationTimeframe.endTime) }
        actual.forEach { Assertions.assertThat(it.header.createdAt).isBetween(creationTimeframe.startTime, creationTimeframe.endTime) }
        actual.forEach { Assertions.assertThat(it.header.updatedAt).isBetween(updateTimeframe.startTime, updateTimeframe.endTime) }
        actual.forEach { Assertions.assertThat(it.legalAddress.createdAt).isBetween(creationTimeframe.startTime, creationTimeframe.endTime) }
        actual.forEach { Assertions.assertThat(it.legalAddress.updatedAt).isBetween(updateTimeframe.startTime, updateTimeframe.endTime) }
    }

    fun assertSiteResponse(
        actual: Collection<SiteWithMainAddressVerboseDto>,
        expected: Collection<SiteWithMainAddressVerboseDto>,
        creationTimeframe: Timeframe,
        updateTimeframe: Timeframe = creationTimeframe
    ) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
            .ignoringFields(
                "${SiteWithMainAddressVerboseDto::site.name}.${SiteVerboseDto::createdAt.name}",
                "${SiteWithMainAddressVerboseDto::site.name}.${SiteVerboseDto::updatedAt.name}",
                "${SiteWithMainAddressVerboseDto::site.name}.${SiteVerboseDto::bpns.name}",
                "${SiteWithMainAddressVerboseDto::mainAddress.name}.${LogisticAddressInvariantVerboseDto::bpna.name}",
                "${SiteWithMainAddressVerboseDto::mainAddress.name}.${LogisticAddressInvariantVerboseDto::bpnSite.name}",
                "${SiteWithMainAddressVerboseDto::mainAddress.name}.${LogisticAddressInvariantVerboseDto::createdAt.name}",
                "${SiteWithMainAddressVerboseDto::mainAddress.name}.${LogisticAddressInvariantVerboseDto::updatedAt.name}",
            )
            .withComparatorForType(instantSecondsComparator, Instant::class.java)
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)

        actual.forEach { Assertions.assertThat(it.site.bpns).isEqualTo(it.mainAddress.bpnSite) }
        actual.forEach { Assertions.assertThat(it.site.createdAt).isBetween(creationTimeframe.startTime, creationTimeframe.endTime) }
        actual.forEach { Assertions.assertThat(it.site.updatedAt).isBetween(updateTimeframe.startTime, updateTimeframe.endTime) }
        actual.forEach { Assertions.assertThat(it.mainAddress.createdAt).isBetween(creationTimeframe.startTime, creationTimeframe.endTime) }
        actual.forEach { Assertions.assertThat(it.mainAddress.updatedAt).isBetween(updateTimeframe.startTime, updateTimeframe.endTime) }
    }

    fun assertAddressResponse(
        actual: Collection<LogisticAddressInvariantVerboseDto>,
        expected: Collection<LogisticAddressInvariantVerboseDto>,
        creationTimeframe: Timeframe,
        updateTimeframe: Timeframe = creationTimeframe
    ) {

        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
            .ignoringFields(
                LogisticAddressInvariantVerboseDto::createdAt.name,
                LogisticAddressInvariantVerboseDto::updatedAt.name,
                LogisticAddressInvariantVerboseDto::bpna.name
            )
            .withComparatorForType(instantSecondsComparator, Instant::class.java)
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .withComparatorForType(stringIgnoreComparator, String::class.java)
            .isEqualTo(expected)



        actual.forEach { Assertions.assertThat(it.createdAt).isBetween(creationTimeframe.startTime, creationTimeframe.endTime) }
        actual.forEach { Assertions.assertThat(it.updatedAt).isBetween(updateTimeframe.startTime, updateTimeframe.endTime) }
    }
}

