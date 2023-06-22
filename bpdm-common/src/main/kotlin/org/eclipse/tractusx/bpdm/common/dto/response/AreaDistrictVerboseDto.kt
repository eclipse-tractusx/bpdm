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

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.NameRegioncodeVerboseDto

@Schema(name = "AreaDistrictVerboseDto", description = "Record for administrativeAreaLevel and district part of an address")
data class AreaDistrictVerboseDto(

    @get:Schema(description = "Region within the country")
    val administrativeAreaLevel1: NameRegioncodeVerboseDto? = null,

    @get:Schema(description = "Further possibility to describe the region/address(e.g. County/Landkreis)")
    val administrativeAreaLevel2: String? = null,

    @get:Schema(description = "Further possibility to describe the region/address(e.g. Township/Gemeinde)")
    val administrativeAreaLevel3: String? = null,

    @get:Schema(description = "Divides the city in several smaller areas")
    val district: String? = null,
)
