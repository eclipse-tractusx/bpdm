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

package org.eclipse.tractusx.bpdm.pool.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded

@Embeddable
data class PhysicalAddressScriptVariantDb(
    @Column(name = "phy_city", nullable = false)
    val city: String,
    @Column(name = "phy_district_l1")
    val district: String?,

    @Embedded
    val street: StreetScriptVariantDb?,

    @Column(name = "phy_industrial_zone")
    val industrialZone: String?,

    @Column(name = "phy_building")
    val building: String?,

    @Column(name = "phy_floor")
    val floor: String?,

    @Column(name = "phy_door")
    val door: String?
)
