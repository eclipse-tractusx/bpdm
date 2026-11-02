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

package org.eclipse.tractusx.orchestrator.api.v6.client

import org.eclipse.tractusx.orchestrator.api.ApiCommons
import org.eclipse.tractusx.orchestrator.api.v6.GoldenRecordTaskApiV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskCreateRequestV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskCreateResponseV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskResultStateSearchRequestV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskResultStateSearchResponseV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStateRequestV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStateResponseV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepReservationRequestV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepReservationResponseV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepResultRequestV6
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.PostExchange

interface GoldenRecordTaskApiClientV6: GoldenRecordTaskApiV6 {

    @PostExchange(value = ApiCommons.BASE_PATH_V6)
    override fun createTasks(
        @RequestBody createRequest: TaskCreateRequestV6
    ): TaskCreateResponseV6

    @PostExchange(value = "${ApiCommons.BASE_PATH_V6}/step-reservations")
    override fun reserveTasksForStep(
        @RequestBody reservationRequest: TaskStepReservationRequestV6
    ): TaskStepReservationResponseV6

    @PostExchange(value = "${ApiCommons.BASE_PATH_V6}/step-results")
    override fun resolveStepResults(
        @RequestBody resultRequest: TaskStepResultRequestV6
    )

    @PostExchange(value = "${ApiCommons.BASE_PATH_V6}/state/search")
    override fun searchTaskStates(
        @RequestBody stateRequest: TaskStateRequestV6
    ): TaskStateResponseV6

    @PostExchange(value = "${ApiCommons.BASE_PATH_V6}/result-state/search")
    override fun searchTaskResultStates(
        @RequestBody stateRequest: TaskResultStateSearchRequestV6
    ): TaskResultStateSearchResponseV6
}