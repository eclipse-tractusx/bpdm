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

package org.eclipse.tractusx.bpdm.orchestrator.v6.util

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.IPageDto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.FinishedTaskEventsV6Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskClientStateV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskCreateV6Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskProcessingStateV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStateV6Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStepReservationEntryV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStepReservationV6Response
import java.time.temporal.ChronoUnit

class OrchestratorAssertRepositoryV6 {

    fun assertCreatedTasksForNewSharingMemberRecords(actual: TaskCreateV6Response, expected: TaskCreateV6Response) {
        assertCreatedTasksForNewSharingMemberRecords(actual.createdTasks, expected.createdTasks)
    }

    fun assertCreatedTasksForExistingSharingMemberRecords(actual: TaskCreateV6Response, expected: TaskCreateV6Response) {
        assertCreatedTasksForExistingSharingMemberRecords(actual.createdTasks, expected.createdTasks)
    }

    fun assertCreatedTasksForNewSharingMemberRecords(actual: List<TaskClientStateV6Dto>, expected: List<TaskClientStateV6Dto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                TaskClientStateV6Dto::taskId.name,
                TaskClientStateV6Dto::recordId.name,
                TaskClientStateV6Dto::processingState.name
            )
            .ignoringFieldsMatchingRegexes("(?i).*updatedAt")
            .ignoringFieldsMatchingRegexes("(?i).*path.*")
            .isEqualTo(expected)
        assertProcessingStates(actual.map { it.processingState }, expected.map { it.processingState })
    }

    fun assertCreatedTasksForExistingSharingMemberRecords(actual: List<TaskClientStateV6Dto>, expected: List<TaskClientStateV6Dto>){
        assertCreatedTasksForNewSharingMemberRecords(actual, expected)
        actual.zip(expected){ actualEntry, expectedEntry -> Assertions.assertThat(actualEntry.recordId).isEqualTo(expectedEntry.recordId) }
    }

    fun assertSearchedTaskClientState(actual: List<TaskClientStateV6Dto>, expected: List<TaskClientStateV6Dto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                TaskClientStateV6Dto::processingState.name
            )
            .ignoringFieldsMatchingRegexes("(?i).*updatedAt")
            .ignoringFieldsMatchingRegexes("(?i).*path.*")
            .isEqualTo(expected)

        assertProcessingStates(actual.map { it.processingState }, expected.map { it.processingState })
    }


    fun assertTaskReservationResponse(actual: TaskStepReservationV6Response, expected: TaskStepReservationV6Response){
        assertTaskReservationEntry(actual.reservedTasks, expected.reservedTasks)

        Assertions.assertThat(actual.timeout).isCloseTo(expected.timeout, Assertions.within(1, ChronoUnit.SECONDS))
    }

    fun assertTaskStateResponse(actual: TaskStateV6Response, expected: TaskStateV6Response){
        assertSearchedTaskClientState(actual.tasks, expected.tasks)
    }

    fun assertFinishedTasksResponse(actual: FinishedTaskEventsV6Response, expected: FinishedTaskEventsV6Response){
        assertPageDto(actual, expected)
        assertFinishedTaskEvents(actual.content, expected.content)
    }

    fun assertFinishedTaskEvents(actual: Collection<FinishedTaskEventsV6Response.Event>, expected: Collection<FinishedTaskEventsV6Response.Event>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(FinishedTaskEventsV6Response.Event::timestamp.name)
            .ignoringFieldsMatchingRegexes("(?i).*path.*")
            .isEqualTo(expected)

        actual.zip(expected){ actualEntry, expectedEntry ->
            Assertions.assertThat(actualEntry.timestamp).isCloseTo(expectedEntry.timestamp, Assertions.within(1, ChronoUnit.SECONDS))
        }
    }

    fun assertTaskReservationEntry(actual: List<TaskStepReservationEntryV6Dto>, expected: List<TaskStepReservationEntryV6Dto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                TaskStepReservationEntryV6Dto::taskId.name,
                TaskStepReservationEntryV6Dto::recordId.name
            )
            .ignoringFieldsMatchingRegexes("(?i).*updatedAt")
            .ignoringFieldsMatchingRegexes("(?i).*path.*")
            .isEqualTo(expected)
    }

    fun assertProcessingStates(actual: List<TaskProcessingStateV6Dto>, expected: List<TaskProcessingStateV6Dto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(TaskProcessingStateV6Dto::createdAt.name)
            .ignoringFields(TaskProcessingStateV6Dto::modifiedAt.name)
            .ignoringFields(TaskProcessingStateV6Dto::timeout.name)
            .ignoringFieldsMatchingRegexes("(?i).*path.*")
            .isEqualTo(expected)

        actual.zip(expected).forEach { (actualEntry, expectedEntry) ->
            Assertions.assertThat(actualEntry.timeout).isCloseTo(expectedEntry.timeout, Assertions.within(1, ChronoUnit.SECONDS))
        }
    }

    fun assertPageDto(actual: IPageDto<*>, expected: IPageDto<*>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                IPageDto<*>::content.name,
                //ToDo: Due to a bug the content size is wrong https://github.com/eclipse-tractusx/bpdm/issues/1579
                IPageDto<*>::contentSize.name
            )
            .isEqualTo(expected)
    }
}