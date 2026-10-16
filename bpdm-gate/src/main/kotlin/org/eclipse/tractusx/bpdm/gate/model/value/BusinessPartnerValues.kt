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

package org.eclipse.tractusx.bpdm.gate.model.value

import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType

/**
 * Value types a business partner carries unchanged through every stage and representation.
 */
object BusinessPartnerValues {
    data class PhysicalPostalAddress(
        val geographicCoordinates: GeoCoordinate?,
        val country: String?,
        val administrativeAreaLevel1: String?,
        val administrativeAreaLevel2: String?,
        val administrativeAreaLevel3: String?,
        val postalCode: String?,
        val city: String?,
        val district: String?,
        val street: Street?,
        val companyPostalCode: String?,
        val industrialZone: String?,
        val building: String?,
        val floor: String?,
        val door: String?,
        val taxJurisdictionCode: String?
    )

    data class AlternativePostalAddress(
        val geographicCoordinates: GeoCoordinate?,
        val country: String?,
        val administrativeAreaLevel1: String?,
        val postalCode: String?,
        val city: String?,
        val deliveryServiceType: DeliveryServiceType?,
        val deliveryServiceQualifier: String?,
        val deliveryServiceNumber: String?
    )

    data class AdditionalSite(
        val bpn: String?,
        val name: String?
    )

    data class ScriptVariant(
        val scriptCode: String,
        val nameParts: List<String>,
        val legalName: String?,
        val shortName: String?,
        val siteName: String?,
        val addressName: String?,
        val physicalAddress: PhysicalAddressScriptVariant,
        val alternativeAddressCity: String?
    )

    data class PhysicalAddressScriptVariant(
        val city: String?,
        val district: String?,
        val street: StreetScriptVariant,
        val industrialZone: String?,
        val building: String?,
        val floor: String?,
        val door: String?
    )

    data class StreetScriptVariant(
        val name: String?,
        val direction: String?,
        val namePrefix: String?,
        val additionalNamePrefix: String?,
        val nameSuffix: String?,
        val additionalNameSuffix: String?
    )
}
