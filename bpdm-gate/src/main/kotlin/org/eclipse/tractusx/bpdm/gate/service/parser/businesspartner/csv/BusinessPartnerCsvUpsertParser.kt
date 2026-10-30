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

package org.eclipse.tractusx.bpdm.gate.service.parser.businesspartner.csv

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerCsvRow
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerCsvUpsertError
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertParsed
import org.eclipse.tractusx.bpdm.gate.service.parser.businesspartner.BusinessPartnerInputUpsertParser
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.springframework.stereotype.Service

/**
 * Turns the rows of a business partner CSV upload into parsed business partner inputs of the uploading tenant, or into
 * every violation the upload contains.
 */
@Service
class BusinessPartnerCsvUpsertParser(
    private val poolApiClient: PoolApiClient,
    private val rowParser: BusinessPartnerCsvRowParser,
    private val inputUpsertParser: BusinessPartnerInputUpsertParser
) {

    /**
     * Parses [rows] for [tenantBpnl] as a whole, succeeding only if the tenant is a legal entity known to the Pool and
     * every row is valid, and failing with every violation otherwise.
     */
    fun parse(rows: List<BusinessPartnerCsvRow>, tenantBpnl: String?): ParseResult<List<BusinessPartnerInputUpsertParsed>, BusinessPartnerCsvUpsertError> {
        val tenant = when (val tenantResult = resolveTenant(tenantBpnl)) {
            is ParseResult.Failure -> return tenantResult
            is ParseResult.Success -> tenantResult.parsed
        }

        val rowResults = rows.mapIndexed { index, row -> rowParser.parse(row, index + FIRST_ROW_NUMBER, tenant) }
        val rowErrors = rowResults.flatMap { (it as? ParseResult.Failure)?.errors.orEmpty() }
        if (rowErrors.isNotEmpty()) return ParseResult.Failure(rowErrors)

        return when (val inputResult = inputUpsertParser.parse(rowResults.map { (it as ParseResult.Success).parsed })) {
            is ParseResult.Success -> inputResult
            is ParseResult.Failure -> ParseResult.Failure(inputResult.errors.map { BusinessPartnerCsvUpsertError.InputRejected(it) })
        }
    }

    private fun resolveTenant(tenantBpnl: String?): ParseResult<BusinessPartnerCsvTenant, BusinessPartnerCsvUpsertError> {
        if (tenantBpnl.isNullOrEmpty() || !tenantBpnl.startsWith("BPNL"))
            return ParseResult.ofSingleFailure(BusinessPartnerCsvUpsertError.InvalidTenantBpnl(tenantBpnl))

        val legalEntity = poolApiClient.legalEntities
            .getLegalEntities(LegalEntitySearchRequest(listOf(tenantBpnl)), PaginationRequest(page = 0, size = 1))
            .content.firstOrNull()
            ?: return ParseResult.ofSingleFailure(BusinessPartnerCsvUpsertError.TenantLegalEntityNotFound(tenantBpnl))

        return ParseResult.Success(BusinessPartnerCsvTenant(tenantBpnl, legalEntity.header.legalName))
    }

    private companion object {
        // The header occupies the first line of the file, so the first business partner is on line 2.
        const val FIRST_ROW_NUMBER = 2
    }
}
