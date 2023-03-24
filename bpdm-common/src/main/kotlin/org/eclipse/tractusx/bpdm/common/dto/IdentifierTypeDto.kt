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

@Schema(name = "IdentifierTypeDto", description = "Identifier type definition for legal entity or address")
data class IdentifierTypeDto(
    @Schema(description = "Unique key (in combination with lsaType) to be used as reference")
    val technicalKey: String,

    @Schema(description = "Specifies if this identifier type is valid for legal entities (L) or addresses (A)")
    val lsaType: IdentifierLsaType,

    @Schema(description = "Full name")
    val name: String,

    @Schema(description = "Validity details")
    val details: Collection<IdentifierTypeDetailDto> = listOf()
)
