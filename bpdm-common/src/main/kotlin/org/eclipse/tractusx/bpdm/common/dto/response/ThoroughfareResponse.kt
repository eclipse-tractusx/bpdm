/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.model.ThoroughfareType

@Schema(name = "Thoroughfare Response", description = "Thoroughfare record of an address such as street, square or industrial zone")
data class ThoroughfareResponse(
    @Schema(description = "Full denotation of the thoroughfare")
    val value: String,
    @Schema(description = "Full name of the thoroughfare")
    val name: String? = null,
    @Schema(description = "Abbreviation or shorthand")
    val shortName: String? = null,
    @Schema(description = "Thoroughfare number")
    val number: String? = null,
    @Schema(description = "Direction information on the thoroughfare")
    val direction: String? = null,
    @Schema(description = "Type of thoroughfare", defaultValue = "OTHER")
    var type: TypeKeyNameUrlDto<ThoroughfareType>,
    @Schema(description = "Language the thoroughfare is specified in")
    var language: TypeKeyNameDto<LanguageCode>
)