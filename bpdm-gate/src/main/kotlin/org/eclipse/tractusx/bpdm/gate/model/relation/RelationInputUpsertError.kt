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


package org.eclipse.tractusx.bpdm.gate.model.relation

import java.time.LocalDate

/**
 * Reason a relation input upsert is rejected, either for the batch as a whole or for one of its entries.
 */
sealed interface RelationInputUpsertError {
    data class UpsertLimitExceeded(val limit: Int, val actual: Int) : RelationInputUpsertError
    data class DuplicateExternalIds(val externalIds: Set<String>) : RelationInputUpsertError
    data class NotFound(val externalId: String?) : RelationInputUpsertError
    data class AlreadyExists(val externalId: String) : RelationInputUpsertError
    data class SourceNotFound(val externalId: String?, val sourceExternalId: String) : RelationInputUpsertError
    data class TargetNotFound(val externalId: String?, val targetExternalId: String) : RelationInputUpsertError
    data class SourceEqualsTarget(val externalId: String?, val businessPartnerExternalId: String) : RelationInputUpsertError
    data class InvalidValidityPeriod(val externalId: String?, val validFrom: LocalDate, val validTo: LocalDate) : RelationInputUpsertError
}
