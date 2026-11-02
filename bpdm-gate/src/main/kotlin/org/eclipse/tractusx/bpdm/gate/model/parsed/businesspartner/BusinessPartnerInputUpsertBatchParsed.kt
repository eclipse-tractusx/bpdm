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
 * A batch of business partner input upsert entries.
 *
 * Guarantees:
 * - No two entries name the same external ID.
 */
data class BusinessPartnerInputUpsertBatchParsed(
    val entries: List<BusinessPartnerInputUpsertParsed>
)
