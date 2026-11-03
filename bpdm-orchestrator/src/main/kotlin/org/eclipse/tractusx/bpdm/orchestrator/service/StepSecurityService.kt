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

package org.eclipse.tractusx.bpdm.orchestrator.service

import org.eclipse.tractusx.bpdm.orchestrator.mapper.v6.TaskV6Mapper.toTaskStep
import org.eclipse.tractusx.bpdm.orchestrator.service.operation.StepSecurityOperation
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepV6
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

@Service
class StepSecurityService(
    private val securityOperation: StepSecurityOperation
) {

    //Is being used by Pre-Authorize annotations
    @Suppress("unused")
    fun assertHasReservationAuthority(authentication: Authentication, step: TaskStep) {
        securityOperation.assertHasReservationAuthority(authentication, step)
    }

    //Is being used by Pre-Authorize annotations
    @Suppress("unused")
    fun assertHasResultAuthority(authentication: Authentication, step: TaskStep) {
        securityOperation.assertHasResultAuthority(authentication, step)
    }

    //Is being used by Pre-Authorize annotations for V6 API
    @Suppress("unused")
    fun assertHasReservationAuthority(authentication: Authentication, step: TaskStepV6) {
        securityOperation.assertHasReservationAuthority(authentication, step.toTaskStep())
    }

    //Is being used by Pre-Authorize annotations for V6 API
    @Suppress("unused")
    fun assertHasResultAuthority(authentication: Authentication, step: TaskStepV6) {
        securityOperation.assertHasResultAuthority(authentication, step.toTaskStep())
    }

}
