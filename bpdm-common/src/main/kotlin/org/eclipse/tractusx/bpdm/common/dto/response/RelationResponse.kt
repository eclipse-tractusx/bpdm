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

package org.eclipse.tractusx.bpdm.common.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.model.RelationClass
import org.eclipse.tractusx.bpdm.common.model.RelationType
import java.time.LocalDateTime

@Schema(name = "Relation Response", description = "Directed relation between two business partners")
data class RelationResponse (
    @Schema(description = "Class of relation like Catena, LEI or DNB relation")
    val relationClass: TypeKeyNameDto<RelationClass>,
    @Schema(description = "Type of relation like predecessor or ownership relation")
    val type: TypeKeyNameDto<RelationType>,
    @Schema(description = "BPN of partner which is the source of the relation")
    val startNode: String,
    @Schema(description = "BPN of partner which is the target of the relation")
    val endNode: String,
    @Schema(description = "Time when the relation started")
    val startedAt: LocalDateTime? = null,
    @Schema(description = "Time when the relation ended")
    val endedAt: LocalDateTime? = null
)