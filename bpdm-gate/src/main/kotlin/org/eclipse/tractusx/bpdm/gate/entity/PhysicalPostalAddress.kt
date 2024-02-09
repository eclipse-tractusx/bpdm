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

package org.eclipse.tractusx.bpdm.gate.entity

import com.neovisionaries.i18n.CountryCode
import jakarta.persistence.*

@Embeddable
data class PhysicalPostalAddress(

    @Embedded
    @AttributeOverride(name = "latitude", column = Column(name = "phy_latitude"))
    @AttributeOverride(name = "longitude", column = Column(name = "phy_longitude"))
    @AttributeOverride(name = "altitude", column = Column(name = "phy_altitude"))
    val geographicCoordinates: GeographicCoordinate?,

    @Column(name = "phy_country")
    @Enumerated(EnumType.STRING)
    val country: CountryCode?,

    /**
     * Region within the country
     */
    @Column(name = "phy_admin_area_l1_region")
    val administrativeAreaLevel1: String?,

    /**
     * Further possibility to describe the region/address(e.g. County)
     */
    @Column(name = "phy_admin_area_l2")
    val administrativeAreaLevel2: String?,

    /**
     * Further possibility to describe the region/address(e.g. Township)
     */
    @Column(name = "phy_admin_area_l3")
    val administrativeAreaLevel3: String?,

    /**
     * A postal code, also known as postcode, PIN or ZIP Code
     */
    @Column(name = "phy_postcode")
    val postalCode: String?,

    /**
     * The city of the address (Synonym: Town, village, municipality)
     */
    @Column(name = "phy_city")
    val city: String?,

    /**
     * Divides the city in several smaller areas
     */
    @Column(name = "phy_district_l1")
    val district: String?,

    @Embedded
    @AttributeOverride(name = "name", column = Column(name = "phy_street_name"))
    @AttributeOverride(name = "houseNumber", column = Column(name = "phy_street_number"))
    @AttributeOverride(name = "houseNumberSupplement", column = Column(name = "phy_street_number_supplement"))
    @AttributeOverride(name = "milestone", column = Column(name = "phy_street_milestone"))
    @AttributeOverride(name = "direction", column = Column(name = "phy_street_direction"))
    @AttributeOverride(name = "namePrefix", column = Column(name = "phy_name_prefix"))
    @AttributeOverride(name = "additionalNamePrefix", column = Column(name = "phy_additional_name_prefix"))
    @AttributeOverride(name = "nameSuffix", column = Column(name = "phy_name_suffix"))
    @AttributeOverride(name = "additionalNameSuffix", column = Column(name = "phy_additional_name_suffix"))
    val street: Street?,

    // specific for PhysicalPostalAddress

    /**
     * A separate postal code for a company, also known as postcode, PIN or ZIP Code
     */
    @Column(name = "phy_company_postcode")
    val companyPostalCode: String?,

    /**
     * The practice of designating an area for industrial development
     */
    @Column(name = "phy_industrial_zone")
    val industrialZone: String?,

    /**
     * Describes a specific building within the address
     */
    @Column(name = "phy_building")
    val building: String?,

    /**
     * Describes the floor/level the delivery shall take place
     */
    @Column(name = "phy_floor")
    val floor: String?,

    /**
     * Describes the door/room/suite on the respective floor the delivery shall take place
     */
    @Column(name = "phy_door")
    val door: String?
)
