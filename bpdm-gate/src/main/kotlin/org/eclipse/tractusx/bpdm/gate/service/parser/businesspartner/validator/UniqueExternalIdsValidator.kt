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

import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertError
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertIn
import org.springframework.stereotype.Component

/**
 * Rejects a batch in which the same external ID occurs more than once.
 */
@Component
class UniqueExternalIdsValidator : BusinessPartnerInputUpsertBatchValidator {

    override fun validate(requests: List<BusinessPartnerInputUpsertIn>): List<BusinessPartnerInputUpsertError> {
        val duplicates = requests.groupingBy { it.externalId }.eachCount().filterValues { it > 1 }.keys
        return if (duplicates.isNotEmpty())
            listOf(BusinessPartnerInputUpsertError.DuplicateExternalIds(duplicates))
        else
            emptyList()
    }
}
