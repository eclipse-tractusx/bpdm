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

package org.eclipse.tractusx.bpdm.orchestrator.api.v7.client

import org.eclipse.tractusx.bpdm.orchestrator.api.ApiCommons
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.OrchestratorRelationsGoldenRecordTaskV7Api
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskCreateRelationsV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskCreateRelationsV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStateV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepReservationV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepResultV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskResultStateSearchV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskResultStateSearchV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStateV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationV7Request
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange
interface RelationsGoldenRecordTaskV7ApiClient: OrchestratorRelationsGoldenRecordTaskV7Api {

    @PostExchange(value = ApiCommons.BASE_PATH_V7_RELATIONS)
    override fun createTasks(
        @RequestBody createRequest: TaskCreateRelationsV7Request
    ): TaskCreateRelationsV7Response

    @PostExchange(value = "${ApiCommons.BASE_PATH_V7_RELATIONS}/state/search")
    override fun searchTaskStates(
        @RequestBody stateRequest: TaskStateV7Request
    ): TaskRelationsStateV7Response

    @PostExchange(value = "${ApiCommons.BASE_PATH_V7_RELATIONS}/result-state/search")
    override fun searchTaskResultStates(
        @RequestBody stateRequest: TaskResultStateSearchV7Request
    ): TaskResultStateSearchV7Response

    @PostExchange(value = "${ApiCommons.BASE_PATH_V7_RELATIONS}/step-reservations")
    override fun reserveTasksForStep(
        @RequestBody reservationRequest: TaskStepReservationV7Request
    ): TaskRelationsStepReservationV7Response

    @PostExchange(value = "${ApiCommons.BASE_PATH_V7_RELATIONS}/step-results")
    override fun resolveStepResults(
        @RequestBody resultRequest: TaskRelationsStepResultV7Request
    )

}
