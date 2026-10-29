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

package org.eclipse.tractusx.bpdm.common.dto

import java.time.LocalDateTime

/**
 * Translates business state dates between the API, where every state carries both dates, and the stored form, where a
 * missing date stands for a state without start or end.
 *
 * Reading the earliest and latest date back as missing is the decoding of this API encoding, so it happens in the API
 * mappers rather than in a parser: the golden record task carries missing dates as they are.
 */
object BusinessStateDates {

    const val EARLIEST_TEXT = "1900-01-01T00:00:00"
    const val LATEST_TEXT = "9999-12-31T23:59:59"

    val EARLIEST: LocalDateTime = LocalDateTime.parse(EARLIEST_TEXT)
    val LATEST: LocalDateTime = LocalDateTime.parse(LATEST_TEXT)

    /**
     * Returns the start date the API reports for [validFrom], the earliest date when it is missing.
     */
    fun reportValidFrom(validFrom: LocalDateTime?): LocalDateTime =
        (validFrom ?: EARLIEST).coerceIn(EARLIEST, LATEST)

    /**
     * Returns the end date the API reports for [validTo], the latest date when it is missing.
     */
    fun reportValidTo(validTo: LocalDateTime?): LocalDateTime =
        (validTo ?: LATEST).coerceIn(EARLIEST, LATEST)

    /**
     * Returns the stored form of a received start date, missing for the earliest date or any earlier one and capped at
     * the latest date.
     */
    fun receiveValidFrom(validFrom: LocalDateTime?): LocalDateTime? =
        validFrom?.takeIf { it > EARLIEST }?.coerceAtMost(LATEST)

    /**
     * Returns the stored form of a received end date, missing for the latest date or any later one and raised to the
     * earliest date.
     */
    fun receiveValidTo(validTo: LocalDateTime?): LocalDateTime? =
        validTo?.takeIf { it < LATEST }?.coerceAtLeast(EARLIEST)
}
