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

package org.eclipse.tractusx.bpdm.gate.model.upsert.input

/**
 * Reason a business partner input upsert is rejected, either for the batch as a whole or for one of its entries.
 */
sealed interface BusinessPartnerInputUpsertError {
    val message: String

    data class UpsertLimitExceeded(val limit: Int, val actual: Int) : BusinessPartnerInputUpsertError {
        override val message get() = "At most $limit business partners may be upserted at once, but $actual were given"
    }

    data class DuplicateExternalIds(val externalIds: Set<String>) : BusinessPartnerInputUpsertError {
        override val message get() = "External IDs occur more than once in the batch: ${externalIds.joinToString()}"
    }

    data class AdditionalSitesWithoutOwnSite(val externalId: String) : BusinessPartnerInputUpsertError {
        override val message
            get() = "Partner with ID '$externalId' is invalid: additional sites of its address are stated " +
                    "but no site of its own is, which they would be additional to"
    }
}
