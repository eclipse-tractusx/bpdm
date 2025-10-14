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

package org.eclipse.tractusx.orchestrator.api.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Contains detailed information about the current processing state of a golden record task")
data class TaskProcessingStateDto(

    @get:Schema(description = "The processing result of the task, can also still be pending", required = true)
    val resultState: ResultState,

    @get:Schema(description = "The last step this task has entered", required = true)
    val step: TaskStep,

    @get:Schema(description = "Whether the task is queued or already reserved for the latest step", required = true)
    val stepState: StepState,

    @get:Schema(
        description = "The actual errors that happened during processing if the task has an error result state. " +
                "The errors refer to the latest step.",
        required = true
    )
    val errors: List<TaskErrorDto> = emptyList(),

    @get:Schema(description = "When the task has been created", required = true)
    val createdAt: Instant,

    @get:Schema(description = "When the task has last been modified", required = true)
    val modifiedAt: Instant,

    @get:Schema(description = "The timestamp until the task is removed from the Orchestrator", deprecated = true)
    val timeout: Instant
)
