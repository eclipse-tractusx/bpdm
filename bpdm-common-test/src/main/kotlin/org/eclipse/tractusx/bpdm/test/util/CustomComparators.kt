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

import org.eclipse.tractusx.bpdm.test.util.StringIgnoreComparator.Companion.IGNORE_STRING
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset


/**
 * Instants given and then returned from a database query differ slightly on the nanosecond level
 * This comparator compares Instants only to the second
 */
class InstantSecondsComparator : Comparator<Instant> {
    override fun compare(actual: Instant?, expected: Instant?): Int {
        if (actual != null && expected != null) {
            return (actual.epochSecond - expected.epochSecond).toInt()
        }

        return if (actual != expected)
            -1
        else
            0
    }
}

/**
 * See [InstantSecondsComparator]. Compares local datetimes on seconds precision
 */
class LocalDatetimeSecondsComparator(private val instantComparator: InstantSecondsComparator) : Comparator<LocalDateTime> {
    override fun compare(actual: LocalDateTime?, expected: LocalDateTime?): Int {
        return instantComparator.compare(actual?.toInstant(ZoneOffset.UTC), expected?.toInstant(ZoneOffset.UTC))
    }
}

/**
 * This comparator compares Strings but returns true if one of the Strings has the value of [IGNORE_STRING]
 */
class StringIgnoreComparator() : Comparator<String> {
    companion object {
        const val IGNORE_STRING = "IGNORE"
    }

    override fun compare(o1: String?, o2: String?): Int {
        if (o1 == "IGNORE" || o2 == "IGNORE")
            return 0

        if (o1 == o2)
            return 0

        if (o1 == null || o2 == null)
            return -1

        return o1.compareTo(o2)
    }

}