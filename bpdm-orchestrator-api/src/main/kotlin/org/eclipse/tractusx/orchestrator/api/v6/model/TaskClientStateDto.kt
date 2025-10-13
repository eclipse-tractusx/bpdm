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
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.orchestrator.api.v6.model

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.orchestrator.api.model.TaskProcessingStateDto

@Schema(description = "The golden record task's processing state together with optional business partner data in case processing is done")
data class TaskClientStateDto(

    @get:Schema(required = true)
    val taskId: String,

    @get:Schema(required = true, description = "The identifier of the gate record for which this task has been created")
    val recordId: String,

    val businessPartnerResult: BusinessPartner,

    @get:Schema(required = true)
    val processingState: TaskProcessingStateDto
)
