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

package org.eclipse.tractusx.bpdm.common.dto.response

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.neovisionaries.i18n.CountryCode
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.IBaseAlternativePostalAddressDto
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.PostalAddressDescription
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.common.service.DataClassUnwrappedJsonDeserializer

@JsonDeserialize(using = DataClassUnwrappedJsonDeserializer::class)
@Schema(description = PostalAddressDescription.headerAlternative)
data class AlternativePostalAddressVerboseDto(

    override val geographicCoordinates: GeoCoordinateDto?,

    // OpenAPI description for complex field does not work!
    override val country: TypeKeyNameVerboseDto<CountryCode>,

    // OpenAPI description for complex field does not work!
    override val administrativeAreaLevel1: RegionDto?,

    override val postalCode: String?,

    override val city: String,

    override val deliveryServiceType: DeliveryServiceType,

    override val deliveryServiceQualifier: String?,

    override val deliveryServiceNumber: String

) : IBaseAlternativePostalAddressDto {
    override fun adminLevel1Key(): String? {
        return administrativeAreaLevel1?.regionCode
    }
    override fun countryCode(): CountryCode {
        return country.technicalKey
    }
}
