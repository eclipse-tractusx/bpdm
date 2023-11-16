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
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.PostalAddressDescription
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType

/**
 * Contains the common fields for all AlternativePostalAddress-DTOs, whether Pool or Gate.
 */
interface IBaseAlternativePostalAddressDto {

    // OpenAPI description for complex field does not work!
    val geographicCoordinates: GeoCoordinateDto?

    @get:Schema(description = PostalAddressDescription.country)
    val country: Any?

    @get:Schema(description = PostalAddressDescription.administrativeAreaLevel1)
    val administrativeAreaLevel1: Any?

    @get:Schema(description = PostalAddressDescription.postalCode)
    val postalCode: String?

    @get:Schema(description = PostalAddressDescription.city)
    val city: String?

    @get:Schema(description = PostalAddressDescription.deliveryServiceType)
    val deliveryServiceType: DeliveryServiceType?

    @get:Schema(description = PostalAddressDescription.deliveryServiceQualifier)
    val deliveryServiceQualifier: String?

    @get:Schema(description = PostalAddressDescription.deliveryServiceNumber)
    val deliveryServiceNumber: String?

    fun adminLevel1Key(): String?

    fun countryCode(): CountryCode?
}