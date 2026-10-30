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
 * The tenant a business partner CSV upload is made for.
 *
 * Guarantees:
 * - [bpnl] is the BPNL of a legal entity the Pool knows, and [legalName] is that legal entity's legal name.
 */
data class BusinessPartnerCsvTenantParsed(
    val bpnl: String,
    val legalName: String
)
