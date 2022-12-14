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

package org.eclipse.tractusx.bpdm.common.dto

import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.model.NameType

@Schema(name = "Name", description = "Name record for a business partner")
data class NameDto(
    @Schema(description = "Full name")
    val value: String,
    @Schema(description = "Abbreviated name or shorthand")
    val shortName: String?,
    @Schema(description = "Type of specified name", defaultValue = "OTHER")
    val type: NameType = NameType.OTHER,
    @Schema(description = "Language in which the name is specified", defaultValue = "undefined")
    val language: LanguageCode = LanguageCode.undefined
)