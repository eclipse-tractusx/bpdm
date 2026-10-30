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

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.combine
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerCsvRowError
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerCsvTenantParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerCsvUpsertError
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerCsvRowRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerInputUpsertRaw
import org.springframework.stereotype.Service

/**
 * Turns one row of a business partner CSV upload into a business partner input upsert entry of the uploading tenant.
 *
 * Every row describes the tenant's own company data, so each entry is assigned the tenant's legal entity rather than
 * one the row states.
 */
@Service
class BusinessPartnerCsvRowParser(
    private val stateParser: BusinessPartnerCsvStateParser,
    private val addressParser: BusinessPartnerCsvAddressParser
) {

    /**
     * Returns the upsert entry [row] describes for [tenant], or fails with every invalid cell, each reported with
     * [rowNumber] and the row's external ID.
     */
    fun parse(row: BusinessPartnerCsvRowRaw, rowNumber: Int, tenant: BusinessPartnerCsvTenantParsed): ParseResult<BusinessPartnerInputUpsertRaw, BusinessPartnerCsvUpsertError> {
        val externalId = CsvCell(BusinessPartnerCsvRowRaw.Columns.EXTERNAL_ID, row.externalId).value

        val result = zipParseResults(parseExternalId(externalId), parseSite(row), addressParser.parse(row)) { parsedExternalId, site, address ->
            BusinessPartnerInputUpsertRaw(
                externalId = parsedExternalId,
                nameParts = emptyList(),
                identifiers = identifiers(row),
                states = emptyList(),
                roles = emptyList(),
                isOwnCompanyData = true,
                legalEntity = BusinessPartnerInputUpsertRaw.LegalEntity(
                    bpn = tenant.bpnl,
                    legalName = tenant.legalName,
                    shortName = null,
                    legalForm = null,
                    ownershipUltimate = null,
                    ultimateOwnerBpnl = null,
                    states = emptyList()
                ),
                site = site,
                address = address,
                externalSequenceTimestamp = null,
                scriptVariants = emptyList(),
                additionalSites = emptyList()
            )
        }

        return when (result) {
            is ParseResult.Success -> result
            is ParseResult.Failure -> ParseResult.Failure(result.errors.map { BusinessPartnerCsvUpsertError.InvalidRow(rowNumber, externalId, it) })
        }
    }

    private fun parseExternalId(externalId: String?): ParseResult<String, BusinessPartnerCsvRowError> =
        externalId
            ?.let { ParseResult.Success(it) }
            ?: ParseResult.ofSingleFailure(BusinessPartnerCsvRowError.MissingExternalId)

    private fun parseSite(row: BusinessPartnerCsvRowRaw): ParseResult<BusinessPartnerInputUpsertRaw.Site, BusinessPartnerCsvRowError> =
        with(row) {
            stateParser.parse(
                CsvCell(BusinessPartnerCsvRowRaw.Columns.SITE_STATES_TYPE, siteStatesType),
                CsvCell(BusinessPartnerCsvRowRaw.Columns.SITE_STATES_VALID_FROM, siteStatesValidFrom),
                CsvCell(BusinessPartnerCsvRowRaw.Columns.SITE_STATES_VALID_TO, siteStatesValidTo)
            ).combine(emptyList()) { state ->
                BusinessPartnerInputUpsertRaw.Site(bpn = cellText(siteBpn), name = cellText(siteName), states = listOf(state))
            }
        }

    private fun identifiers(row: BusinessPartnerCsvRowRaw) =
        with(row) {
            listOf(
                identifier(identifiersType1, identifiersValue1, identifiersIssuingBody1),
                identifier(identifiersType2, identifiersValue2, identifiersIssuingBody2),
                identifier(identifiersType3, identifiersValue3, identifiersIssuingBody3)
            )
        }

    private fun identifier(type: String?, value: String?, issuingBody: String?) =
        BusinessPartnerInputUpsertRaw.Identifier(type = cellText(type), value = cellText(value), issuingBody = cellText(issuingBody))
}
