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
 * SPDX-License-IdentifierV7Dto: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.PostalAddressDescription

@Schema(description = PostalAddressDescription.headerPhysical)
data class PhysicalAddressV7Dto(
    val geographicCoordinates: GeoCoordinateV7Dto,
    @Schema(description = PostalAddressDescription.country)
    val country: String?,
    @Schema(description = PostalAddressDescription.administrativeAreaLevel1)
    val administrativeAreaLevel1: String?,
    @Schema(description = PostalAddressDescription.administrativeAreaLevel2)
    val administrativeAreaLevel2: String?,
    @Schema(description = PostalAddressDescription.administrativeAreaLevel3)
    val administrativeAreaLevel3: String?,
    @Schema(description = PostalAddressDescription.postalCode)
    val postalCode: String?,
    @Schema(description = PostalAddressDescription.city)
    val city: String?,
    @Schema(description = PostalAddressDescription.district)
    val district: String?,
    val street: StreetV7Dto,
    @Schema(description = PostalAddressDescription.companyPostalCode)
    val companyPostalCode: String?,
    @Schema(description = PostalAddressDescription.industrialZone)
    val industrialZone: String?,
    @Schema(description = PostalAddressDescription.building)
    val building: String?,
    @Schema(description = PostalAddressDescription.floor)
    val floor: String?,
    @Schema(description = PostalAddressDescription.door)
    val door: String?,
    @Schema(description = PostalAddressDescription.taxJurisdictionCode)
    val taxJurisdictionCode: String?
){
    companion object {
        val empty: PhysicalAddressV7Dto = PhysicalAddressV7Dto(
            GeoCoordinateV7Dto.empty, null, null, null,
            null, null, null, null, StreetV7Dto.empty, null, null,
            null, null, null, null
        )
    }
}
