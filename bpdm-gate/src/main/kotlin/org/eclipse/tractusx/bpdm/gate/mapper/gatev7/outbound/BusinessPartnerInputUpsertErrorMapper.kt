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

package org.eclipse.tractusx.bpdm.gate.mapper.gatev7.outbound

import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputUpsertError
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputUpsertError.AdditionalSitesWithoutOwnSite
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputUpsertError.DuplicateExternalIds
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputUpsertError.UpsertLimitExceeded
import org.springframework.stereotype.Component

/**
 * Maps a rejected business partner input upsert to the error messages the API reports.
 */
@Component
class BusinessPartnerInputUpsertErrorMapper {

    /**
     * Returns the message the API reports for [error].
     */
    fun toMessage(error: BusinessPartnerInputUpsertError): String =
        when (error) {
            is UpsertLimitExceeded ->
                "At most ${error.limit} business partners may be upserted at once, but ${error.actual} were given"
            is DuplicateExternalIds ->
                "External IDs occur more than once in the batch: ${error.externalIds.joinToString()}"
            is AdditionalSitesWithoutOwnSite ->
                "Partner with ID '${error.externalId}' is invalid: additional sites of its address are stated " +
                        "but no site of its own is, which they would be additional to"
        }
}
