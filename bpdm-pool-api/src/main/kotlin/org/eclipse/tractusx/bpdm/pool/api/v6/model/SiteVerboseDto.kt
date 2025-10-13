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

package org.eclipse.tractusx.bpdm.pool.api.v6.model

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.IBaseSiteDto
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.CommonDescription
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.SiteDescription
import org.eclipse.tractusx.bpdm.pool.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteStateVerboseDto
import java.time.Instant

@Schema(description = SiteDescription.header)
data class SiteVerboseDto(

    @get:Schema(description = SiteDescription.bpns)
    val bpns: String,

    override val name: String,
    override val states: Collection<SiteStateVerboseDto> = emptyList(),

    @get:Schema(description = "Indicates whether the site is owned and thus provided by a Catena-X Member.")
    val isCatenaXMemberData: Boolean,

    @get:Schema(description = SiteDescription.bpnLegalEntity)
    val bpnLegalEntity: String,

    @get:Schema(description = CommonDescription.createdAt)
    val createdAt: Instant,

    @get:Schema(description = CommonDescription.updatedAt)
    val updatedAt: Instant,

    override val confidenceCriteria: ConfidenceCriteriaDto

) : IBaseSiteDto
