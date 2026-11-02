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

import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.usertype.UserType
import java.io.Serializable
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.time.ZoneOffset
import java.util.Calendar
import java.util.TimeZone

class DbTimestampConverter : UserType<DbTimestamp> {
    override fun equals(x: DbTimestamp?, y: DbTimestamp?): Boolean = x == y

    override fun hashCode(x: DbTimestamp?): Int = x?.hashCode() ?: 0

    override fun getSqlType(): Int = Types.TIMESTAMP

    override fun returnedClass(): Class<DbTimestamp> = DbTimestamp::class.java

    override fun nullSafeGet(rs: ResultSet, position: Int, options: WrapperOptions?): DbTimestamp? =
        rs.getTimestamp(position, utcCalendar())?.toInstant()?.toTimestamp()

    override fun nullSafeSet(st: PreparedStatement, value: DbTimestamp?, position: Int, options: WrapperOptions?) {
        if (value == null) st.setNull(position, Types.TIMESTAMP)
        else st.setTimestamp(position, Timestamp.from(value.instant), utcCalendar())
    }

    private fun utcCalendar(): Calendar = Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC))

    override fun isMutable(): Boolean = false

    override fun assemble(cached: Serializable?, owner: Any?): DbTimestamp? = cached as DbTimestamp?

    override fun disassemble(value: DbTimestamp?): Serializable? = value

    override fun deepCopy(value: DbTimestamp?): DbTimestamp? = value
}
