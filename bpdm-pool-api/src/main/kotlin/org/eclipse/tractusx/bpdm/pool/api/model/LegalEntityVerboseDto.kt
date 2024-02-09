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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.IBaseLegalEntityDto
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.CommonDescription
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.LegalEntityDescription
import java.time.Instant

@Schema(description = LegalEntityDescription.header)
data class LegalEntityVerboseDto(

    @get:Schema(description = LegalEntityDescription.bpnl)
    val bpnl: String,

    @get:Schema(description = LegalEntityDescription.legalName)
    val legalName: String,

    override val legalShortName: String? = null,

    @field:JsonProperty("legalForm")
    @get:Schema(description = LegalEntityDescription.legalForm)
    val legalFormVerbose: LegalFormDto? = null,

    override val identifiers: Collection<LegalEntityIdentifierVerboseDto> = emptyList(),
    override val states: Collection<LegalEntityStateVerboseDto> = emptyList(),
    override val classifications: Collection<LegalEntityClassificationVerboseDto> = emptyList(),

    @get:ArraySchema(arraySchema = Schema(description = LegalEntityDescription.relations))
    val relations: Collection<RelationVerboseDto> = emptyList(),

    @get:Schema(description = LegalEntityDescription.currentness)
    val currentness: Instant,

    override val confidenceCriteria: ConfidenceCriteriaDto,

    @get:Schema(description = CommonDescription.createdAt)
    val createdAt: Instant,

    @get:Schema(description = CommonDescription.updatedAt)
    val updatedAt: Instant,

) : IBaseLegalEntityDto {

    @get:JsonIgnore
    override val legalForm: String?
        get() = legalFormVerbose?.technicalKey
}
