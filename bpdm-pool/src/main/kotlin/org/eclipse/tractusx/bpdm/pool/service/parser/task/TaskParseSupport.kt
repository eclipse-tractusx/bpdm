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


package org.eclipse.tractusx.bpdm.pool.service.parser.task

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.GoldenRecordUpsertParseError

/**
 * The one parse result of a single-entry delegation, with its errors recorded against the entry being parsed.
 *
 * The parsers a partner delegates to are batch-shaped, and an upsert parser hands them one entry at a time.
 */
internal fun <T, E> List<ParseResult<T, E>>.singleOrRecord(
    errors: MutableList<GoldenRecordUpsertParseError>,
    toError: (E) -> GoldenRecordUpsertParseError
): T? =
    when (val result = single()) {
        is ParseResult.Success -> result.parsed
        is ParseResult.Failure -> { errors += result.errors.map(toError); null }
    }

/** The plan, or every reason it could not be made. */
internal fun <T> T?.orFailure(errors: List<GoldenRecordUpsertParseError>): ParseResult<T, GoldenRecordUpsertParseError> =
    if (this == null || errors.isNotEmpty()) ParseResult.Failure(errors) else ParseResult.Success(this)
