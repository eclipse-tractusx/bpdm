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

package org.eclipse.tractusx.bpdm.pool.api.v6.model.request

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "LegalFormRequest", description = "New legal form record to be referenced by business partners")
data class LegalFormRequest(
    @Schema(description = "Unique key to be used for reference")
    val technicalKey: String,

    @Schema(description = "Full name of the legal form")
    val name: String,

    @Schema(description = "Transliterated name of the legal form")
    val transliteratedName: String?,

    @Schema(description = "Comma separed list of abbreviations of the legal form name")
    val abbreviation: String?,

    @Schema(description = "Transliterated abbreviations of the legal form abbreviations")
    val transliteratedAbbreviations: String?,

    @Schema(description = "The country to which this legal form belongs to")
    val country: CountryCode?,

    @Schema(description = "The language of the legal form's name")
    val language: LanguageCode?,

    @Schema(description = "The administrative area level 1 this legal form belongs to")
    val administrativeAreaLevel1: String?,

    @Schema(description = "Whether this legal form is considered as active according to GLEIF")
    val isActive: Boolean
)
