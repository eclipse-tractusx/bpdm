/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.common.model

import java.io.Serializable
import java.time.Instant
import java.time.temporal.ChronoUnit

/** An instant with the same microsecond precision as the database. */
class DbTimestamp(instant: Instant) : Serializable, Comparable<DbTimestamp> {
    val instant: Instant = instant.truncatedTo(ChronoUnit.MICROS)

    override fun compareTo(other: DbTimestamp): Int = instant.compareTo(other.instant)

    override fun equals(other: Any?): Boolean = other is DbTimestamp && instant == other.instant

    override fun hashCode(): Int = instant.hashCode()

    override fun toString(): String = "DbTimestamp(instant=$instant)"

    companion object {
        fun now(): DbTimestamp = DbTimestamp(Instant.now())
    }
}

fun Instant.toTimestamp(): DbTimestamp = DbTimestamp(this)
