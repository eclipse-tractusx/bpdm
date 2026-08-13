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

package org.eclipse.tractusx.bpdm.orchestrator.entity

import jakarta.persistence.*
import org.hibernate.annotations.Formula

@Embeddable
data class PostalAddressScriptVariantDb (
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    val scope: PostalAddressDb.Scope,
    @Column(name = "script_code", nullable = false)
    val scriptCode: String,
    @Column(name = "address_name")
    val addressName: String?,
    @Embedded
    var physicalAddress: PhysicalAddressScriptVariantDb,
    @Embedded
    var alternativeAddress: AlternativeAddressScriptVariantDb?
)

@Embeddable
data class PhysicalAddressScriptVariantDb(
    @Column(name = "phy_city")
    val city: String?,
    @Column(name = "phy_district_l1")
    val district: String?,
    @Embedded
    val street: StreetScriptVariantDb,
    @Column(name = "phy_industrial_zone")
    val industrialZone: String?,
    @Column(name = "phy_building")
    val building: String?,
    @Column(name = "phy_floor")
    val floor: String?,
    @Column(name = "phy_door")
    val door: String?
)

@Embeddable
data class AlternativeAddressScriptVariantDb(
    @Column(name = "alt_city")
    val city: String?
){
    /**
     * Keeps Hibernate from reading an alternative variant whose only column is null as an absent variant.
     */
    @Formula("1")
    private val isNonNull = 1
}

@Embeddable
data class StreetScriptVariantDb(
    @Column(name = "phy_street_name")
    val name: String?,
    @Column(name = "phy_direction")
    val direction: String?,
    @Column(name = "phy_street_name_prefix")
    val namePrefix: String?,
    @Column(name = "phy_street_name_additional_prefix")
    val additionalNamePrefix: String?,
    @Column(name = "phy_street_name_suffix")
    val nameSuffix: String?,
    @Column(name = "phy_street_name_additional_suffix")
    val additionalNameSuffix: String?
){
    /**
     * Keeps Hibernate from reading an all-null street as an absent street, which the non-null property forbids.
     */
    @Formula("1")
    private val isNonNull = 1
}