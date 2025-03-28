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

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class RelationSharingStateDto(
    @get:Schema(description = "The external identifier of the business partner for which the sharing state entry was created.")
    val externalId: String,

    @get:Schema(description = "One of the sharing state types of the current sharing state.")
    val sharingStateType: RelationSharingStateType = RelationSharingStateType.Ready,

    @get:Schema(description = "One of the sharing error codes in case the current sharing state type is \"error\". \n" +
            "* `SharingProcessError`: A general error occurred during the sharing process.\n" +
            "* `SharingTimeout`: The processing took to long to complete.\n")
    val sharingErrorCode: RelationSharingStateErrorCode? = null,

    @get:Schema(description = "The error message in case the current sharing state type is \"error\".")
    val sharingErrorMessage: String? = null,

    @get:Schema(description = "When the sharing state last changed.")
    val taskId: String?,

    @get:Schema(description = "The orchestrator task identifier that was created")
    val updatedAt: Instant,


)
