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

package org.eclipse.tractusx.bpdm.pool.api.model.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.AlternativePostalAddressVerboseDto
import org.eclipse.tractusx.bpdm.common.dto.response.PhysicalPostalAddressVerboseDto
import java.time.Instant

@Schema(name = "MainAddressResponse", description = "Main address for site")
data class MainAddressResponse(

    @Schema(description = "Physical postal address")
    val physicalPostalAddress: PhysicalPostalAddressVerboseDto,

    @Schema(description = "Alternative postal address")
    val alternativePostalAddress: AlternativePostalAddressVerboseDto? = null,

    @Schema(description = "BPN of the related site")
    val bpnSite: String,

    @Schema(description = "The timestamp the business partner data was created")
    val createdAt: Instant,

    @Schema(description = "The timestamp the business partner data was last updated")
    val updatedAt: Instant
)