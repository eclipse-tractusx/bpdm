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

/**
 * Reason a business partner input upsert is rejected, either for the batch as a whole or for one of its entries.
 */
sealed interface BusinessPartnerInputUpsertError {
    data class UpsertLimitExceeded(val limit: Int, val actual: Int) : BusinessPartnerInputUpsertError
    data class DuplicateExternalIds(val externalIds: Set<String>) : BusinessPartnerInputUpsertError
    data class AdditionalSitesWithoutOwnSite(val externalId: String) : BusinessPartnerInputUpsertError
}
