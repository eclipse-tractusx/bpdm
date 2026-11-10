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

package org.eclipse.tractusx.bpdm.orchestrator.controller.v6

import org.eclipse.tractusx.bpdm.common.exception.BpdmUpsertLimitException
import org.eclipse.tractusx.bpdm.orchestrator.config.ApiConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.service.application.v6.GoldenRecordTaskCreateApplicationV6Service
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
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController("GoldenRecordTaskControllerLegacy")
class GoldenRecordTaskControllerV6(
    val goldenRecordTaskLegacyServiceMapper: GoldenRecordTaskLegacyServiceMapper,
    val apiConfigProperties: ApiConfigProperties,
    val goldenRecordTaskCreateApplicationService: GoldenRecordTaskCreateApplicationV6Service
) : OrchestratorGoldenRecordTaskV6Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.CREATE_TASK})")
    override fun createTasks(createRequest: TaskCreateV6Request): TaskCreateV6Response {
        if (createRequest.requests.size > apiConfigProperties.upsertLimit)
            throw BpdmUpsertLimitException(createRequest.requests.size, apiConfigProperties.upsertLimit)

        return goldenRecordTaskCreateApplicationService.createTasks(createRequest)
    }

    @PreAuthorize("@stepSecurityService.assertHasReservationAuthority(authentication, #reservationRequest.step)")
    override fun reserveTasksForStep(reservationRequest: TaskStepReservationV6Request): TaskStepReservationV6Response {
        if (reservationRequest.amount > apiConfigProperties.upsertLimit)
            throw BpdmUpsertLimitException(reservationRequest.amount, apiConfigProperties.upsertLimit)

        return goldenRecordTaskLegacyServiceMapper.reserveTasksForStep(reservationRequest)
    }

    @PreAuthorize("@stepSecurityService.assertHasResultAuthority(authentication, #resultRequest.step)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun resolveStepResults(resultRequest: TaskStepResultV6Request) {
        if (resultRequest.results.size > apiConfigProperties.upsertLimit)
            throw BpdmUpsertLimitException(resultRequest.results.size, apiConfigProperties.upsertLimit)

        goldenRecordTaskLegacyServiceMapper.resolveStepResults(resultRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.VIEW_TASK})")
    override fun searchTaskStates(stateRequest: TaskStateV6Request): TaskStateV6Response {
        return goldenRecordTaskLegacyServiceMapper.searchTaskStates(stateRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.VIEW_TASK})")
    override fun searchTaskResultStates(stateRequest: TaskResultStateSearchV6Request): TaskResultStateSearchV6Response {
        return goldenRecordTaskLegacyServiceMapper.searchTaskResultStates(stateRequest)
    }
}
