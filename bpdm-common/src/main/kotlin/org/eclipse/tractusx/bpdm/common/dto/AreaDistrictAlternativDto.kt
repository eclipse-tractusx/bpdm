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

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "AreaDistrictDto", description = "Record for administrativeAreaLevel and district part of an alternativ address")
data class AreaDistrictAlternativDto(

    @get:Schema(description = "Identifying code of the Region within the country (e.g. Bayern)")
    val administrativeAreaLevel1: String? = null,
    
    @get:Schema(description = "Divides the city in several smaller areas")
    val districtLevel1: String? = null,

    @get:Schema(description = "Divides the DistrictLevel1 in several smaller areas. Synonym: Subdistrict")
    val districtLevel2: String? = null,

    )
