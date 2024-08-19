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

package org.eclipse.tractusx.bpdm.orchestrator.repository

import org.eclipse.tractusx.bpdm.orchestrator.entity.DbTimestamp
import org.eclipse.tractusx.bpdm.orchestrator.entity.GateRecordDb
import org.eclipse.tractusx.bpdm.orchestrator.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface GoldenRecordTaskRepository : CrudRepository<GoldenRecordTaskDb, Long>, PagingAndSortingRepository<GoldenRecordTaskDb, Long> {

    fun findByUuidIn(uuids: Set<UUID>): Set<GoldenRecordTaskDb>

    @Query("SELECT task from GoldenRecordTaskDb task WHERE task.processingState.step = :step AND task.processingState.stepState = :stepState")
    fun findByStepAndStepState(step: TaskStep, stepState: GoldenRecordTaskDb.StepState, pageable: Pageable): Page<GoldenRecordTaskDb>

    fun findByProcessingStatePendingTimeoutBefore(time: DbTimestamp, pageable: Pageable): Page<GoldenRecordTaskDb>

    fun findByProcessingStateRetentionTimeoutBefore(time: DbTimestamp, pageable: Pageable): Page<GoldenRecordTaskDb>

    fun findTasksByGateRecordAndProcessingStateResultState(record: GateRecordDb, resultState: GoldenRecordTaskDb.ResultState) : Set<GoldenRecordTaskDb>
}