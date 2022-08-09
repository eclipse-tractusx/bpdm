/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.common.dto.response

import com.neovisionaries.i18n.CountryCode
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.model.AddressType

@Schema(name = "Address Response", description = "Localized address record of a business partner")
data class AddressResponse(
    @Schema(description = "Language and character set the address is written in")
    val version: AddressVersionResponse,
    @Schema(description = "Entity which is in care of this address")
    val careOf: String? = null,
    @Schema(description = "Contexts of this address")
    val contexts: Collection<String> = emptyList(),
    @Schema(description = "Address country")
    val country: TypeKeyNameDto<CountryCode>,
    @ArraySchema(arraySchema = Schema(description = "Areas such as country region and county"))
    val administrativeAreas: Collection<AdministrativeAreaResponse> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Postcodes assigned to this address"))
    val postCodes: Collection<PostCodeResponse> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Localities such as city, block and quarter"))
    val localities: Collection<LocalityResponse> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Thoroughfares such as street, zone and square"))
    val thoroughfares: Collection<ThoroughfareResponse> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Premises such as building, level and room"))
    val premises: Collection<PremiseResponse> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Delivery points for post"))
    val postalDeliveryPoints: Collection<PostalDeliveryPointResponse> = emptyList(),
    @Schema(description = "Geographic coordinates to find this location")
    val geographicCoordinates: GeoCoordinateDto? = null,
    @ArraySchema(arraySchema = Schema(description = "Types of this address"))
    val types: Collection<TypeKeyNameUrlDto<AddressType>> = emptyList()
)