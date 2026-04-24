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
    @Column(name = "phy_postcode")
    val postalCode: String?,
    @Column(name = "phy_city")
    val city: String?,
    @Column(name = "phy_district_l1")
    val district: String?,
    @Embedded
    val street: PostalAddressDb.Street,
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
data class AlternativeAddressScriptVariantDb(
    @Column(name = "alt_postcode")
    val postalCode: String?,
    @Column(name = "alt_city")
    val city: String?,
    @Column(name = "alt_delivery_service_qualifier")
    val deliveryServiceQualifier: String?,
    @Column(name = "alt_delivery_service_number")
    val deliveryServiceNumber: String?
)