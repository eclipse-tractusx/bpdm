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

package org.eclipse.tractusx.bpdm.orchestrator.api.v6.client

import org.eclipse.tractusx.bpdm.orchestrator.api.ApiCommons
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.OrchestratorGoldenRecordTaskV6Api
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskCreateV6Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskCreateV6Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskResultStateSearchV6Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskResultStateSearchV6Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStateV6Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStateV6Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStepReservationV6Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStepReservationV6Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStepResultV6Request
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.PostExchange

interface GoldenRecordTaskV6ApiClient: OrchestratorGoldenRecordTaskV6Api {

    @PostExchange(value = ApiCommons.BASE_PATH_V6)
    override fun createTasks(
        @RequestBody createRequest: TaskCreateV6Request
    ): TaskCreateV6Response

    @PostExchange(value = "${ApiCommons.BASE_PATH_V6}/step-reservations")
    override fun reserveTasksForStep(
        @RequestBody reservationRequest: TaskStepReservationV6Request
    ): TaskStepReservationV6Response

    @PostExchange(value = "${ApiCommons.BASE_PATH_V6}/step-results")
    override fun resolveStepResults(
        @RequestBody resultRequest: TaskStepResultV6Request
    )

    @PostExchange(value = "${ApiCommons.BASE_PATH_V6}/state/search")
    override fun searchTaskStates(
        @RequestBody stateRequest: TaskStateV6Request
    ): TaskStateV6Response

    @PostExchange(value = "${ApiCommons.BASE_PATH_V6}/result-state/search")
    override fun searchTaskResultStates(
        @RequestBody stateRequest: TaskResultStateSearchV6Request
    ): TaskResultStateSearchV6Response
}