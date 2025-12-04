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

package org.eclipse.tractusx.bpdm.pool.api.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.neovisionaries.i18n.CountryCode
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.IBasePhysicalPostalAddressDto
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.PostalAddressDescription

@Schema(description = PostalAddressDescription.headerPhysical)
data class PhysicalPostalAddressVerboseDto(

    override val geographicCoordinates: GeoCoordinateDto?,

    @field:JsonProperty("country")
    @get:Schema(description = PostalAddressDescription.country)
    val countryVerbose: TypeKeyNameVerboseDto<CountryCode>,

    @field:JsonProperty("administrativeAreaLevel1")
    @get:Schema(description = PostalAddressDescription.administrativeAreaLevel1)
    val administrativeAreaLevel1Verbose: RegionDto?,

    override val administrativeAreaLevel2: String?,
    override val administrativeAreaLevel3: String?,
    override val postalCode: String?,
    override val city: String,
    override val district: String?,
    override val street: StreetDto?,
    override val companyPostalCode: String?,
    override val industrialZone: String?,
    override val building: String?,
    override val floor: String?,
    override val door: String?,
    override val taxJurisdictionCode: String?

) : IBasePhysicalPostalAddressDto {

    @get:JsonIgnore
    override val country: CountryCode
        get() = countryVerbose.technicalKey

    @get:JsonIgnore
    override val administrativeAreaLevel1: String?
        get() = administrativeAreaLevel1Verbose?.regionCode
}
