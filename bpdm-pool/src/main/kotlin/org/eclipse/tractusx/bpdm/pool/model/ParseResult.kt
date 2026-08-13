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

    companion object{
        fun <E> ofSingleFailure(error: E) : ParseResult<Nothing, E> = Failure(listOf(error))
    }
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

/**
 * Combines several independent parse results for the *same* entry into one, accumulating the errors of all that failed
 * and applying [transform] to their parsed values only when every input succeeded (applicative validation). This lets a
 * service assemble one operation result from results produced by separate, single-responsibility parsers (e.g. content,
 * legal-entity parent, site parent) without manual error collection. Each input may carry a narrower error type; thanks
 * to `ParseResult`'s covariance in the error type they unify to the wider operation error type [E].
 */
fun <A, B, R, E> zipParseResults(a: ParseResult<A, E>, b: ParseResult<B, E>, transform: (A, B) -> R): ParseResult<R, E> {
    val errors = failureErrors(a, b)
    return if (errors.isEmpty())
        ParseResult.Success(transform(a.successValue(), b.successValue()))
    else
        ParseResult.Failure(errors)
}

fun <A, B, C, R, E> zipParseResults(
    a: ParseResult<A, E>,
    b: ParseResult<B, E>,
    c: ParseResult<C, E>,
    transform: (A, B, C) -> R
): ParseResult<R, E> {
    val errors = failureErrors(a, b, c)
    return if (errors.isEmpty())
        ParseResult.Success(transform(a.successValue(), b.successValue(), c.successValue()))
    else
        ParseResult.Failure(errors)
}

/**
 * Positional list overloads of [zipParseResults]: combine the parse results of several order-preserving lists entry by
 * entry. The lists must share the same size (the per-entry positional contract of [ParseResult]).
 */
fun <A, B, R, E> zipParseResults(
    a: List<ParseResult<A, E>>,
    b: List<ParseResult<B, E>>,
    transform: (A, B) -> R
): List<ParseResult<R, E>> {
    require(a.size == b.size) { "Parse result lists must align positionally: ${a.size} vs ${b.size}" }
    return a.indices.map { index -> zipParseResults(a[index], b[index], transform) }
}

fun <A, B, C, R, E> zipParseResults(
    a: List<ParseResult<A, E>>,
    b: List<ParseResult<B, E>>,
    c: List<ParseResult<C, E>>,
    transform: (A, B, C) -> R
): List<ParseResult<R, E>> {
    require(a.size == b.size && b.size == c.size) { "Parse result lists must align positionally: ${a.size}, ${b.size}, ${c.size}" }
    return a.indices.map { index -> zipParseResults(a[index], b[index], c[index], transform) }
}

/**
 * Cross-validates two positionally-aligned result lists against a constraint relating their two values. [validate] runs
 * only where BOTH inputs succeeded — the only case where the constraint is meaningful — and the errors it returns (empty
 * = satisfied) are folded into that entry's [b] result. Where either input already failed, [b] passes through unchanged:
 * its own resolution error already surfaces, and the constraint can't be evaluated without both operands.
 *
 * This keeps a cross-field validator (e.g. "the parent site must belong to the address's legal entity") a pure function
 * of resolved domain values, unaware of resolution failures — the "run only on resolved inputs, weave verdicts back"
 * orchestration lives here, mirroring how [chainParseResults] sequences a dependent stage after a successful one.
 */
fun <A, B, E> crossValidateParseResults(
    a: List<ParseResult<A, E>>,
    b: List<ParseResult<B, E>>,
    validate: (A, B) -> List<E>
): List<ParseResult<B, E>> {
    require(a.size == b.size) { "Parse result lists must align positionally: ${a.size} vs ${b.size}" }
    return b.indices.map { index ->
        val aResult = a[index]
        val bResult = b[index]
        if (aResult is ParseResult.Success && bResult is ParseResult.Success)
            bResult.combine(validate(aResult.parsed, bResult.parsed)) { it }
        else
            bResult
    }
}

/**
 * Chains a second parse stage after a first, positionally: [second] runs only on the entries [first] parsed
 * successfully, and its per-entry verdicts are woven back into [first]'s positions. Failures of [first] pass straight
 * through unchanged. Lets a higher pipeline stage delegate to a lower stage's `parse` (which itself yields per-entry
 * `ParseResult`s) without manually re-interleaving the two. Same success-iterator weave as [parseAndExecute], but the
 * downstream step returns verdicts rather than finished values.
 */
fun <A, B, E> chainParseResults(
    first: List<ParseResult<A, E>>,
    second: (List<A>) -> List<ParseResult<B, E>>
): List<ParseResult<B, E>> {
    val secondResults = second(first.filterIsInstance<ParseResult.Success<A>>().map { it.parsed })

    val secondIterator = secondResults.iterator()
    return first.map { result ->
        when (result) {
            is ParseResult.Success -> secondIterator.next()
            is ParseResult.Failure -> result
        }
    }
}

fun <REQUEST, PARSED, CREATED, ERROR> parseAndExecute(
    requests: List<REQUEST>,
    parse: (List<REQUEST>) -> List<ParseResult<PARSED, ERROR>>,
    execute: (List<PARSED>) -> List<CREATED>
): List<ParseResult<CREATED, ERROR>> {
    val parseResults = parse(requests)
    val executionResults = execute(parseResults.filterIsInstance<ParseResult.Success<PARSED>>().map { it.parsed })

    val executionIterator = executionResults.iterator()
    return parseResults.map { result ->
        when (result) {
            is ParseResult.Success -> ParseResult.Success(executionIterator.next())
            is ParseResult.Failure -> result
        }
    }
}

/**
 * The all-or-nothing counterpart of [parseAndExecute]: executes the batch only when *every* entry parsed, and otherwise
 * throws the exception [onParseFailure] builds from the errors of all entries that failed.
 *
 * For an operation whose endpoint answers with a single result and therefore has no per-entry error channel to report a
 * rejection through, where letting the sound entries through would half-apply the batch. Errors are still accumulated
 * across the whole batch, so one call reports every bad entry.
 */
fun <REQUEST, PARSED, RESULT, ERROR> parseAndExecuteAllOrNone(
    requests: List<REQUEST>,
    parse: (List<REQUEST>) -> List<ParseResult<PARSED, ERROR>>,
    onParseFailure: (List<ERROR>) -> RuntimeException,
    execute: (List<PARSED>) -> RESULT
): RESULT {
    val parseResults = parse(requests)
    val errors = parseResults.filterIsInstance<ParseResult.Failure<ERROR>>().flatMap { it.errors }
    if (errors.isNotEmpty()) throw onParseFailure(errors)

    return execute(parseResults.filterIsInstance<ParseResult.Success<PARSED>>().map { it.parsed })
}


private fun <E> failureErrors(vararg results: ParseResult<*, E>): List<E> =
    results.filterIsInstance<ParseResult.Failure<E>>().flatMap { it.errors }

// Safe only after [failureErrors] has confirmed no input failed.
private fun <T> ParseResult<T, *>.successValue(): T = (this as ParseResult.Success<T>).parsed
