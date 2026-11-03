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
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressInvariantVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityHeaderVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto
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
        actual: Collection<LegalEntityWithLegalAddressVerboseV7Dto>,
        expected: Collection<LegalEntityWithLegalAddressVerboseV7Dto>,
        creationTimeframe: Timeframe,
        updateTimeframe: Timeframe = creationTimeframe
    ) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
            .ignoringFields(
                "${LegalEntityWithLegalAddressVerboseV7Dto::header.name}.${LegalEntityHeaderVerboseV7Dto::createdAt.name}",
                "${LegalEntityWithLegalAddressVerboseV7Dto::header.name}.${LegalEntityHeaderVerboseV7Dto::updatedAt.name}",
                "${LegalEntityWithLegalAddressVerboseV7Dto::header.name}.${LegalEntityHeaderVerboseV7Dto::bpnl.name}",
                "${LegalEntityWithLegalAddressVerboseV7Dto::header.name}.${LegalEntityHeaderVerboseV7Dto::currentness.name}",
                "${LegalEntityWithLegalAddressVerboseV7Dto::legalAddress.name}.${LogisticAddressInvariantVerboseV7Dto::bpna.name}",
                "${LegalEntityWithLegalAddressVerboseV7Dto::legalAddress.name}.${LogisticAddressInvariantVerboseV7Dto::bpnLegalEntity.name}",
                "${LegalEntityWithLegalAddressVerboseV7Dto::legalAddress.name}.${LogisticAddressInvariantVerboseV7Dto::createdAt.name}",
                "${LegalEntityWithLegalAddressVerboseV7Dto::legalAddress.name}.${LogisticAddressInvariantVerboseV7Dto::updatedAt.name}"
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
        actual: Collection<SiteWithMainAddressVerboseV7Dto>,
        expected: Collection<SiteWithMainAddressVerboseV7Dto>,
        creationTimeframe: Timeframe,
        updateTimeframe: Timeframe = creationTimeframe
    ) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
            .ignoringFields(
                "${SiteWithMainAddressVerboseV7Dto::site.name}.${SiteVerboseV7Dto::createdAt.name}",
                "${SiteWithMainAddressVerboseV7Dto::site.name}.${SiteVerboseV7Dto::updatedAt.name}",
                "${SiteWithMainAddressVerboseV7Dto::site.name}.${SiteVerboseV7Dto::bpns.name}",
                "${SiteWithMainAddressVerboseV7Dto::mainAddress.name}.${LogisticAddressInvariantVerboseV7Dto::bpna.name}",
                "${SiteWithMainAddressVerboseV7Dto::mainAddress.name}.${LogisticAddressInvariantVerboseV7Dto::bpnSite.name}",
                "${SiteWithMainAddressVerboseV7Dto::mainAddress.name}.${LogisticAddressInvariantVerboseV7Dto::createdAt.name}",
                "${SiteWithMainAddressVerboseV7Dto::mainAddress.name}.${LogisticAddressInvariantVerboseV7Dto::updatedAt.name}",
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
        actual: Collection<LogisticAddressInvariantVerboseV7Dto>,
        expected: Collection<LogisticAddressInvariantVerboseV7Dto>,
        creationTimeframe: Timeframe,
        updateTimeframe: Timeframe = creationTimeframe
    ) {

        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
            .ignoringFields(
                LogisticAddressInvariantVerboseV7Dto::createdAt.name,
                LogisticAddressInvariantVerboseV7Dto::updatedAt.name,
                LogisticAddressInvariantVerboseV7Dto::bpna.name
            )
            .withComparatorForType(instantSecondsComparator, Instant::class.java)
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .withComparatorForType(stringIgnoreComparator, String::class.java)
            .isEqualTo(expected)



        actual.forEach { Assertions.assertThat(it.createdAt).isBetween(creationTimeframe.startTime, creationTimeframe.endTime) }
        actual.forEach { Assertions.assertThat(it.updatedAt).isBetween(updateTimeframe.startTime, updateTimeframe.endTime) }
    }
}

