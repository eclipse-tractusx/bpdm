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

package org.eclipse.tractusx.bpdm.gate.api.v6.model.request

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.gate.api.model.IRelationDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType

@Schema(description = "A request to create a new relation between two business partners")
data class RelationPostRequest(
    @Schema(description = "The identifier under which the relation will be referenced. " +
            "If not given a unique identifier will be automatically assigned by the system. " +
            "A given external identifier needs be unique.")
    override val externalId: String? = null,
    @Schema(description = "The type of relation that should be created")
    override val relationType: RelationType,
    @Schema(description = "The external identifier of the business partner from which the relation should emerge (the source)")
    override val businessPartnerSourceExternalId: String,
    @Schema(description = "The external identifier of the business partner to which the relation should point (the target)")
    override val businessPartnerTargetExternalId: String
): IRelationDto