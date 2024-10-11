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

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import java.io.Serializable
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types

class DbTimestampConverter: UserType<DbTimestamp> {

    override fun equals(p0: DbTimestamp?, p1: DbTimestamp?): Boolean {
        return p0?.instant == p1?.instant
    }

    override fun hashCode(p0: DbTimestamp?): Int {
        return p0?.instant.hashCode()
    }

    override fun getSqlType(): Int {
        return Types.TIMESTAMP
    }

    override fun returnedClass(): Class<DbTimestamp> {
        return DbTimestamp::class.java
    }

    override fun nullSafeGet(resultSet: ResultSet, position: Int, p2: SharedSessionContractImplementor?, p3: Any?): DbTimestamp? {
        return resultSet.getTimestamp(position)?.let { DbTimestamp(it.toInstant()) }
    }

    override fun isMutable(): Boolean {
        return false
    }

    override fun assemble(p0: Serializable?, p1: Any?): DbTimestamp? {
        return p0?.let { p0 as DbTimestamp  }
    }

    override fun disassemble(p0: DbTimestamp?): Serializable? {
        return p0
    }

    override fun deepCopy(p0: DbTimestamp?): DbTimestamp? {
       return p0?.let { DbTimestamp(it.instant) }
    }

    override fun nullSafeSet(preparedStatement: PreparedStatement, timeStamp: DbTimestamp?, index: Int, p3: SharedSessionContractImplementor?) {
        if(timeStamp == null) preparedStatement.setNull(index, Types.TIMESTAMP)
        else{
            preparedStatement.setTimestamp(index, Timestamp.from(timeStamp.instant))
        }
    }
}