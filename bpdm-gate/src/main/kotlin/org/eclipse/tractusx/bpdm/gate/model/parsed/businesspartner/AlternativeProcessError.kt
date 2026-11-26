/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner

/** A validation failure in an alternative-process request. */
sealed interface AlternativeProcessError {
    data class DuplicateExternalIds(val externalIds: Set<String>) : AlternativeProcessError
    data class BusinessPartnerNotFound(val externalId: String) : AlternativeProcessError
    data class ProcessMismatch(val externalId: String, val process: String?) : AlternativeProcessError
    data class SiteNameMissing(val externalId: String) : AlternativeProcessError
    data class InvalidInput(val error: BusinessPartnerInputUpsertError) : AlternativeProcessError
}
