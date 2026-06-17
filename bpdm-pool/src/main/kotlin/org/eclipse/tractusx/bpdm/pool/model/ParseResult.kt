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

package org.eclipse.tractusx.bpdm.pool.model

/**
 * Per-entry outcome of parsing a single request into [T] or a list of errors [E].
 *
 * A `parse` returning `List<ParseResult<T, E>>` guarantees the result is order-preserving and positional: the result has
 * the same size as the input and the i-th result is the verdict for the i-th request. Failures are per entry, not a
 * batch-wide partition, so one bad request never discards the verdicts of its neighbours.
 */
sealed interface ParseResult<out T, out E> {
    data class Success<out T>(val parsed: T) : ParseResult<T, Nothing>
    data class Failure<out E>(val errors: List<E>) : ParseResult<Nothing, E>
}

/**
 * Folds [extraErrors] (errors produced outside this result, e.g. a service's parent resolution or duplicate checks) into
 * this result and, if nothing failed, maps the parsed value with [transform]. Lets a service assemble its operation-specific
 * result from the shared content parse result plus its own per-entry errors. Thanks to `ParseResult`'s covariance in the
 * error type, a result with a narrower error type can be combined with a wider operation error type.
 */
fun <T, R, E> ParseResult<T, E>.combine(extraErrors: List<E>, transform: (T) -> R): ParseResult<R, E> =
    when (this) {
        is ParseResult.Success -> if (extraErrors.isEmpty()) ParseResult.Success(transform(parsed)) else ParseResult.Failure(extraErrors)
        is ParseResult.Failure -> ParseResult.Failure(errors + extraErrors)
    }
