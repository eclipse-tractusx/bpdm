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

package org.eclipse.tractusx.bpdm.pool.entity

import java.time.LocalDate

/**
 * A relation validity period with its open end resolved to a concrete date, so that two periods can be compared.
 * Every relation family compares validities through this type, so they all agree on what an overlap is.
 */
data class RelationTimePeriod(
    val validFrom: LocalDate,
    val validTo: LocalDate
) {
    companion object {
        private val openEnd = LocalDate.parse("9999-01-01")

        /**
         * Returns the period a missing [validTo] leaves open as one running until a date no relation reaches.
         */
        fun fromUnlimited(validFrom: LocalDate, validTo: LocalDate?): RelationTimePeriod =
            RelationTimePeriod(validFrom, validTo ?: openEnd)
    }

    /**
     * Whether this period and [other] intersect, where periods meeting on a single shared date do not.
     */
    fun hasOverlap(other: RelationTimePeriod): Boolean =
        validFrom < other.validTo && validTo > other.validFrom
}
