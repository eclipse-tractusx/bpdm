/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.model.raw.address

import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import java.time.Instant

data class LogisticAddressRaw(
    val name: String?,
    val states: List<AddressStateRaw>,
    val identifiers: List<AddressIdentifierRaw>,
    val physicalPostalAddress: PhysicalPostalAddressRaw,
    val alternativePostalAddress: AlternativePostalAddressRaw?,
    val confidenceCriteria: ConfidenceCriteriaRaw,
    val scriptVariants: List<AddressScriptVariantRaw>
)

data class PhysicalPostalAddressRaw(
    val geographicCoordinates: GeoCoordinateRaw?,
    val country: String?,
    val administrativeAreaLevel1: String?,
    val administrativeAreaLevel2: String?,
    val administrativeAreaLevel3: String?,
    val postalCode: String?,
    val city: String?,
    val district: String?,
    val street: StreetRaw?,
    val companyPostalCode: String?,
    val industrialZone: String?,
    val building: String?,
    val floor: String?,
    val door: String?,
    val taxJurisdictionCode: String?
)

data class AlternativePostalAddressRaw(
    val geographicCoordinates: GeoCoordinateRaw?,
    val country: String?,
    val administrativeAreaLevel1: String?,
    val postalCode: String?,
    val city: String?,
    val deliveryServiceType: DeliveryServiceType?,
    val deliveryServiceQualifier: String?,
    val deliveryServiceNumber: String?
)

data class GeoCoordinateRaw(
    val longitude: Double?,
    val latitude: Double?,
    val altitude: Double?
)

data class AddressStateRaw(
    val validFrom: Instant?,
    val validTo: Instant?,
    val type: BusinessStateType?
)

data class AddressIdentifierRaw(
    val value: String?,
    val type: String?
)
