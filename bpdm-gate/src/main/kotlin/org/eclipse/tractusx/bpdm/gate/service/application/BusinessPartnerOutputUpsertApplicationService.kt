/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.service.application

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.gate.mapper.DbToOutputMapper
import org.eclipse.tractusx.bpdm.gate.model.output.businesspartner.AlternativeProcessResult
import org.eclipse.tractusx.bpdm.gate.model.output.businesspartner.BusinessPartnerOutputRecord
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.AlternativeOutputRaw
import org.eclipse.tractusx.bpdm.gate.service.operation.businesspartner.AlternativeProcessOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.businesspartner.AlternativeProcessParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Validates and writes a provider's output batch atomically, then shapes its API-independent result. */
@Service
class BusinessPartnerOutputUpsertApplicationService(
    private val parser: AlternativeProcessParser,
    private val operation: AlternativeProcessOperationService,
    private val mapper: DbToOutputMapper
) {
    /** Writes only if every entry belongs to its requested process in [tenantBpnl]. */
    @Transactional
    fun upsert(requests: List<AlternativeOutputRaw>, tenantBpnl: String?): AlternativeProcessResult<List<BusinessPartnerOutputRecord>> =
        when (val parsed = parser.parseOutputs(requests, tenantBpnl)) {
            is ParseResult.Failure -> AlternativeProcessResult.Rejected(parsed.errors)
            is ParseResult.Success -> AlternativeProcessResult.Applied(operation.write(parsed.parsed).map(mapper::toRecord))
        }
}
