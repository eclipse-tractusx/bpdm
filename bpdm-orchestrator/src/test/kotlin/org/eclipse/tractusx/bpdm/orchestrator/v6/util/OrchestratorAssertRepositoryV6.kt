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
import org.eclipse.tractusx.orchestrator.api.v6.model.FinishedTaskEventsResponseV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskClientStateDtoV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskCreateResponseV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskProcessingStateDtoV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStateResponseV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepReservationEntryDtoV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepReservationResponseV6
import java.time.temporal.ChronoUnit

class OrchestratorAssertRepositoryV6 {

    fun assertCreatedTasksForNewSharingMemberRecords(actual: TaskCreateResponseV6, expected: TaskCreateResponseV6) {
        assertCreatedTasksForNewSharingMemberRecords(actual.createdTasks, expected.createdTasks)
    }

    fun assertCreatedTasksForExistingSharingMemberRecords(actual: TaskCreateResponseV6, expected: TaskCreateResponseV6) {
        assertCreatedTasksForExistingSharingMemberRecords(actual.createdTasks, expected.createdTasks)
    }

    fun assertCreatedTasksForNewSharingMemberRecords(actual: List<TaskClientStateDtoV6>, expected: List<TaskClientStateDtoV6>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                TaskClientStateDtoV6::taskId.name,
                TaskClientStateDtoV6::recordId.name,
                TaskClientStateDtoV6::processingState.name
            )
            .ignoringFieldsMatchingRegexes("(?i).*updatedAt")
            .ignoringFieldsMatchingRegexes("(?i).*path.*")
            .isEqualTo(expected)
        assertProcessingStates(actual.map { it.processingState }, expected.map { it.processingState })
    }

    fun assertCreatedTasksForExistingSharingMemberRecords(actual: List<TaskClientStateDtoV6>, expected: List<TaskClientStateDtoV6>){
        assertCreatedTasksForNewSharingMemberRecords(actual, expected)
        actual.zip(expected){ actualEntry, expectedEntry -> Assertions.assertThat(actualEntry.recordId).isEqualTo(expectedEntry.recordId) }
    }

    fun assertSearchedTaskClientState(actual: List<TaskClientStateDtoV6>, expected: List<TaskClientStateDtoV6>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                TaskClientStateDtoV6::processingState.name
            )
            .ignoringFieldsMatchingRegexes("(?i).*updatedAt")
            .ignoringFieldsMatchingRegexes("(?i).*path.*")
            .isEqualTo(expected)

        assertProcessingStates(actual.map { it.processingState }, expected.map { it.processingState })
    }


    fun assertTaskReservationResponse(actual: TaskStepReservationResponseV6, expected: TaskStepReservationResponseV6){
        assertTaskReservationEntry(actual.reservedTasks, expected.reservedTasks)

        Assertions.assertThat(actual.timeout).isCloseTo(expected.timeout, Assertions.within(1, ChronoUnit.SECONDS))
    }

    fun assertTaskStateResponse(actual: TaskStateResponseV6, expected: TaskStateResponseV6){
        assertSearchedTaskClientState(actual.tasks, expected.tasks)
    }

    fun assertFinishedTasksResponse(actual: FinishedTaskEventsResponseV6, expected: FinishedTaskEventsResponseV6){
        assertPageDto(actual, expected)
        assertFinishedTaskEvents(actual.content, expected.content)
    }

    fun assertFinishedTaskEvents(actual: Collection<FinishedTaskEventsResponseV6.Event>, expected: Collection<FinishedTaskEventsResponseV6.Event>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(FinishedTaskEventsResponseV6.Event::timestamp.name)
            .ignoringFieldsMatchingRegexes("(?i).*path.*")
            .isEqualTo(expected)

        actual.zip(expected){ actualEntry, expectedEntry ->
            Assertions.assertThat(actualEntry.timestamp).isCloseTo(expectedEntry.timestamp, Assertions.within(1, ChronoUnit.SECONDS))
        }
    }

    fun assertTaskReservationEntry(actual: List<TaskStepReservationEntryDtoV6>, expected: List<TaskStepReservationEntryDtoV6>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                TaskStepReservationEntryDtoV6::taskId.name,
                TaskStepReservationEntryDtoV6::recordId.name
            )
            .ignoringFieldsMatchingRegexes("(?i).*updatedAt")
            .ignoringFieldsMatchingRegexes("(?i).*path.*")
            .isEqualTo(expected)
    }

    fun assertProcessingStates(actual: List<TaskProcessingStateDtoV6>, expected: List<TaskProcessingStateDtoV6>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(TaskProcessingStateDtoV6::createdAt.name)
            .ignoringFields(TaskProcessingStateDtoV6::modifiedAt.name)
            .ignoringFields(TaskProcessingStateDtoV6::timeout.name)
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