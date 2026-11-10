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

package org.eclipse.tractusx.bpdm.orchestrator.controller

import org.eclipse.tractusx.bpdm.common.exception.BpdmUpsertLimitException
import org.eclipse.tractusx.bpdm.orchestrator.config.ApiConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.service.GoldenRecordTaskService
import org.eclipse.tractusx.bpdm.orchestrator.service.application.v7.GoldenRecordTaskCreateApplicationV7Service
import org.eclipse.tractusx.bpdm.orchestrator.service.application.v7.GoldenRecordTaskReserveApplicationV7Service
import org.eclipse.tractusx.bpdm.orchestrator.service.application.v7.GoldenRecordTaskResolveApplicationV7Service
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.OrchestratorGoldenRecordTaskV7Api
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskCreateV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskCreateV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskResultStateSearchV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskResultStateSearchV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStateV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStateV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepResultV7Request
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GoldenRecordTaskController(
    val apiConfigProperties: ApiConfigProperties,
    val goldenRecordTaskService: GoldenRecordTaskService,
    val goldenRecordTaskCreateApplicationService: GoldenRecordTaskCreateApplicationV7Service,
    val goldenRecordTaskReserveApplicationService: GoldenRecordTaskReserveApplicationV7Service,
    val goldenRecordTaskResolveApplicationService: GoldenRecordTaskResolveApplicationV7Service
) : OrchestratorGoldenRecordTaskV7Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.CREATE_TASK})")
    override fun createTasks(createRequest: TaskCreateV7Request): TaskCreateV7Response {
        if (createRequest.requests.size > apiConfigProperties.upsertLimit)
            throw BpdmUpsertLimitException(createRequest.requests.size, apiConfigProperties.upsertLimit)

        return goldenRecordTaskCreateApplicationService.createTasks(createRequest)
    }

    @PreAuthorize("@stepSecurityService.assertHasReservationAuthority(authentication, #reservationRequest.step)")
    override fun reserveTasksForStep(reservationRequest: TaskStepReservationV7Request): TaskStepReservationV7Response {
        if (reservationRequest.amount > apiConfigProperties.upsertLimit)
            throw BpdmUpsertLimitException(reservationRequest.amount, apiConfigProperties.upsertLimit)

        return goldenRecordTaskReserveApplicationService.reserveTasksForStep(reservationRequest)
    }

    @PreAuthorize("@stepSecurityService.assertHasResultAuthority(authentication, #resultRequest.step)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun resolveStepResults(resultRequest: TaskStepResultV7Request) {
        if (resultRequest.results.size > apiConfigProperties.upsertLimit)
            throw BpdmUpsertLimitException(resultRequest.results.size, apiConfigProperties.upsertLimit)

        goldenRecordTaskResolveApplicationService.resolveStepResults(resultRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.VIEW_TASK})")
    override fun searchTaskStates(stateRequest: TaskStateV7Request): TaskStateV7Response {
        return goldenRecordTaskService.searchTaskStates(stateRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.VIEW_TASK})")
    override fun searchTaskResultStates(stateRequest: TaskResultStateSearchV7Request): TaskResultStateSearchV7Response {
        return goldenRecordTaskService.searchTaskResultStates(stateRequest)
    }

}
