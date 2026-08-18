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

package org.eclipse.tractusx.bpdm.common.util

private const val MAX_LISTED_IDENTIFIERS = 20

/**
 * Names the given identifiers in a log message, listing at most twenty of them and counting the remainder.
 */
fun Collection<String>.joinIdentifiersForLog(): String =
    if (size <= MAX_LISTED_IDENTIFIERS)
        joinToString(", ")
    else
        take(MAX_LISTED_IDENTIFIERS).joinToString(", ", postfix = " and ${size - MAX_LISTED_IDENTIFIERS} more")

/**
 * Counts the given subjects for a log message, using the singular noun for a single subject and the plural for any other
 * number.
 */
fun countForLog(count: Int, singular: String, plural: String): String =
    "$count ${if (count == 1) singular else plural}"

/**
 * Names the given identifiers in parentheses behind a count, contributing nothing where there are none.
 */
fun Collection<String>.parenthesizeIdentifiersForLog(): String =
    if (isEmpty()) "" else " (${joinIdentifiersForLog()})"
