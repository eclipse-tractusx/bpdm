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
import org.eclipse.tractusx.orchestrator.api.model.FinishedTaskEventsResponse
import org.eclipse.tractusx.orchestrator.api.model.TaskProcessingStateDto
import org.eclipse.tractusx.orchestrator.api.v6.model.*
import java.time.temporal.ChronoUnit

class OrchestratorAssertRepositoryV6 {

    fun assertCreatedTasksForNewSharingMemberRecords(actual: TaskCreateResponse, expected: TaskCreateResponse) {
        assertCreatedTasksForNewSharingMemberRecords(actual.createdTasks, expected.createdTasks)
    }

    fun assertCreatedTasksForExistingSharingMemberRecords(actual: TaskCreateResponse, expected: TaskCreateResponse) {
        assertCreatedTasksForExistingSharingMemberRecords(actual.createdTasks, expected.createdTasks)
    }

    fun assertCreatedTasksForNewSharingMemberRecords(actual: List<TaskClientStateDto>, expected: List<TaskClientStateDto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                TaskClientStateDto::taskId.name,
                TaskClientStateDto::recordId.name,
                TaskClientStateDto::processingState.name
            )
            .isEqualTo(expected)

        assertProcessingStates(actual.map { it.processingState }, expected.map { it.processingState })
    }

    fun assertCreatedTasksForExistingSharingMemberRecords(actual: List<TaskClientStateDto>, expected: List<TaskClientStateDto>){
        assertCreatedTasksForNewSharingMemberRecords(actual, expected)
        actual.zip(expected){ actualEntry, expectedEntry -> Assertions.assertThat(actualEntry.recordId).isEqualTo(expectedEntry.recordId) }
    }

    fun assertSearchedTaskClientState(actual: List<TaskClientStateDto>, expected: List<TaskClientStateDto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                TaskClientStateDto::processingState.name
            )
            .isEqualTo(expected)

        assertProcessingStates(actual.map { it.processingState }, expected.map { it.processingState })
    }


    fun assertTaskReservationResponse(actual: TaskStepReservationResponse, expected: TaskStepReservationResponse){
        assertTaskReservationEntry(actual.reservedTasks, expected.reservedTasks)

        Assertions.assertThat(actual.timeout).isCloseTo(expected.timeout, Assertions.within(1, ChronoUnit.SECONDS))
    }

    fun assertTaskStateResponse(actual: TaskStateResponse, expected: TaskStateResponse){
        assertSearchedTaskClientState(actual.tasks, expected.tasks)
    }

    fun assertFinishedTasksResponse(actual: FinishedTaskEventsResponse, expected: FinishedTaskEventsResponse){
        assertPageDto(actual, expected)
        assertFinishedTaskEvents(actual.content, expected.content)
    }

    fun assertFinishedTaskEvents(actual: Collection<FinishedTaskEventsResponse.Event>, expected: Collection<FinishedTaskEventsResponse.Event>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(FinishedTaskEventsResponse.Event::timestamp.name)
            .isEqualTo(expected)

        actual.zip(expected){ actualEntry, expectedEntry ->
            Assertions.assertThat(actualEntry.timestamp).isCloseTo(expectedEntry.timestamp, Assertions.within(1, ChronoUnit.SECONDS))
        }
    }

    fun assertTaskReservationEntry(actual: List<TaskStepReservationEntryDto>, expected: List<TaskStepReservationEntryDto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                TaskStepReservationEntryDto::taskId.name,
                TaskStepReservationEntryDto::recordId.name
            )
            .isEqualTo(expected)
    }

    fun assertProcessingStates(actual: List<TaskProcessingStateDto>, expected: List<TaskProcessingStateDto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(TaskProcessingStateDto::createdAt.name)
            .ignoringFields(TaskProcessingStateDto::modifiedAt.name)
            .ignoringFields(TaskProcessingStateDto::timeout.name)
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