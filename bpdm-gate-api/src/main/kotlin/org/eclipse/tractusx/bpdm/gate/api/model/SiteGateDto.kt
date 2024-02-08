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

package org.eclipse.tractusx.bpdm.gate.api.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.SiteDescription

// TODO: This DTO does not implement IBaseSiteDto because it is missing a [name] property.
//  Removing this from IBaseSiteDto has lots of implications. And this DTO is only used in a deprecated API anyways!
@Schema(description = SiteDescription.header)
data class SiteGateDto(

    @get:ArraySchema(arraySchema = Schema(description = SiteDescription.nameParts))
    val nameParts: Collection<String> = emptyList(),

    @get:ArraySchema(arraySchema = Schema(description = SiteDescription.states))
    val states: Collection<SiteStateDto> = emptyList(),

    @get:ArraySchema(arraySchema = Schema(description = SiteDescription.roles))
    val roles: Collection<BusinessPartnerRole> = emptyList(),
)
