/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.gate.service.businesspartner.input

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.util.countForLog
import org.eclipse.tractusx.bpdm.common.util.joinIdentifiersForLog
import org.eclipse.tractusx.bpdm.gate.mapper.record.BusinessPartnerInputRecordMapper
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertEntryResult
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertError
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertIn
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertResult
import org.eclipse.tractusx.bpdm.gate.service.businesspartner.input.operation.BusinessPartnerInputUpsertOperationService
import org.eclipse.tractusx.bpdm.gate.service.businesspartner.input.operation.toInputLogIdentifier
import org.eclipse.tractusx.bpdm.gate.service.businesspartner.input.parser.BusinessPartnerInputUpsertParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionAspectSupport

/**
 * Upserts a batch of business partner inputs for every API version, all or nothing.
 *
 * Only rules over the batch's shape are checked up front; each entry is parsed and executed before the next one is
 * looked at, so it is judged against the state its predecessors left behind. A rejected batch is rolled back as a whole.
 */
@Service
class BusinessPartnerInputUpsertApplicationService(
    private val parser: BusinessPartnerInputUpsertParser,
    private val operationService: BusinessPartnerInputUpsertOperationService,
    private val recordMapper: BusinessPartnerInputRecordMapper
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Upserts [requests] for [tenantBpnl] and returns the inputs that were written, or writes nothing and returns the
     * violations of every entry if any entry is invalid.
     */
    @Transactional
    fun upsert(requests: List<BusinessPartnerInputUpsertIn>, tenantBpnl: String?): BusinessPartnerInputUpsertResult {
        val batchErrors = parser.validateBatch(requests)
        if (batchErrors.isNotEmpty()) return BusinessPartnerInputUpsertResult.Rejected(batchErrors)

        val errors = mutableListOf<BusinessPartnerInputUpsertError>()
        val applied = mutableListOf<BusinessPartnerInputUpsertEntryResult.Applied>()
        requests.forEach { request ->
            when (val result = parser.parse(request)) {
                is ParseResult.Failure -> errors += result.errors
                is ParseResult.Success -> operationService.upsert(result.parsed, tenantBpnl)
                    .let { if (it is BusinessPartnerInputUpsertEntryResult.Applied) applied += it }
            }
        }

        if (errors.isNotEmpty()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
            return BusinessPartnerInputUpsertResult.Rejected(errors)
        }
        reportWrites(applied)
        return BusinessPartnerInputUpsertResult.Written(applied.map { recordMapper.toRecord(it.input) })
    }

    private fun reportWrites(applied: List<BusinessPartnerInputUpsertEntryResult.Applied>) {
        report("Created", applied.filterIsInstance<BusinessPartnerInputUpsertEntryResult.Created>())
        report("Updated", applied.filterIsInstance<BusinessPartnerInputUpsertEntryResult.Updated>())
    }

    private fun report(verb: String, written: List<BusinessPartnerInputUpsertEntryResult.Applied>) {
        if (written.isEmpty()) return
        logger.info {
            "$verb ${countForLog(written.size, "business partner input", "business partner inputs")}: " +
                    written.map { it.input.toInputLogIdentifier() }.joinIdentifiersForLog()
        }
    }
}
