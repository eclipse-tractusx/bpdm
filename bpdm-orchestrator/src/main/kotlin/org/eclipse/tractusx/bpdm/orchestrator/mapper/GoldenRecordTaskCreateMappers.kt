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

package org.eclipse.tractusx.bpdm.orchestrator.mapper

import org.eclipse.tractusx.bpdm.orchestrator.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.model.GoldenRecordTaskCreateRequest
import org.eclipse.tractusx.bpdm.orchestrator.service.parser.GoldenRecordTaskResponseParser
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateResponse
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class GoldenRecordTaskCreateInboundMapper {
    fun toRequests(createRequest: TaskCreateRequest): List<GoldenRecordTaskCreateRequest> {
        return createRequest.requests.mapIndexed { index, request ->
            GoldenRecordTaskCreateRequest(
                index = index,
                mode = createRequest.mode,
                recordId = request.recordId,
                businessPartner = request.businessPartner
            )
        }
    }
}

@Component
class GoldenRecordTaskCreateOutboundMapper(
    private val responseParser: GoldenRecordTaskResponseParser
) {
    fun toResponse(createdTasks: List<GoldenRecordTaskDb>, retentionTimeout: (GoldenRecordTaskDb) -> Instant): TaskCreateResponse {
        return createdTasks
            .map { task -> responseParser.toClientState(task, retentionTimeout(task)) }
            .let { TaskCreateResponse(createdTasks = it) }
    }
}
