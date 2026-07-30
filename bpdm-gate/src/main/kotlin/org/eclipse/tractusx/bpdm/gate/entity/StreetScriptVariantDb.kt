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

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class StreetScriptVariantDb(
    @Column(name = "phy_street_name")
    val name: String?,

    @Column(name = "phy_street_direction")
    val direction: String?,

    @Column(name = "phy_name_prefix")
    val namePrefix: String?,

    @Column(name = "phy_additional_name_prefix")
    val additionalNamePrefix: String?,

    @Column(name = "phy_name_suffix")
    val nameSuffix: String?,

    @Column(name = "phy_additional_name_suffix")
    val additionalNameSuffix: String?
)
