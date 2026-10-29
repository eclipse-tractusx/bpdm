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

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.type.descriptor.WrapperOptions
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.Calendar

class DbTimestampTest {
    @Test
    fun `converter reads and writes timestamps with an explicit UTC calendar`() {
        val timestamp = Instant.parse("2026-01-01T00:00:00.123456Z").toTimestamp()
        var readCalendar: Calendar? = null
        var writeCalendar: Calendar? = null
        var writtenTimestamp: Timestamp? = null
        val resultSet = Proxy.newProxyInstance(
            ResultSet::class.java.classLoader, arrayOf(ResultSet::class.java)
        ) { _, method, args ->
            check(method.name == "getTimestamp")
            readCalendar = args!![1] as Calendar
            Timestamp.from(timestamp.instant)
        } as ResultSet
        val statement = Proxy.newProxyInstance(
            PreparedStatement::class.java.classLoader, arrayOf(PreparedStatement::class.java)
        ) { _, method, args ->
            check(method.name == "setTimestamp")
            writtenTimestamp = args!![1] as Timestamp
            writeCalendar = args[2] as Calendar
            null
        } as PreparedStatement

        val converter = DbTimestampConverter()
        val options: WrapperOptions? = null
        assertThat(converter.nullSafeGet(resultSet, 1, options)).isEqualTo(timestamp)
        converter.nullSafeSet(statement, timestamp, 1, options)

        assertThat(readCalendar!!.timeZone.id).isEqualTo("UTC")
        assertThat(writeCalendar!!.timeZone.id).isEqualTo("UTC")
        assertThat(writtenTimestamp!!.toInstant()).isEqualTo(timestamp.instant)
    }

    @Test
    fun `timestamps truncate to database precision and compare by value`() {
        val timestamp = Instant.parse("2026-01-01T00:00:00.123456789Z").toTimestamp()
        val sameTimestamp = Instant.parse("2026-01-01T00:00:00.123456Z").toTimestamp()

        assertThat(timestamp.instant).isEqualTo(sameTimestamp.instant)
        assertThat(timestamp).isEqualTo(sameTimestamp)
        assertThat(timestamp.hashCode()).isEqualTo(sameTimestamp.hashCode())
        assertThat(timestamp.compareTo(sameTimestamp)).isZero()
        assertThat(timestamp).isLessThan(Instant.parse("2026-01-01T00:00:00.123457Z").toTimestamp())
    }

    @Test
    fun `new entities start with identical creation and update timestamps`() {
        val entity = object : BaseEntityWithExplicitTimestamps() {}

        assertThat(entity.updatedAt).isEqualTo(entity.createdAt)
        assertThat(entity.createdAt.instant.nano % 1000).isZero()
    }
}
