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

package org.eclipse.tractusx.bpdm.pool.util

import org.eclipse.tractusx.bpdm.common.model.ParseResult

/**
 * The one parse result of a single-entry delegation, with its errors recorded against the entry being parsed.
 *
 * The parsers a partner delegates to are batch-shaped, and an upsert parser hands them one entry at a time.
 */
internal fun <T, E, RECORDED> List<ParseResult<T, E>>.singleOrRecord(
    errors: MutableList<RECORDED>,
    toError: (E) -> RECORDED
): T? =
    when (val result = single()) {
        is ParseResult.Success -> result.parsed
        is ParseResult.Failure -> { errors += result.errors.map(toError); null }
    }

/** The parsed value, or null with its errors recorded against the entry being parsed. */
internal fun <T, E> ParseResult<T, E>.parsedOrRecord(errors: MutableList<in E>): T? =
    when (this) {
        is ParseResult.Success -> parsed
        is ParseResult.Failure -> { errors.addAll(this.errors); null }
    }

/** The parsed value, or every reason it could not be made. */
internal fun <T, E> T?.orFailure(errors: List<E>): ParseResult<T, E> =
    if (this == null || errors.isNotEmpty()) ParseResult.Failure(errors) else ParseResult.Success(this)

/** The parsed value, or null where this entry was rejected. */
internal fun <T> ParseResult<T, *>.parsedOrNull(): T? =
    if (this is ParseResult.Success) parsed else null

/** Every error the rejected entries among these results carry. */
internal fun <E> List<ParseResult<*, E>>.failureErrors(): List<E> =
    flatMap { if (it is ParseResult.Failure) it.errors else emptyList() }
