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

package org.eclipse.tractusx.bpdm.orchestrator.v7.util

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.IPageDto
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.sharingmember.SharingMemberRecordV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.FinishedTaskEventsV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskClientRelationsStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskClientStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskCreateRelationsV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskCreateV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskProcessingRelationsStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskProcessingStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStateV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepReservationEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepReservationV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskResultStateSearchV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStateV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationV7Response
import java.time.temporal.ChronoUnit

class OrchestratorAssertRepositoryV7 {


    fun assertBusinessPartnerTaskCreateResponseEqual(actual: TaskCreateV7Response, expected: TaskCreateV7Response, isForNewRecord: Boolean){
        assertBusinessPartnerClientStatesEqual(actual.createdTasks, expected.createdTasks, ignoreTaskId = true, ignoreRecordId = isForNewRecord)
    }

    fun assertBusinessPartnerTaskStateResponseEqual(actual: TaskStateV7Response, expected: TaskStateV7Response){
        assertBusinessPartnerClientStatesEqual(actual.tasks, expected.tasks, ignoreTaskId = false, ignoreRecordId = false)
    }

    fun assertBusinessPartnerTaskReservationResponseEqual(actual: TaskStepReservationV7Response, expected: TaskStepReservationV7Response, ignoreRecordId: Boolean){
        assertBusinessPartnerTaskReservationEntriesEqual(actual.reservedTasks, expected.reservedTasks, ignoreRecordId)
        Assertions.assertThat(actual.timeout).isCloseTo(expected.timeout, Assertions.within(1, ChronoUnit.SECONDS))
    }

    fun assertBusinessPartnerTaskReservationEntriesEqual(actual: List<TaskStepReservationEntryV7Dto>, expected: List<TaskStepReservationEntryV7Dto>, ignoreRecordId: Boolean){
        val ignoredFields = listOfNotNull(
            TaskStepReservationEntryV7Dto::recordId.name.takeIf { ignoreRecordId }
        ).toTypedArray()

        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(*ignoredFields)
            .isEqualTo(expected)
    }

    fun assertTaskResultStateSearchResponseEqual(actual: TaskResultStateSearchV7Response, expected: TaskResultStateSearchV7Response){
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    fun assertFinishedTaskEventsResponseEqual(actual: FinishedTaskEventsV7Response, expected: FinishedTaskEventsV7Response){
        assertPageDto(actual, expected)

        assertFinishedBusinessPartnerTaskEventEqual(actual.content, expected.content)
    }

    fun assertFinishedBusinessPartnerTaskEventEqual(actual: Collection<FinishedTaskEventsV7Response.Event>, expected: Collection<FinishedTaskEventsV7Response.Event>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                FinishedTaskEventsV7Response.Event::timestamp.name
            )
            .isEqualTo(expected)

        actual.zip(expected){ actualEntry, expectedEntry ->
            Assertions.assertThat(actualEntry.timestamp).isCloseTo(expectedEntry.timestamp, Assertions.within(1, ChronoUnit.SECONDS))
        }
    }

    fun assertRelationTaskCreateResponseEqual(actual: TaskCreateRelationsV7Response, expected: TaskCreateRelationsV7Response, isForNewRecord: Boolean){
        assertTaskClientRelationsStateEqual(actual.createdTasks, expected.createdTasks, ignoreTaskId = true, ignoreRecordId = isForNewRecord)
    }

    fun assertRelationTaskStateResponseEqual(actual: TaskRelationsStateV7Response, expected: TaskRelationsStateV7Response){
        assertTaskClientRelationsStateEqual(actual.tasks, expected.tasks, ignoreTaskId = false, ignoreRecordId = false)
    }

    fun assertRelationTaskReservationResponseEqual(
        actual: TaskRelationsStepReservationV7Response,
        expected: TaskRelationsStepReservationV7Response,
        ignoreRecordId: Boolean
    ){
        assertRelationTaskReservationEntriesEqual(actual.reservedTasks, expected.reservedTasks, ignoreRecordId)
        Assertions.assertThat(actual.timeout).isCloseTo(expected.timeout, Assertions.within(1, ChronoUnit.SECONDS))
    }

