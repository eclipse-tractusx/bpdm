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

package org.eclipse.tractusx.orchestrator.api.model

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.IBaseSiteDto

data class SiteDto(

    @get:Schema(description = "A reference to the BPNS of this site. Either by the BPN value itself or a BPN request identifier.")
    val bpnSReference: BpnReferenceDto? = null,

    @get:Schema(description = "Whether this site data is different from its golden record counterpart in the Pool")
    val hasChanged: Boolean? = null,

    override val name: String? = null,
    override val states: Collection<SiteStateDto> = emptyList(),

    val mainAddress: LogisticAddressDto? = null,

    override val confidenceCriteria: ConfidenceCriteria = ConfidenceCriteria()

) : IBaseSiteDto
