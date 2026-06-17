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
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import java.time.Instant

/**
 * Bounded (validated) internal representation of a logistic address, decoupled from the API DTO and the JPA entity.
 * Shared by the parsed and result pipeline stages; the loose inbound counterpart is [LogisticAddressRequest].
 */
data class LogisticAddress(
    val name: String?,
    val states: List<AddressState>,
    val identifiers: List<AddressIdentifier>,
    val physicalPostalAddress: PhysicalPostalAddress,
    val alternativePostalAddress: AlternativePostalAddress?,
    val confidenceCriteria: ConfidenceCriteria
)

data class PhysicalPostalAddress(
    val geographicCoordinates: GeoCoordinate?,
    val country: CountryCode,
    val administrativeAreaLevel1: String?,
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

data class AlternativePostalAddress(
    val geographicCoordinates: GeoCoordinate?,
    val country: CountryCode,
    val administrativeAreaLevel1: String?,
    val postalCode: String?,
    val city: String,
    val deliveryServiceType: DeliveryServiceType,
    val deliveryServiceQualifier: String?,
    val deliveryServiceNumber: String
)

data class Street(
    val name: String? = null,
    val houseNumber: String? = null,
    val houseNumberSupplement: String? = null,
    val milestone: String? = null,
    val direction: String? = null,
    val namePrefix: String? = null,
    val additionalNamePrefix: String? = null,
    val nameSuffix: String? = null,
    val additionalNameSuffix: String? = null
)

data class AddressState(
    val validFrom: Instant?,
    val validTo: Instant?,
    val type: BusinessStateType
)

data class AddressIdentifier(
    val value: String,
    val type: String
)
