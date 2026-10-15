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

package org.eclipse.tractusx.bpdm.gate.service.businesspartner.input.parser

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.gate.mapper.parsed.BusinessPartnerInputUpsertParsedMapper
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertError
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertIn
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertParsed
import org.eclipse.tractusx.bpdm.gate.service.businesspartner.input.parser.validator.BusinessPartnerInputUpsertBatchValidator
import org.eclipse.tractusx.bpdm.gate.service.businesspartner.input.parser.validator.BusinessPartnerInputUpsertValidator
import org.springframework.stereotype.Service

/**
 * Turns business partner upsert entries into their parsed form, or into every rule violation they contain.
 */
@Service
class BusinessPartnerInputUpsertParser(
    private val normalizer: BusinessPartnerInputUpsertNormalizer,
    private val parsedMapper: BusinessPartnerInputUpsertParsedMapper,
    private val validators: List<BusinessPartnerInputUpsertValidator>,
    private val batchValidators: List<BusinessPartnerInputUpsertBatchValidator>
) {

    /**
     * Returns the violations of the rules over the batch's shape, or an empty list if the batch as a whole is sound.
     */
    fun validateBatch(requests: List<BusinessPartnerInputUpsertIn>): List<BusinessPartnerInputUpsertError> =
        batchValidators.flatMap { it.validate(requests) }

    /**
     * Parses a single entry, failing with the violations of every rule it breaks.
     */
    fun parse(request: BusinessPartnerInputUpsertIn): ParseResult<BusinessPartnerInputUpsertParsed, BusinessPartnerInputUpsertError> {
        val normalized = normalizer.normalize(request)
        val errors = validators.flatMap { it.validate(normalized) }

        return if (errors.isEmpty())
            ParseResult.Success(parsedMapper.toParsed(normalized))
        else
            ParseResult.Failure(errors)
    }
}
