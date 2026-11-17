/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.model.output.businesspartner

/** All-or-nothing outcome of an alternative-process request. */
sealed interface AlternativeProcessResult<out T> {
    /** Every entry was validated and applied. */
    data class Applied<T>(val value: T) : AlternativeProcessResult<T>

    /** No entries were changed because the request violates these rules. */
    data class Rejected(val messages: List<String>) : AlternativeProcessResult<Nothing>
}
