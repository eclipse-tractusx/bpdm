/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.orchestrator.entity

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType

@Embeddable
data class PostalAddressDb(
    @Column(name = "address_name")
    val addressName: String?,
    @Embedded
    val physicalAddress: PhysicalAddressDb,
    @Embedded
    val alternativeAddress: AlternativeAddress,
    @Column(name = "has_changed")
    val hasChanged: Boolean?
) {
    //associate also with equivalent scopes of other entities
    enum class Scope(
        val bpnReference: BpnReferenceDb.Scope,
        val identifier: IdentifierDb.Scope,
        val state: BusinessStateDb.Scope,
        val confidence: ConfidenceCriteriaDb.Scope
    ) {
        LegalAddress(
            BpnReferenceDb.Scope.LegalAddress,
            IdentifierDb.Scope.LegalAddress,
            BusinessStateDb.Scope.LegalAddress,
            ConfidenceCriteriaDb.Scope.LegalAddress
        ),
        SiteMainAddress(
            BpnReferenceDb.Scope.SiteMainAddress,
            IdentifierDb.Scope.SiteMainAddress,
            BusinessStateDb.Scope.SiteMainAddress,
            ConfidenceCriteriaDb.Scope.SiteMainAddress
        ),
        AdditionalAddress(
            BpnReferenceDb.Scope.AdditionalAddress,
            IdentifierDb.Scope.AdditionalAddress,
            BusinessStateDb.Scope.AdditionalAddress,
            ConfidenceCriteriaDb.Scope.AdditionalAddress
        ),
        UncategorizedAddress(
            BpnReferenceDb.Scope.UncategorizedAddress,
            IdentifierDb.Scope.UncategorizedAddress,
            BusinessStateDb.Scope.UncategorizedAddress,
            ConfidenceCriteriaDb.Scope.UncategorizedAddress
        )
    }

    @Embeddable
    data class PhysicalAddressDb(
        @Embedded
        @AttributeOverride(name = "latitude", column = Column(name = "phy_latitude"))
        @AttributeOverride(name = "longitude", column = Column(name = "phy_longitude"))
        @AttributeOverride(name = "altitude", column = Column(name = "phy_altitude"))
        val geographicCoordinates: GeoCoordinate,
        @Column(name = "phy_country")
        val country: String?,
        @Column(name = "phy_admin_area_l1_region")
        val administrativeAreaLevel1: String?,
        @Column(name = "phy_admin_area_l2")
        val administrativeAreaLevel2: String?,
        @Column(name = "phy_admin_area_l3")
        val administrativeAreaLevel3: String?,
        @Column(name = "phy_postcode")
        val postalCode: String?,
        @Column(name = "phy_city")
        val city: String?,
        @Column(name = "phy_district_l1")
        val district: String?,
        @Embedded
        val street: Street,
        @Column(name = "phy_company_postcode")
        val companyPostalCode: String?,
        @Column(name = "phy_industrial_zone")
        val industrialZone: String?,
        @Column(name = "phy_building")
        val building: String?,
        @Column(name = "phy_floor")
        val floor: String?,
        @Column(name = "phy_door")
        val door: String?,
        @Column(name = "phy_tax_jurisdiction")
        val taxJurisdictionCode: String?
    )

    @Embeddable
    data class AlternativeAddress(
        @Column(name = "alt_exists", nullable = false)
        val exists: Boolean,
        @Embedded
        @AttributeOverride(name = "latitude", column = Column(name = "alt_latitude"))
        @AttributeOverride(name = "longitude", column = Column(name = "alt_longitude"))
        @AttributeOverride(name = "altitude", column = Column(name = "alt_altitude"))
        val geographicCoordinates: GeoCoordinate,
        @Column(name = "alt_country")
        val country: String?,
        @Column(name = "alt_admin_area_l1_region")
        val administrativeAreaLevel1: String?,
        @Column(name = "alt_postcode")
        val postalCode: String?,
        @Column(name = "alt_city")
        val city: String?,
        @Column(name = "alt_delivery_service_type")
        @Enumerated(EnumType.STRING)
        val deliveryServiceType: DeliveryServiceType?,
        @Column(name = "alt_delivery_service_qualifier")
        val deliveryServiceQualifier: String?,
        @Column(name = "alt_delivery_service_number")
        val deliveryServiceNumber: String?
    )

    @Embeddable
    data class GeoCoordinate(
        @Column(name = "longitude")
        val longitude: Float?,
        @Column(name = "latitude")
        val latitude: Float?,
        @Column(name = "altitude")
        val altitude: Float?
    )

    @Embeddable
    data class Street(
        @Column(name = "phy_street_name")
        val name: String?,
        @Column(name = "phy_house_number")
        val houseNumber: String?,
        @Column(name = "phy_house_number_supplement")
        val houseNumberSupplement: String?,
        @Column(name = "phy_milestone")
        val milestone: String?,
        @Column(name = "phy_direction")
        val direction: String?,
        @Column(name = "phy_street_name_prefix")
        val namePrefix: String?,
        @Column(name = "phy_street_name_additional_prefix")
        val additionalNamePrefix: String?,
        @Column(name = "phy_street_name_suffix")
        val nameSuffix: String?,
        @Column(name = "phy_street_name_additional_suffix")
        val additionalNameSuffix: String?,
    )
}