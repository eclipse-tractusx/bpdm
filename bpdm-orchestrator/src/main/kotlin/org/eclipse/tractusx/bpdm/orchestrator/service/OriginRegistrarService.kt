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

import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.entity.DbTimestamp
import org.eclipse.tractusx.bpdm.orchestrator.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.entity.OriginRegistrarDb
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.bpdm.orchestrator.repository.OriginRegistrarRepository
import org.eclipse.tractusx.orchestrator.api.model.PriorityEnum
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.eclipse.tractusx.orchestrator.api.model.UpsertOriginRequest
import org.eclipse.tractusx.orchestrator.api.model.UpsertOriginResponse
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class OriginRegistrarService(
    private val originRegistrarRepository: OriginRegistrarRepository,
    private val taskRepository: GoldenRecordTaskRepository,
) {

    fun getPriority(originId:String):PriorityEnum{
        return if (originId != null) {
            originRegistrarRepository.findByOriginId(originId)?.let { priorityIndicator ->
                taskRepository.countByOriginIdAndProcessingStateResultStateAndPriorityAndCreatedAtAfter(originId, GoldenRecordTaskDb.ResultState.Pending, PriorityEnum.High, DbTimestamp(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())).takeIf { it >= priorityIndicator.threshold }
                    ?.let { PriorityEnum.Low }
                    ?: priorityIndicator.priority
            } ?: PriorityEnum.Low
        } else PriorityEnum.Low
    }

    fun registerOrigin(request: UpsertOriginRequest): UpsertOriginResponse {
        var originDb = originRegistrarRepository.save(
            OriginRegistrarDb(
                originId = UUID.randomUUID().toString(),
                name = request.name,
                threshold = request.threshold,
                priority = request.priority
            )
        )
        return UpsertOriginResponse(originId = originDb.originId, name = originDb.name,
            priority = originDb.priority, threshold = originDb.threshold)
    }

    fun fetchOrigin(originId: String): UpsertOriginResponse {
        val originDb = originRegistrarRepository.findByOriginId(originId)
        return if (originDb == null){
            throw BpdmNotFoundException("Origin Value", originId)
        }else {
            UpsertOriginResponse(
                originId = originDb.originId, name = originDb.name,
                priority = originDb.priority, threshold = originDb.threshold
            )
        }
    }

    fun updateOrigin(originId: String, request: UpsertOriginRequest): UpsertOriginResponse {
        var originDb = originRegistrarRepository.findByOriginId(originId) ?: throw BpdmNotFoundException("Origin Value", originId)
        originDb.name = request.name
        originDb.priority = request.priority
        originDb.threshold = request.threshold
        originDb = originRegistrarRepository.save(originDb)
        return UpsertOriginResponse(
            originId = originDb.originId, name = originDb.name,
            priority = originDb.priority, threshold = originDb.threshold
        )
    }
}