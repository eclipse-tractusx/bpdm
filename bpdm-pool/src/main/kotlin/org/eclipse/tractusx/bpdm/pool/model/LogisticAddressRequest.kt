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

package org.eclipse.tractusx.bpdm.pool.model

import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import java.time.Instant

/**
 * Loose (unvalidated) inbound counterpart of [LogisticAddress]: every constraint that distinguishes a valid address is
 * relaxed (country as raw String, city/delivery fields/confidence nullable) so a single `parse` can be the one validation
 * funnel for all callers. `parse` turns this into the bounded [LogisticAddressParsed]. Already-nullable value types (`Street`,
 * the script-variant types) are reused as-is rather than re-declared as request variants.
 */
data class LogisticAddressRequest(
    val name: String?,
    val states: List<AddressStateRequest>,
    val identifiers: List<AddressIdentifierRequest>,
    val physicalPostalAddress: PhysicalPostalAddressRequest,
    val alternativePostalAddress: AlternativePostalAddressRequest?,
    val confidenceCriteria: ConfidenceCriteriaRequest
)

/** Bundle of a loose address with its script variants — the input unit of the shared content parser, mirroring [org.eclipse.tractusx.bpdm.pool.model.AddressContentParsed]. */
data class AddressContentRequest(
    val address: LogisticAddressRequest,
    val scriptVariants: List<AddressScriptVariant>
)

data class PhysicalPostalAddressRequest(
    val geographicCoordinates: GeoCoordinateRequest?,
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

data class AlternativePostalAddressRequest(
    val geographicCoordinates: GeoCoordinateRequest?,
    val country: String?,
    val administrativeAreaLevel1: String?,
    val postalCode: String?,
    val city: String?,
    val deliveryServiceType: DeliveryServiceType?,
    val deliveryServiceQualifier: String?,
    val deliveryServiceNumber: String?
)

data class GeoCoordinateRequest(
    val longitude: Double?,
    val latitude: Double?,
    val altitude: Double?
)

data class ConfidenceCriteriaRequest(
    val sharedByOwner: Boolean?,
    val checkedByExternalDataSource: Boolean?,
    val lastConfidenceCheckAt: Instant?,
    val nextConfidenceCheckAt: Instant?
)

data class AddressStateRequest(
    val validFrom: Instant?,
    val validTo: Instant?,
    val type: BusinessStateType?
)

data class AddressIdentifierRequest(
    val value: String?,
    val type: String?
)
