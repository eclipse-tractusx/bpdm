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

package org.eclipse.tractusx.bpdm.gate.service.businesspartner.input.parser.validator

import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertError
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertIn
import org.springframework.stereotype.Component

/**
 * Rejects a business partner that states further sites of its address without stating a site of its own, which those
 * sites would be additional to.
 */
@Component
class AdditionalSitesNeedOwnSiteValidator : BusinessPartnerInputUpsertValidator {

    override fun validate(request: BusinessPartnerInputUpsertIn): List<BusinessPartnerInputUpsertError> {
        val statesSite = request.site.bpn != null || request.site.name != null
        return if (request.additionalSites.isNotEmpty() && !statesSite)
            listOf(BusinessPartnerInputUpsertError.AdditionalSitesWithoutOwnSite(request.externalId))
        else
            emptyList()
    }
}
