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

package org.eclipse.tractusx.bpdm.gate.service.parser.businesspartner.validator

import org.eclipse.tractusx.bpdm.gate.config.ApiConfigProperties
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputUpsertError
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerInputUpsertRaw
import org.springframework.stereotype.Component

/**
 * Rejects a batch larger than the configured upsert limit.
 */
@Component
class UpsertLimitValidator(
    private val apiConfigProperties: ApiConfigProperties
) : BusinessPartnerInputUpsertBatchValidator {

    override fun validate(requests: List<BusinessPartnerInputUpsertRaw>): List<BusinessPartnerInputUpsertError> {
        val limit = apiConfigProperties.upsertLimit
        return if (requests.size > limit)
            listOf(BusinessPartnerInputUpsertError.UpsertLimitExceeded(limit, requests.size))
        else
            emptyList()
    }
}
