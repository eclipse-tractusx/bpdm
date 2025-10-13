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
import org.eclipse.tractusx.bpdm.orchestrator.service.RelationsGoldenRecordTaskService
import org.eclipse.tractusx.orchestrator.api.RelationsGoldenRecordTaskApi
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class RelationsGoldenRecordTaskController(
    val apiConfigProperties: ApiConfigProperties,
    val relationsGoldenRecordTaskService: RelationsGoldenRecordTaskService
) : RelationsGoldenRecordTaskApi{

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.CREATE_TASK})")
    override fun createTasks(createRequest: TaskCreateRelationsRequest): TaskCreateRelationsResponse {
        if (createRequest.requests.size > apiConfigProperties.upsertLimit)
            throw BpdmUpsertLimitException(createRequest.requests.size, apiConfigProperties.upsertLimit)

        return relationsGoldenRecordTaskService.createTasks(createRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.VIEW_TASK})")
    override fun searchTaskStates(stateRequest: TaskStateRequest): TaskRelationsStateResponse {
        return relationsGoldenRecordTaskService.searchTaskStates(stateRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.VIEW_TASK})")
    override fun searchTaskResultStates(stateRequest: TaskResultStateSearchRequest): TaskResultStateSearchResponse {
        return relationsGoldenRecordTaskService.searchTaskResultStates(stateRequest)
    }

    @PreAuthorize("@stepSecurityService.assertHasReservationAuthority(authentication, #reservationRequest.step)")
    override fun reserveTasksForStep(reservationRequest: TaskStepReservationRequest): TaskRelationsStepReservationResponse {
        if (reservationRequest.amount > apiConfigProperties.upsertLimit)
            throw BpdmUpsertLimitException(reservationRequest.amount, apiConfigProperties.upsertLimit)

        return relationsGoldenRecordTaskService.reserveTasksForStep(reservationRequest)
    }

    @PreAuthorize("@stepSecurityService.assertHasResultAuthority(authentication, #resultRequest.step)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun resolveStepResults(resultRequest: TaskRelationsStepResultRequest) {
        if (resultRequest.results.size > apiConfigProperties.upsertLimit)
            throw BpdmUpsertLimitException(resultRequest.results.size, apiConfigProperties.upsertLimit)

        relationsGoldenRecordTaskService.resolveStepResults(resultRequest)
    }

}