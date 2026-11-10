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
 * SPDX-License-IdentifierV7Dto: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType

@Schema(description = "The business state information for the corresponding business partner")
data class BusinessStateV7Dto(
    @get:Schema(description = "Date since when the status is/was valid.")
    val validFrom: Instant?,
    @get:Schema(description = "Date until the status was valid, if applicable.")
    val validTo: Instant?,
    @get:Schema(description = "The type of this specified status.")
    val type: BusinessStateType?
)
