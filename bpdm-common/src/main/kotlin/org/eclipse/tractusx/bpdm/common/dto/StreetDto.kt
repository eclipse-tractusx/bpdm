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

@Schema(name = "Name", description = "A public road in a city, town, or village, typically with houses and buildings on one or both sides.")
data class StreetDto(
    @Schema(description = "Describes the official Name of the Street.")
    val name: String? = null,

    @Schema(description = "Describes the House Number")
    val houseNumber: String? = null,

    @Schema(description = "The Milestone is relevant for long roads without specific house numbers.")
    val milestone: String? = null,

    @Schema(description = "Describes the direction")
    val direction: String?  = null
)