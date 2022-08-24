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
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeNameUrlDto

@Schema(name = "Legal Form Response", description = "Legal form a business partner can have")
data class LegalFormResponse(
    @Schema(description = "Unique key to be used for reference")
    val technicalKey: String,
    @Schema(description = "Full name of the legal form")
    val name: String?,
    @Schema(description = "Link for further information on the legal form")
    val url: String? = null,
    @Schema(description = "Abbreviation of the legal form name")
    val mainAbbreviation: String? = null,
    @Schema(description = "Language in which the legal form is specified")
    val language: TypeKeyNameDto<LanguageCode>,
    @Schema(description = "Categories in which this legal form falls under")
    val categories: Collection<TypeNameUrlDto> = emptyList()
)