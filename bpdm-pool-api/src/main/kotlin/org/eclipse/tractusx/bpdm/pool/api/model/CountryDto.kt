/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a country accepted by the Pool")
data class CountryDto(
    @Schema(description = "The country code")
    val countryCode: String,
    @Schema(description = "The name of the country")
    val name: String,
    @Schema(description = "Description of the assignment status when the code is not officially assigned under ISO 3166-1")
    val description: String?
)
