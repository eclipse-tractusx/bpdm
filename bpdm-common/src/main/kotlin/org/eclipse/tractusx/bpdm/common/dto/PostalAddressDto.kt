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
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto

@Schema(name = "PostalAdress", description = "Address record for a business partner")
data class PostalAddressDto(

    @Schema(description = "Geographic coordinates to find this location")
    val geographicCoordinates: GeoCoordinateDto? = null,

    @Schema(description = "Describes the country")
    val country: CountryCode,

    @Schema(description = "Identifying code of the Region within the country (e.g. Bayern)")
    val administrativeAreaLevel1: String? = null,

    @Schema(description = "Further possibility to describe the region/address(e.g. County/Landkreis)")
    val administrativeAreaLevel2: String? = null,

    @Schema(description = "Further possibility to describe the region/address(e.g. Township/Gemeinde)")
    val administrativeAreaLevel3: String? = null,

    @Schema(description = "Further possibility to describe the region/address(e.g. Sub-Province for China/Japan)")
    val administrativeAreaLevel4: String? = null,

    @Schema(description ="A postal code, also known as postcode, PIN or ZIP Code")
    val postCode: String? = null,

    @Schema(description =  "The city of the address (Synonym: Town, village, municipality)")
    val city: String,

    @Schema(description =  "Divides the city in several smaller areas")
    val districtLevel1: String? = null,

    @Schema(description =  "Divides the DistrictLevel1 in several smaller areas. Synonym: Subdistrict")
    val districtLevel2: String? = null,

    @Schema(description = "Address country")
    val street: StreetDto? = null,

    val physicalAddress: PhysicalPostalAddressDto = PhysicalPostalAddressDto(),

    val alternativeAddress: AlternativePostalAddressDto? = AlternativePostalAddressDto()
)