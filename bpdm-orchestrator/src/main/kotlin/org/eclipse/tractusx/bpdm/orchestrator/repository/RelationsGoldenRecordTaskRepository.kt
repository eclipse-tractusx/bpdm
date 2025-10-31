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

package org.eclipse.tractusx.bpdm.orchestrator.repository

import org.eclipse.tractusx.bpdm.orchestrator.entity.DbTimestamp
import org.eclipse.tractusx.bpdm.orchestrator.entity.RelationsGoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.entity.SharingMemberRecordDb
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RelationsGoldenRecordTaskRepository : CrudRepository<RelationsGoldenRecordTaskDb, Long>, PagingAndSortingRepository<RelationsGoldenRecordTaskDb, Long> {

    fun findByUuidIn(uuids: Set<UUID>): Set<RelationsGoldenRecordTaskDb>

    @Query("SELECT DISTINCT task FROM RelationsGoldenRecordTaskDb task LEFT JOIN FETCH task.gateRecord WHERE task IN :tasks")
    fun fetchGateRecords(tasks: Set<RelationsGoldenRecordTaskDb>): Set<RelationsGoldenRecordTaskDb>

    @Query("SELECT DISTINCT task FROM RelationsGoldenRecordTaskDb task LEFT JOIN FETCH task.processingState.errors WHERE task IN :tasks")
    fun fetchProcessingErrors(tasks: Set<RelationsGoldenRecordTaskDb>): Set<RelationsGoldenRecordTaskDb>

    @Query("SELECT task from RelationsGoldenRecordTaskDb task WHERE task.processingState.step = :step AND task.processingState.stepState = :stepState")
    fun findByStepAndStepState(step: TaskStep, stepState: RelationsGoldenRecordTaskDb.StepState, pageable: Pageable): Page<RelationsGoldenRecordTaskDb>

    fun findTasksByGateRecordInAndProcessingStateResultState(records: Set<SharingMemberRecordDb>, resultState: RelationsGoldenRecordTaskDb.ResultState) : Set<RelationsGoldenRecordTaskDb>

    fun findByProcessingStateResultStateInAndUpdatedAtAfter(resultStates: Set<RelationsGoldenRecordTaskDb.ResultState>, fromTime: DbTimestamp, pageable: Pageable): Page<RelationsGoldenRecordTaskDb>
}

fun RelationsGoldenRecordTaskRepository.fetchRelationsData(tasks: Set<RelationsGoldenRecordTaskDb>) {
    fetchGateRecords(tasks)
    fetchProcessingErrors(tasks)
}