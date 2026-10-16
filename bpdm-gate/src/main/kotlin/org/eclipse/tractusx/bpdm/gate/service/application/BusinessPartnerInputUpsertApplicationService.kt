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

package org.eclipse.tractusx.bpdm.gate.service.application

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.gate.mapper.record.BusinessPartnerInputRecordMapper
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertEntryResult
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertIn
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertResult
import org.eclipse.tractusx.bpdm.gate.service.operation.businesspartner.BusinessPartnerInputUpsertOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.businesspartner.BusinessPartnerInputUpsertParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Upserts a batch of business partner inputs for every API version, all or nothing.
 */
@Service
class BusinessPartnerInputUpsertApplicationService(
    private val parser: BusinessPartnerInputUpsertParser,
    private val operationService: BusinessPartnerInputUpsertOperationService,
    private val recordMapper: BusinessPartnerInputRecordMapper
) {
    /**
     * Upserts [requests] for [tenantBpnl] and returns the inputs that were written, or writes nothing and returns the
     * violations of every entry if any entry is invalid.
     */
    @Transactional
    fun upsert(requests: List<BusinessPartnerInputUpsertIn>, tenantBpnl: String?): BusinessPartnerInputUpsertResult =
        when (val parseResult = parser.parse(requests)) {
            is ParseResult.Failure -> BusinessPartnerInputUpsertResult.Rejected(parseResult.errors)
            is ParseResult.Success -> {
                val applied = operationService.upsert(parseResult.parsed, tenantBpnl)
                    .filterIsInstance<BusinessPartnerInputUpsertEntryResult.Applied>()
                BusinessPartnerInputUpsertResult.Written(applied.map { recordMapper.toRecord(it.input) })
            }
        }
}
