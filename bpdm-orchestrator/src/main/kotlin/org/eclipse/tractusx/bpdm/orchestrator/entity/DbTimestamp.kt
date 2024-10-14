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

package org.eclipse.tractusx.bpdm.orchestrator.entity

import java.io.Serializable
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * This helper type makes sure that all timestamps on the entities are actually truncated to microseconds (same as in the database)
 *
 * This makes sure that timestamps in entities and database are always equal even before the entity is persisted in the database
 */
class DbTimestamp(instant: Instant): Serializable{


    private val truncatedInstant = instant.truncatedTo(ChronoUnit.MICROS)

    val instant get(): Instant = truncatedInstant

    companion object{
        fun now(): DbTimestamp = Instant.now().toTimestamp()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DbTimestamp

        return truncatedInstant == other.truncatedInstant
    }

    override fun hashCode(): Int {
        return truncatedInstant?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "DbTimestamp(truncatedInstant=$truncatedInstant)"
    }
}

fun Instant.toTimestamp(): DbTimestamp = DbTimestamp(this)