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

package org.eclipse.tractusx.bpdm.gate.api.model.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.gate.api.exception.ErrorCode

@Schema(title = "ErrorInfo", description = "Holds information about failures")
data class ErrorInfo<out ERROR : ErrorCode>(

    @get:Schema(description = "Error code identifying the error")
    val errorCode: ERROR,

    @get:Schema(description = "Error message that explains the error")
    val message: String,

    @get:Schema(description = "Key (externalId) of the entity that failed")
    val entityKey: String?
)
