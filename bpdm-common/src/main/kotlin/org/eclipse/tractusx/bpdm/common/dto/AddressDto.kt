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

import com.neovisionaries.i18n.CountryCode
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.model.AddressType

@Schema(name = "Address", description = "Localized address record for a business partner")
data class AddressDto(
    @Schema(description = "Language and character set the address is written in")
    val version: AddressVersionDto = AddressVersionDto(),
    @Schema(description = "Entity which is in care of this address")
    val careOf: String? = null,
    @Schema(description = "Contexts of this address")
    val contexts: Collection<String> = emptyList(),
    @Schema(description = "Address country", defaultValue = "UNDEFINED")
    val country: CountryCode = CountryCode.UNDEFINED,
    @ArraySchema(arraySchema = Schema(description = "Area such as country region or county", defaultValue = "[]"))
    val administrativeAreas: Collection<AdministrativeAreaDto> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Postcodes assigned to this address", defaultValue = "[]"))
    val postCodes: Collection<PostCodeDto> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "City, block and/or quarter", defaultValue = "[]"))
    val localities: Collection<LocalityDto> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Street, zone and/or square", defaultValue = "[]"))
    val thoroughfares: Collection<ThoroughfareDto> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Building, level and/or room", defaultValue = "[]"))
    val premises: Collection<PremiseDto> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Postal delivery points", defaultValue = "[]"))
    val postalDeliveryPoints: Collection<PostalDeliveryPointDto> = emptyList(),
    @Schema(description = "Geographic coordinates to find this location")
    val geographicCoordinates: GeoCoordinateDto? = null,
    @ArraySchema(arraySchema = Schema(description = "Type of address", defaultValue = "[]"))
    val types: Collection<AddressType> = emptyList()
)