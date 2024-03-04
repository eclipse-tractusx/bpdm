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

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerbose
import org.eclipse.tractusx.bpdm.common.model.RelationType
import java.time.LocalDateTime

@Schema(name = "RelationVerbose", description = "Directed relation between two business partners")
data class RelationVerbose(

    @get:Schema(description = "Type of relation like predecessor or ownership relation")
    val type: TypeKeyNameVerbose<RelationType>,

    @get:Schema(description = "BPN of partner which is the source of the relation")
    val startBpnl: String,

    @get:Schema(description = "BPN of partner which is the target of the relation")
    val endBpnl: String,

    @get:Schema(description = "Time when the relation started")
    val validFrom: LocalDateTime? = null,

    @get:Schema(description = "Time when the relation ended")
    val validTo: LocalDateTime? = null
)