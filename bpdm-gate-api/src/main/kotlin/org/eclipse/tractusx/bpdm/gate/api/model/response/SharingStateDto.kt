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

package org.eclipse.tractusx.bpdm.gate.api.model.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.LsaType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import java.time.LocalDateTime

@Schema(
    name = "SharingState"
)
data class SharingStateDto(
    @get:Schema(description = "LSA Type")
    val lsaType: LsaType,

    @get:Schema(description = "External identifier")
    val externalId: String,

    @get:Schema(description = "Type of sharing state")
    val sharingStateType: SharingStateType,

    @get:Schema(description = "Sharing error code (for error)")
    val sharingErrorCode: BusinessPartnerSharingError? = null,

    @get:Schema(description = "Sharing error message (for error)")
    val sharingErrorMessage: String? = null,

    @get:Schema(description = "BPN (for success)")
    val bpn: String? = null,

    @get:Schema(description = "Sharing process started (not updated if null)")
    val sharingProcessStarted: LocalDateTime? = null
)
