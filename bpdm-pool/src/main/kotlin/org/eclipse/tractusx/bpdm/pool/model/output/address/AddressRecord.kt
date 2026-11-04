/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.bpdm.pool.model.output.address

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessStateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.ConfidenceCriteriaRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.RelationValidityPeriodRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.AdministrativeAreaRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.CountryRecord
import java.time.Instant

data class AddressRecord(
    val bpn: String,
    val legalEntityBpn: String?,
    val siteBpn: String?,
    val additionalSiteBpns: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val name: String?,
    val states: List<BusinessStateRecord>,
    val identifiers: List<AddressIdentifierRecord>,
    val relations: List<AddressRelationRecord>,
    val physicalPostalAddress: PhysicalPostalAddressRecord,
    val alternativePostalAddress: AlternativePostalAddressRecord?,
    val confidenceCriteria: ConfidenceCriteriaRecord,
    val isDataSpaceParticipant: Boolean,
    val addressType: AddressType,
    val scriptVariants: List<AddressScriptVariantRecord>
)

data class AddressIdentifierRecord(
    val value: String,
    val typeTechnicalKey: String,
    val typeName: String
)

data class AddressRelationRecord(
    val type: AddressRelationTypeV7,
    val sourceBpna: String,
    val targetBpna: String,
    val validityPeriods: List<RelationValidityPeriodRecord>,
    val reasonCode: String?
)

data class PhysicalPostalAddressRecord(
    val geographicCoordinates: GeoCoordinateRecord?,
    val country: CountryRecord,
    val administrativeAreaLevel1: AdministrativeAreaRecord?,
    val administrativeAreaLevel2: String?,
    val administrativeAreaLevel3: String?,
    val postalCode: String?,
    val city: String,
    val district: String?,
    val street: StreetRecord?,
    val companyPostalCode: String?,
    val industrialZone: String?,
    val building: String?,
    val floor: String?,
    val door: String?,
    val taxJurisdictionCode: String?
)

data class AlternativePostalAddressRecord(
    val geographicCoordinates: GeoCoordinateRecord?,
    val country: CountryRecord,
    val administrativeAreaLevel1: AdministrativeAreaRecord?,
    val postalCode: String?,
    val city: String,
    val deliveryServiceType: DeliveryServiceType,
    val deliveryServiceNumber: String,
    val deliveryServiceQualifier: String?
)

data class GeoCoordinateRecord(
    val longitude: Double,
    val latitude: Double,
    val altitude: Double?
)

data class StreetRecord(
    val name: String?,
    val houseNumber: String?,
    val houseNumberSupplement: String?,
    val milestone: String?,
    val direction: String?,
    val namePrefix: String?,
    val additionalNamePrefix: String?,
    val nameSuffix: String?,
    val additionalNameSuffix: String?
)

data class AddressScriptVariantRecord(
    val scriptCode: String,
    val name: String?,
    val physicalAddress: PhysicalAddressScriptVariantRecord,
    val alternativeAddress: AlternativeAddressScriptVariantRecord?
)

data class PhysicalAddressScriptVariantRecord(
    val city: String,
    val district: String?,
    val street: StreetScriptVariantRecord?,
    val industrialZone: String?,
    val building: String?,
    val floor: String?,
    val door: String?
)

data class StreetScriptVariantRecord(
    val name: String?,
    val direction: String?,
    val namePrefix: String?,
    val additionalNamePrefix: String?,
    val nameSuffix: String?,
    val additionalNameSuffix: String?
)

data class AlternativeAddressScriptVariantRecord(
    val city: String
)
