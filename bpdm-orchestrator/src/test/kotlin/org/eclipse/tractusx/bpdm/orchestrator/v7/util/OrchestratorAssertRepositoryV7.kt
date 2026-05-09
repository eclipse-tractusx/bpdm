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
import org.eclipse.tractusx.orchestrator.api.SharingMemberRecord
import org.eclipse.tractusx.orchestrator.api.model.*
import java.time.temporal.ChronoUnit

class OrchestratorAssertRepositoryV7 {


    fun assertBusinessPartnerTaskCreateResponseEqual(actual: TaskCreateResponse, expected: TaskCreateResponse, isForNewRecord: Boolean){
        assertBusinessPartnerClientStatesEqual(actual.createdTasks, expected.createdTasks, ignoreTaskId = true, ignoreRecordId = isForNewRecord)
    }

    fun assertBusinessPartnerTaskStateResponseEqual(actual: TaskStateResponse, expected: TaskStateResponse){
        assertBusinessPartnerClientStatesEqual(actual.tasks, expected.tasks, ignoreTaskId = false, ignoreRecordId = false)
    }

    fun assertBusinessPartnerTaskReservationResponseEqual(actual: TaskStepReservationResponse, expected: TaskStepReservationResponse, ignoreRecordId: Boolean){
        assertBusinessPartnerTaskReservationEntriesEqual(actual.reservedTasks, expected.reservedTasks, ignoreRecordId)
        Assertions.assertThat(actual.timeout).isCloseTo(expected.timeout, Assertions.within(1, ChronoUnit.SECONDS))
    }

    fun assertBusinessPartnerTaskReservationEntriesEqual(actual: List<TaskStepReservationEntryDto>, expected: List<TaskStepReservationEntryDto>, ignoreRecordId: Boolean){
        val ignoredFields = listOfNotNull(
            TaskStepReservationEntryDto::recordId.name.takeIf { ignoreRecordId }
        ).toTypedArray()

        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(*ignoredFields)
            .isEqualTo(expected)
    }

    fun assertTaskResultStateSearchResponseEqual(actual: TaskResultStateSearchResponse, expected: TaskResultStateSearchResponse){
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    fun assertFinishedTaskEventsResponseEqual(actual: FinishedTaskEventsResponse, expected: FinishedTaskEventsResponse){
        assertPageDto(actual, expected)

        assertFinishedBusinessPartnerTaskEventEqual(actual.content, expected.content)
    }

    fun assertFinishedBusinessPartnerTaskEventEqual(actual: Collection<FinishedTaskEventsResponse.Event>, expected: Collection<FinishedTaskEventsResponse.Event>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                FinishedTaskEventsResponse.Event::timestamp.name
            )
            .isEqualTo(expected)

        actual.zip(expected){ actualEntry, expectedEntry ->
            Assertions.assertThat(actualEntry.timestamp).isCloseTo(expectedEntry.timestamp, Assertions.within(1, ChronoUnit.SECONDS))
        }
    }

    fun assertRelationTaskCreateResponseEqual(actual: TaskCreateRelationsResponse, expected: TaskCreateRelationsResponse, isForNewRecord: Boolean){
        assertTaskClientRelationsStateEqual(actual.createdTasks, expected.createdTasks, ignoreTaskId = true, ignoreRecordId = isForNewRecord)
    }

    fun assertRelationTaskStateResponseEqual(actual: TaskRelationsStateResponse, expected: TaskRelationsStateResponse){
        assertTaskClientRelationsStateEqual(actual.tasks, expected.tasks, ignoreTaskId = false, ignoreRecordId = false)
    }

    fun assertRelationTaskReservationResponseEqual(
        actual: TaskRelationsStepReservationResponse,
        expected: TaskRelationsStepReservationResponse,
        ignoreRecordId: Boolean
    ){
        assertRelationTaskReservationEntriesEqual(actual.reservedTasks, expected.reservedTasks, ignoreRecordId)
        Assertions.assertThat(actual.timeout).isCloseTo(expected.timeout, Assertions.within(1, ChronoUnit.SECONDS))
    }

    fun assertTaskClientRelationsStateEqual(
        actual: List<TaskClientRelationsStateDto>,
        expected: List<TaskClientRelationsStateDto>,
        ignoreTaskId: Boolean = true,
        ignoreRecordId: Boolean = true
    ){
        val ignoredFields = listOfNotNull(
            TaskClientStateDto::taskId.name.takeIf { ignoreTaskId },
            TaskClientStateDto::recordId.name.takeIf { ignoreRecordId },
            TaskClientStateDto::processingState.name
        ).toTypedArray()

        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(*ignoredFields)
            .isEqualTo(expected)

        assertRelationProcessingStates(actual.map { it.processingState }, expected.map { it.processingState })
    }

    fun assertRelationTaskReservationEntriesEqual(
        actual: List<TaskRelationsStepReservationEntryDto>,
        expected: List<TaskRelationsStepReservationEntryDto>,
        ignoreRecordId: Boolean
    ){
        val ignoredFields = listOfNotNull(
            TaskStepReservationEntryDto::recordId.name.takeIf { ignoreRecordId }
        ).toTypedArray()

        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(*ignoredFields)
            .isEqualTo(expected)
    }


    fun assertBusinessPartnerClientStatesEqual(
        actual: List<TaskClientStateDto>,
        expected: List<TaskClientStateDto>,
        ignoreTaskId: Boolean = true,
        ignoreRecordId: Boolean = true
    ){
        val ignoredFields = listOfNotNull(
            TaskClientStateDto::taskId.name.takeIf { ignoreTaskId },
            TaskClientStateDto::recordId.name.takeIf { ignoreRecordId },
            TaskClientStateDto::processingState.name
        ).toTypedArray()

        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(*ignoredFields)
            .isEqualTo(expected)

        assertBusinessPartnerProcessingStates(actual.map { it.processingState }, expected.map { it.processingState })
    }

    fun assertSharingMemberRecordEqual(actual: PageDto<SharingMemberRecord>, expected: PageDto<SharingMemberRecord>){
        assertPageDto(actual, expected)

        actual.content.zip(expected.content){ actualEntry, expectedEntry -> assertSharingMemberRecordEqual(actualEntry, expectedEntry) }
    }

    fun assertSharingMemberRecordEqual(actual: SharingMemberRecord, expected: SharingMemberRecord){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                SharingMemberRecord::createdAt.name,
                SharingMemberRecord::updatedAt.name
            )
            .isEqualTo(expected)

        Assertions.assertThat(actual.createdAt).isCloseTo(expected.createdAt, Assertions.within(1, ChronoUnit.SECONDS))
        Assertions.assertThat(actual.updatedAt).isCloseTo(expected.updatedAt, Assertions.within(1, ChronoUnit.SECONDS))
    }

    fun assertBusinessPartnerProcessingStates(actual: List<TaskProcessingStateDto>, expected: List<TaskProcessingStateDto>) {
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

    fun assertRelationProcessingStates(actual: List<TaskProcessingRelationsStateDto>, expected: List<TaskProcessingRelationsStateDto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(TaskProcessingRelationsStateDto::createdAt.name)
            .ignoringFields(TaskProcessingRelationsStateDto::modifiedAt.name)
            .ignoringFields(TaskProcessingRelationsStateDto::timeout.name)
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