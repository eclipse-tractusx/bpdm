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

package org.eclipse.tractusx.bpdm.gate.entity

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded

@Embeddable
data class PhysicalPostalAddressScriptVariantDb (
    @Column(name = "phy_postcode")
    val postalCode: String?,
    @Column(name = "phy_city")
    val city: String?,
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
    val street: StreetDb?,

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
    val taxJurisdictionCode: String? = null
)