    fun assertTaskClientRelationsStateEqual(
        actual: List<TaskClientRelationsStateV7Dto>,
        expected: List<TaskClientRelationsStateV7Dto>,
        ignoreTaskId: Boolean = true,
        ignoreRecordId: Boolean = true
    ){
        val ignoredFields = listOfNotNull(
            TaskClientStateV7Dto::taskId.name.takeIf { ignoreTaskId },
            TaskClientStateV7Dto::recordId.name.takeIf { ignoreRecordId },
            TaskClientStateV7Dto::processingState.name
        ).toTypedArray()

        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(*ignoredFields)
            .isEqualTo(expected)

        assertRelationProcessingStates(actual.map { it.processingState }, expected.map { it.processingState })
    }

    fun assertRelationTaskReservationEntriesEqual(
        actual: List<TaskRelationsStepReservationEntryV7Dto>,
        expected: List<TaskRelationsStepReservationEntryV7Dto>,
        ignoreRecordId: Boolean
    ){
        val ignoredFields = listOfNotNull(
            TaskStepReservationEntryV7Dto::recordId.name.takeIf { ignoreRecordId }
        ).toTypedArray()

        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(*ignoredFields)
            .isEqualTo(expected)
    }


    fun assertBusinessPartnerClientStatesEqual(
        actual: List<TaskClientStateV7Dto>,
        expected: List<TaskClientStateV7Dto>,
        ignoreTaskId: Boolean = true,
        ignoreRecordId: Boolean = true
    ){
        val ignoredFields = listOfNotNull(
            TaskClientStateV7Dto::taskId.name.takeIf { ignoreTaskId },
            TaskClientStateV7Dto::recordId.name.takeIf { ignoreRecordId },
            TaskClientStateV7Dto::processingState.name
        ).toTypedArray()

        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(*ignoredFields)
            .isEqualTo(expected)

        assertBusinessPartnerProcessingStates(actual.map { it.processingState }, expected.map { it.processingState })
    }

    fun assertSharingMemberRecordEqual(actual: PageDto<SharingMemberRecordV7Dto>, expected: PageDto<SharingMemberRecordV7Dto>){
        assertPageDto(actual, expected)

        actual.content.zip(expected.content){ actualEntry, expectedEntry -> assertSharingMemberRecordEqual(actualEntry, expectedEntry) }
    }

    fun assertSharingMemberRecordEqual(actual: SharingMemberRecordV7Dto, expected: SharingMemberRecordV7Dto){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                SharingMemberRecordV7Dto::createdAt.name,
                SharingMemberRecordV7Dto::updatedAt.name
            )
            .isEqualTo(expected)

        Assertions.assertThat(actual.createdAt).isCloseTo(expected.createdAt, Assertions.within(1, ChronoUnit.SECONDS))
        Assertions.assertThat(actual.updatedAt).isCloseTo(expected.updatedAt, Assertions.within(1, ChronoUnit.SECONDS))
    }

    fun assertBusinessPartnerProcessingStates(actual: List<TaskProcessingStateV7Dto>, expected: List<TaskProcessingStateV7Dto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(TaskProcessingStateV7Dto::createdAt.name)
            .ignoringFields(TaskProcessingStateV7Dto::modifiedAt.name)
            .ignoringFields(TaskProcessingStateV7Dto::timeout.name)
            .isEqualTo(expected)

        actual.zip(expected).forEach { (actualEntry, expectedEntry) ->
            Assertions.assertThat(actualEntry.timeout).isCloseTo(expectedEntry.timeout, Assertions.within(1, ChronoUnit.SECONDS))
        }
    }

    fun assertRelationProcessingStates(actual: List<TaskProcessingRelationsStateV7Dto>, expected: List<TaskProcessingRelationsStateV7Dto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(TaskProcessingRelationsStateV7Dto::createdAt.name)
            .ignoringFields(TaskProcessingRelationsStateV7Dto::modifiedAt.name)
            .ignoringFields(TaskProcessingRelationsStateV7Dto::timeout.name)
            .isEqualTo(expected)

        actual.zip(expected).forEach { (actualEntry, expectedEntry) ->
            Assertions.assertThat(actualEntry.timeout).isCloseTo(expectedEntry.timeout, Assertions.within(1, ChronoUnit.SECONDS))
        }
    }

    fun assertPageDto(actual: IPageDto<*>, expected: IPageDto<*>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                IPageDto<*>::content.name
            )
            .isEqualTo(expected)
    }
}