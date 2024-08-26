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

package org.eclipse.tractusx.bpdm.orchestrator.service

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.orchestrator.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.entity.toTimestamp
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.orchestrator.api.model.FinishedTaskEventsResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class GoldenRecordTaskEventService(
    private val taskRepository: GoldenRecordTaskRepository,
    private val responseMapper: ResponseMapper
) {
    private val finishedTaskStates = setOf(GoldenRecordTaskDb.ResultState.Success,GoldenRecordTaskDb.ResultState.Error)


    fun getFinishedTaskEvents(timestamp: Instant, paginationRequest: PaginationRequest): FinishedTaskEventsResponse{
        val pageRequest = PageRequest.of(paginationRequest.page, paginationRequest.size, Sort.Direction.ASC, "updatedAt")
        val finishedTasksPage = taskRepository.findByProcessingStateResultStateInAndUpdatedAtAfter(finishedTaskStates, timestamp.toTimestamp(), pageRequest )

        return FinishedTaskEventsResponse(
            totalElements = finishedTasksPage.totalElements,
            totalPages = finishedTasksPage.totalPages,
            page = finishedTasksPage.number,
            contentSize = finishedTasksPage.size,
            content = finishedTasksPage.content.map { FinishedTaskEventsResponse.Event(it.updatedAt.instant,responseMapper.toResultState(it.processingState.resultState), it.uuid.toString()) }
        )
    }
}