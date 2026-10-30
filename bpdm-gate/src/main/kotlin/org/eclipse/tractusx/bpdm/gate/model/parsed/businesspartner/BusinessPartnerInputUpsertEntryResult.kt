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

package org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner

import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb

/**
 * What upserting a single business partner input did: applied it in one of three ways, or skipped it for a reason.
 */
sealed interface BusinessPartnerInputUpsertEntryResult {

    sealed interface Applied : BusinessPartnerInputUpsertEntryResult {
        val input: BusinessPartnerDb
    }

    data class Created(override val input: BusinessPartnerDb) : Applied
    data class Updated(override val input: BusinessPartnerDb) : Applied
    data class Resubmitted(override val input: BusinessPartnerDb) : Applied
    data class Skipped(val externalId: String, val reason: SkipReason) : BusinessPartnerInputUpsertEntryResult

    enum class SkipReason {
        Outdated,
        Unchanged
    }
}
