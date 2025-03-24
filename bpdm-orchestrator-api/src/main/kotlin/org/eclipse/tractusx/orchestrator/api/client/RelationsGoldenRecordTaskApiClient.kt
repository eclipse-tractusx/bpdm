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

package org.eclipse.tractusx.orchestrator.api.client

import org.eclipse.tractusx.orchestrator.api.RelationsGoldenRecordTaskApi
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange(RelationsGoldenRecordTaskApi.RELATIONS_TASKS_PATH)
interface RelationsGoldenRecordTaskApiClient: RelationsGoldenRecordTaskApi {

    @PostExchange
    override fun createTasks(
        @RequestBody createRequest: TaskCreateRelationsRequest
    ): TaskCreateRelationsResponse

    @PostExchange("/state/search")
    override fun searchTaskStates(
        @RequestBody stateRequest: TaskStateRequest
    ): TaskRelationsStateResponse

    @PostExchange("/result-state/search")
    override fun searchTaskResultStates(
        @RequestBody stateRequest: TaskResultStateSearchRequest
    ): TaskResultStateSearchResponse

    @PostExchange("/step-reservations")
    override fun reserveTasksForStep(
        @RequestBody reservationRequest: TaskStepReservationRequest
    ): TaskRelationsStepReservationResponse

    @PostExchange("/step-results")
    override fun resolveStepResults(
        @RequestBody resultRequest: TaskRelationsStepResultRequest
    )

}