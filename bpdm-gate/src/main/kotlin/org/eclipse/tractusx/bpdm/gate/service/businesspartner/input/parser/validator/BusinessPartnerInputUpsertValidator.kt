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

/**
 * One validation rule over a single normalized business partner upsert entry.
 *
 * A rule sees only the normalized entry and never the verdict of another rule, so every rule runs on every entry and a
 * rejection reports all violations at once.
 */
interface BusinessPartnerInputUpsertValidator {

    /**
     * Returns the violations of this rule in [request], or an empty list if it holds.
     */
    fun validate(request: BusinessPartnerInputUpsertIn): List<BusinessPartnerInputUpsertError>
}
