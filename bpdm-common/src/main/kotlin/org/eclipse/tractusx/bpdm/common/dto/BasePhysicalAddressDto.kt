/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.common.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "BasePhysicalAddressDto", description = "Address record for the basical physical address fields")
data class BasePhysicalAddressDto(

    @get:Schema(description = "A separate postal code for a company, also known as postcode, PIN or ZIP Code")
    val companyPostalCode: String? = null,

    @get:Schema(description = "The practice of designating an area for industrial development")
    val industrialZone: String? = null,

    @get:Schema(description = "Describes a specific building within the address")
    val building: String? = null,

    @get:Schema(description = "Describes the floor/level the delivery shall take place")
    val floor: String? = null,

    @get:Schema(description = "Describes the  door/room/suite on the respective floor the delivery shall take place")
    val door: String? = null,
)