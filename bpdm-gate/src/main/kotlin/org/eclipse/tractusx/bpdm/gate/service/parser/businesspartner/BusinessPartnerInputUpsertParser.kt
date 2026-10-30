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

package org.eclipse.tractusx.bpdm.gate.service.parser.businesspartner

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.gate.mapper.parsed.BusinessPartnerInputUpsertParsedMapper
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputUpsertError
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputUpsertParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerInputUpsertRaw
import org.eclipse.tractusx.bpdm.gate.service.parser.businesspartner.validator.BusinessPartnerInputUpsertBatchValidator
import org.eclipse.tractusx.bpdm.gate.service.parser.businesspartner.validator.BusinessPartnerInputUpsertValidator
import org.springframework.stereotype.Service

/**
 * Turns a batch of business partner upsert entries into their parsed form, or into every rule violation it contains.
 */
@Service
class BusinessPartnerInputUpsertParser(
    private val normalizer: BusinessPartnerInputUpsertNormalizer,
    private val parsedMapper: BusinessPartnerInputUpsertParsedMapper,
    private val validators: List<BusinessPartnerInputUpsertValidator>,
    private val batchValidators: List<BusinessPartnerInputUpsertBatchValidator>
) {

    /**
     * Parses [requests] as a whole, succeeding only if every entry is valid and failing with the violations of every
     * entry otherwise.
     */
    fun parse(requests: List<BusinessPartnerInputUpsertRaw>): ParseResult<List<BusinessPartnerInputUpsertParsed>, BusinessPartnerInputUpsertError> {
        val batchErrors = batchValidators.flatMap { it.validate(requests) }
        if (batchErrors.isNotEmpty()) return ParseResult.Failure(batchErrors)

        val normalized = requests.map(normalizer::normalize)
        val errors = normalized.flatMap { request -> validators.flatMap { it.validate(request) } }

        return if (errors.isEmpty())
            ParseResult.Success(normalized.map(parsedMapper::toParsed))
        else
            ParseResult.Failure(errors)
    }
}
