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

package org.eclipse.tractusx.bpdm.pool.controller

import org.eclipse.tractusx.bpdm.pool.api.PoolBpnApi
import org.eclipse.tractusx.bpdm.pool.api.model.request.BpnRequestIdentifierSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.IdentifiersSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.BpnIdentifierMappingDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.BpnRequestIdentifierMappingDto
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.service.application.v7.BpnSearchApplicationV7Service
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class BpnController(
    private val bpnSearchApplicationService: BpnSearchApplicationV7Service
) : PoolBpnApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun findBpnsByIdentifiers(@RequestBody request: IdentifiersSearchRequest): ResponseEntity<Set<BpnIdentifierMappingDto>> {
        return ResponseEntity(bpnSearchApplicationService.searchBpnsByIdentifiers(request), HttpStatus.OK)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun findBpnByRequestedIdentifiers(@RequestBody request: BpnRequestIdentifierSearchRequest): ResponseEntity<Set<BpnRequestIdentifierMappingDto>> {
        return ResponseEntity(bpnSearchApplicationService.searchBpnsByRequestedIdentifiers(request), HttpStatus.OK)
    }
}
