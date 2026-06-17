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

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.eclipse.tractusx.bpdm.pool.entity.RegionDb
import java.time.Instant

/**
 * Bounded address whose metadata has been resolved to entities: identifier type, administrative area level 1 (region)
 * and (via [AddressScriptVariantParsed]) script code. This is the middle pipeline stage between [LogisticAddressRequest]
 * and the entity-free [LogisticAddress] result; only this stage carries persistence entities. Fields without metadata
 * (states, confidence, geo coordinate, street) reuse the bounded value types.
 */
data class LogisticAddressParsed(
    val name: String?,
    val states: List<AddressState>,
    val identifiers: List<AddressIdentifierParsed>,
    val physicalPostalAddress: PhysicalPostalAddressParsed,
    val alternativePostalAddress: AlternativePostalAddressParsed?,
    val confidenceCriteria: ConfidenceCriteriaParsed
)

/**
 * Inbound (upsert) confidence criteria: only the fields a caller actually supplies. `numberOfSharingMembers` and
 * `confidenceLevel` are Pool-computed, not upserted, so they appear solely on the outbound [ConfidenceCriteria].
 */
data class ConfidenceCriteriaParsed(
    val sharedByOwner: Boolean,
    val checkedByExternalDataSource: Boolean,
    val lastConfidenceCheckAt: Instant,
    val nextConfidenceCheckAt: Instant
)

data class PhysicalPostalAddressParsed(
    val geographicCoordinates: GeoCoordinate?,
    val country: CountryCode,
    val administrativeAreaLevel1: RegionDb?,
    val administrativeAreaLevel2: String?,
    val administrativeAreaLevel3: String?,
    val postalCode: String?,
    val city: String,
    val district: String?,
    val street: Street?,
    val companyPostalCode: String?,
    val industrialZone: String?,
    val building: String?,
    val floor: String?,
    val door: String?,
    val taxJurisdictionCode: String?
)

data class AlternativePostalAddressParsed(
    val geographicCoordinates: GeoCoordinate?,
    val country: CountryCode,
    val administrativeAreaLevel1: RegionDb?,
    val postalCode: String?,
    val city: String,
    val deliveryServiceType: DeliveryServiceType,
    val deliveryServiceQualifier: String?,
    val deliveryServiceNumber: String
)

data class AddressIdentifierParsed(
    val value: String,
    val type: IdentifierTypeDb
)

/** Bundle of a parsed address with its parsed script variants — the success payload of the shared content parser. */
data class AddressContentParsed(
    val address: LogisticAddressParsed,
    val scriptVariants: List<AddressScriptVariantParsed>
)
