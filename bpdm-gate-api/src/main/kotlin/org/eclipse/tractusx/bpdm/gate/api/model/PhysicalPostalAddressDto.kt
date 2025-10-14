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

package org.eclipse.tractusx.bpdm.gate.api.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.neovisionaries.i18n.CountryCode
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.IBasePhysicalPostalAddressDto
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.PostalAddressDescription
import org.eclipse.tractusx.bpdm.common.service.DataClassUnwrappedJsonDeserializer

@JsonDeserialize(using = DataClassUnwrappedJsonDeserializer::class)
@Schema(description = PostalAddressDescription.headerPhysical)
data class PhysicalPostalAddressDto(

    override val geographicCoordinates: GeoCoordinateDto? = null,
    override val country: CountryCode? = null,
    override val administrativeAreaLevel1: String? = null,
    override val administrativeAreaLevel2: String? = null,
    override val administrativeAreaLevel3: String? = null,
    override val postalCode: String? = null,
    override val city: String? = null,
    override val district: String? = null,
    override val street: StreetDto? = null,
    override val companyPostalCode: String? = null,
    override val industrialZone: String? = null,
    override val building: String? = null,
    override val floor: String? = null,
    override val door: String? = null,
    override val taxJurisdictionCode: String? = null

) : IBasePhysicalPostalAddressDto
