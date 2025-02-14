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

package org.eclipse.tractusx.bpdm.orchestrator.controller

import org.eclipse.tractusx.bpdm.orchestrator.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.service.OriginRegistrarService
import org.eclipse.tractusx.orchestrator.api.OriginatorRegistrarApi
import org.eclipse.tractusx.orchestrator.api.model.UpsertOriginRequest
import org.eclipse.tractusx.orchestrator.api.model.UpsertOriginResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class OriginRegistrarController(
    private val originRegistrarService: OriginRegistrarService
): OriginatorRegistrarApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.CREATE_TASK})")
    override fun registerOrigin(request: UpsertOriginRequest): UpsertOriginResponse {
        return originRegistrarService.registerOrigin(request)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.VIEW_TASK})")
    override fun fetchOrigin(originId: String): UpsertOriginResponse {
        return originRegistrarService.fetchOrigin(originId)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.CREATE_TASK})")
    override fun updateOrigin(originId: String, request: UpsertOriginRequest): UpsertOriginResponse {
        return originRegistrarService.updateOrigin(originId,request)
    }
}