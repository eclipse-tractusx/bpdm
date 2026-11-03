/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner

import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetV7Dto

data class BusinessPartnerSearchPhysicalAddressV7Dto(
    val geographicCoordinates: GeoCoordinateDto? = null,
    val country: String? = null,
    val administrativeAreaLevel1: String? = null,
    val administrativeAreaLevel2: String? = null,
    val administrativeAreaLevel3: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val district: String? = null,
    val street: StreetV7Dto? = null,
    val companyPostalCode: String? = null,
    val industrialZone: String? = null,
    val building: String? = null,
    val floor: String? = null,
    val door: String? = null,
    val taxJurisdictionCode: String? = null
)
