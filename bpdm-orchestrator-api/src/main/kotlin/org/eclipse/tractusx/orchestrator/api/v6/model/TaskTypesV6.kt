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
import org.eclipse.tractusx.bpdm.common.dto.IPageDto
import java.time.Instant

enum class TaskModeV6 {
    UpdateFromSharingMember,
    UpdateFromPool
}

enum class TaskStepV6 {
    CleanAndSync,
    PoolSync,
    Clean
}

enum class TaskErrorTypeV6 {
    Timeout,
    Unspecified,
    NaturalPersonError,
    BpnErrorNotFound,
    BpnErrorTooManyOptions,
    MandatoryFieldValidationFailed,
    BlacklistCountryPresent,
    UnknownSpecialCharacters
}

@Schema(description = "Describes an error that happened during processing of a task \n")
data class TaskErrorDtoV6(
    @get:Schema(description = "The type of error that occurred. \n" +
            "* `NaturalPersonError`: The provided record contains natural person information.\n" +
            "* `BpnErrorNotFound`: The provided record can not be matched to a legal entity or an address.\n" +
            "* `BpnErrorTooManyOptions`: The provided record can not link to a clear legal entity.\n" +
            "* `MandatoryFieldValidationFailed`: The provided record does not fulfill mandatory validation rules.\n" +
            "* `BlacklistCountryPresent`: The provided record is part of a country that is not allowed to be processed by the GR process (example: Brazil).\n" +
            "* `UnknownSpecialCharacters`: The provided record contains unallowed special characters.\n" , required = true)
    val type: TaskErrorTypeV6,
    @get:Schema(description = "The free text, detailed description of the error", required = true)
    val description: String
)

enum class ResultStateV6 {
    Pending,
    Success,
    Error
}

enum class StepStateV6 {
    Queued,
    Reserved,
    Success,
    Error
}

@Schema(description = "Contains detailed information about the current processing state of a golden record task")
data class TaskProcessingStateDtoV6(
    @get:Schema(description = "The processing result of the task, can also still be pending", required = true)
    val resultState: ResultStateV6,
    @get:Schema(description = "The last step this task has entered", required = true)
    val step: TaskStepV6,
    @get:Schema(description = "Whether the task is queued or already reserved for the latest step", required = true)
    val stepState: StepStateV6,
    @get:Schema(
        description = "The actual errors that happened during processing if the task has an error result state. " +
                "The errors refer to the latest step.",
        required = true
    )
    val errors: List<TaskErrorDtoV6> = emptyList(),
    @get:Schema(description = "When the task has been created", required = true)
    val createdAt: Instant,
    @get:Schema(description = "When the task has last been modified", required = true)
    val modifiedAt: Instant,
    @get:Schema(description = "The timestamp until the task is removed from the Orchestrator", deprecated = true)
    val timeout: Instant
)

@Schema(description = "Request object for giving a list of task identifiers to search for the state of tasks")
data class TaskStateRequestV6(
    val entries: List<Entry>
){
    data class Entry(
        val taskId: String,
        val recordId: String
    )
}

@Schema(description = "Request object for reserving a number of tasks waiting in a step queue.")
data class TaskStepReservationRequestV6(
    @get:Schema(description = "The maximum number of tasks to reserve. Can be fewer if queue is not full enough.", required = true)
    val amount: Int = 10,
    @get:Schema(description = "The step queue to reserve from", required = true)
    val step: TaskStepV6
)

data class FinishedTaskEventsResponseV6(
    override val totalElements: Long,
    override val totalPages: Int,
    override val page: Int,
    override val contentSize: Int,
    override val content: Collection<Event>
): IPageDto<FinishedTaskEventsResponseV6.Event>{
    data class Event(
        val timestamp: Instant,
        val resultState: ResultStateV6,
        val taskId: String
    )
}

data class TaskResultStateSearchRequestV6(
    val taskIds: List<String>
)

data class TaskResultStateSearchResponseV6(
    val resultStates: List<ResultStateV6?>
)
