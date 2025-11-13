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

import org.eclipse.tractusx.bpdm.orchestrator.config.PermissionConfigProperties
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

@Service
class StepSecurityService(
    private val permissionConfigProperties: PermissionConfigProperties
) {

    private val defaultPermissions = PermissionConfigProperties()


    //Is being used by Pre-Authorize annotations
    @Suppress("unused")
    fun assertHasReservationAuthority(authentication: Authentication, step: TaskStep){
        val authorities = authentication.authorities.map { it.authority.uppercase() }

        val expectedAuthority = permissionConfigProperties.reservation[step]
            ?: defaultPermissions.reservation[step]

        expectedAuthority?.uppercase().takeIf { it in authorities }
            ?: throw AccessDeniedException("Insufficient permissions")
    }

    //Is being used by Pre-Authorize annotations
    @Suppress("unused")
    fun assertHasResultAuthority(authentication: Authentication, step: TaskStep){
        val authorities = authentication.authorities.map { it.authority.uppercase() }

        val expectedAuthority = permissionConfigProperties.result[step]
            ?: defaultPermissions.result[step]

        expectedAuthority?.uppercase().takeIf { it in authorities }
            ?: throw AccessDeniedException("Insufficient permissions")
    }
}