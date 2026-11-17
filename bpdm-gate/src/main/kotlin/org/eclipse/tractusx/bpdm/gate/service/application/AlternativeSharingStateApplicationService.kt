/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.service.application

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.gate.model.output.businesspartner.AlternativeProcessResult
import org.eclipse.tractusx.bpdm.gate.service.operation.businesspartner.AlternativeProcessOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.businesspartner.AlternativeProcessParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Owns the atomic parse-and-transition boundary of provider sharing-state requests. */
@Service
class AlternativeSharingStateApplicationService(
    private val parser: AlternativeProcessParser,
    private val operation: AlternativeProcessOperationService
) {
    /** Marks every requested record pending, or rejects the batch without changing any record. */
    @Transactional
    fun pending(externalIds: Collection<String>, process: String, tenantBpnl: String?): AlternativeProcessResult<Unit> =
        when (val parsed = parser.parseStates(externalIds.map { it to process }, tenantBpnl)) {
            is ParseResult.Failure -> AlternativeProcessResult.Rejected(parsed.errors)
            is ParseResult.Success -> AlternativeProcessResult.Applied(operation.pending(parsed.parsed))
        }

    /** Fails every requested record with [message], or rejects the batch without changing any record. */
    @Transactional
    fun error(externalIds: Collection<String>, process: String, message: String, tenantBpnl: String?): AlternativeProcessResult<Unit> =
        when (val parsed = parser.parseStates(externalIds.map { it to process }, tenantBpnl)) {
            is ParseResult.Failure -> AlternativeProcessResult.Rejected(parsed.errors)
            is ParseResult.Success -> AlternativeProcessResult.Applied(operation.error(parsed.parsed, message))
        }
}
