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

package org.eclipse.tractusx.bpdm.pool.api.model

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.LegalFormDescription

@Schema(description = LegalFormDescription.header)
data class LegalFormDto(

    @get:Schema(description = LegalFormDescription.technicalKey)
    val technicalKey: String,

    @get:Schema(description = LegalFormDescription.name)
    val name: String,

    val transliteratedName: String?,

    @get:Schema(description = LegalFormDescription.abbreviation)
    val abbreviations: String? = null,

    val country: CountryCode?,

    val language: LanguageCode?,

    val administrativeAreaLevel1: String?,

    val transliteratedAbbreviations: String?,

    val isActive: Boolean
)
