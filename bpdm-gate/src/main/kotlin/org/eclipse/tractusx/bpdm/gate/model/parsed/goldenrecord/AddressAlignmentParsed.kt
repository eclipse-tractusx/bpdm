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

package org.eclipse.tractusx.bpdm.gate.model.parsed.goldenrecord

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.AdditionalSiteRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.AddressGoldenRecordRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.SiteGoldenRecordRaw

/**
 * The address golden record a business partner output takes on, with the site it brings along.
 *
 * Guarantees:
 * - [address] is not older than the address golden record the output holds.
 * - [adoptedSite] is set only when the output tracks no site, and then is the site [address] belongs to.
 */
data class AddressAlignmentParsed(
    val address: AddressGoldenRecordRaw,
    val addressType: AddressType,
    val additionalSites: List<AdditionalSiteRaw>,
    val adoptedSite: SiteGoldenRecordRaw?
)
