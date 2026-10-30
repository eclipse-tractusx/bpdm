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
import org.eclipse.tractusx.bpdm.gate.model.output.businesspartner.BusinessPartnerCsvUpsertResult
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputUpsertEntryResult
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerCsvRowRaw
import org.eclipse.tractusx.bpdm.gate.service.operation.businesspartner.BusinessPartnerInputUpsertOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.businesspartner.csv.BusinessPartnerCsvUpsertParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Upserts the business partner inputs of an uploaded CSV file, all or nothing.
 */
@Service
class BusinessPartnerCsvUpsertApplicationService(
    private val parser: BusinessPartnerCsvUpsertParser,
    private val operationService: BusinessPartnerInputUpsertOperationService,
    private val recordMapper: BusinessPartnerInputRecordMapper
) {

    /**
     * Upserts the business partner inputs [rows] describe for [tenantBpnl] and returns the inputs that were written, or
     * writes nothing and returns every violation if the tenant or any row is invalid.
     */
    @Transactional
    fun upload(rows: List<BusinessPartnerCsvRowRaw>, tenantBpnl: String?): BusinessPartnerCsvUpsertResult =
        when (val parseResult = parser.parse(rows, tenantBpnl)) {
            is ParseResult.Failure -> BusinessPartnerCsvUpsertResult.Rejected(parseResult.errors)
            is ParseResult.Success -> {
                val applied = operationService.upsert(parseResult.parsed, tenantBpnl)
                    .filterIsInstance<BusinessPartnerInputUpsertEntryResult.Applied>()
                BusinessPartnerCsvUpsertResult.Written(applied.map { recordMapper.toRecord(it.input) })
            }
        }
}
