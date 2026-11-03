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
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerCsvRowError
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerCsvRowRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerInputUpsertRaw
import org.springframework.stereotype.Service

/**
 * Turns the address cells of a business partner CSV row into the address of the business partner.
 */
@Service
class BusinessPartnerCsvAddressParser(
    private val addressTypeParser: BusinessPartnerCsvAddressTypeParser,
    private val postalAddressParser: BusinessPartnerCsvPostalAddressParser,
    private val stateParser: BusinessPartnerCsvStateParser
) {

    /**
     * Returns the address [row] describes, or fails with every cell that holds no valid value.
     */
    fun parse(row: BusinessPartnerCsvRowRaw): ParseResult<BusinessPartnerInputUpsertRaw.Address, BusinessPartnerCsvRowError> =
        with(row) {
            zipParseResults(
                addressTypeParser.parse(CsvCell(BusinessPartnerCsvRowRaw.Columns.ADDRESS_TYPE, addressType)),
                zipParseResults(postalAddressParser.parsePhysical(row), postalAddressParser.parseAlternative(row), ::Pair),
                stateParser.parse(
                    CsvCell(BusinessPartnerCsvRowRaw.Columns.ADDRESS_STATES_TYPE, addressStatesType),
                    CsvCell(BusinessPartnerCsvRowRaw.Columns.ADDRESS_STATES_VALID_FROM, addressStatesValidFrom),
                    CsvCell(BusinessPartnerCsvRowRaw.Columns.ADDRESS_STATES_VALID_TO, addressStatesValidTo)
                )
            ) { parsedAddressType, (physicalPostalAddress, alternativePostalAddress), state ->
                BusinessPartnerInputUpsertRaw.Address(
                    bpn = cellText(addressBpn),
                    name = cellText(addressName),
                    addressType = parsedAddressType,
                    physicalPostalAddress = physicalPostalAddress,
                    alternativePostalAddress = alternativePostalAddress,
                    states = listOf(state)
                )
            }
        }
}
