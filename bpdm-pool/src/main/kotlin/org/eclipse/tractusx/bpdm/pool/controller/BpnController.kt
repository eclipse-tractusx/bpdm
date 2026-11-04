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

import org.eclipse.tractusx.bpdm.pool.api.v7.PoolBpnV7Api
import org.eclipse.tractusx.bpdm.pool.api.v7.model.bpn.BpnIdentifierMappingV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.bpn.BpnRequestIdentifierMappingV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.bpn.BpnRequestIdentifierSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.bpn.IdentifiersSearchV7Request
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.mapper.apiv7.ApiV7BpnMapper
import org.eclipse.tractusx.bpdm.pool.mapper.apiv7.ApiV7InboundMapper
import org.eclipse.tractusx.bpdm.pool.model.output.bpn.BpnIdentifierSearchResult
import org.eclipse.tractusx.bpdm.pool.model.output.bpn.BpnRequestIdentifierSearchResult
import org.eclipse.tractusx.bpdm.pool.service.application.BpnIdentifierSearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.BpnRequestIdentifierSearchApplicationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class BpnController(
    private val bpnIdentifierSearchApplicationService: BpnIdentifierSearchApplicationService,
    private val bpnRequestIdentifierSearchApplicationService: BpnRequestIdentifierSearchApplicationService,
    private val apiV7InboundMapper: ApiV7InboundMapper,
    private val apiV7BpnMapper: ApiV7BpnMapper
) : PoolBpnV7Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun findBpnsByIdentifiers(@RequestBody request: IdentifiersSearchV7Request): ResponseEntity<Set<BpnIdentifierMappingV7Dto>> {
        val searchRequest = apiV7InboundMapper.toBpnIdentifierSearchRaw(request)
        return when (val result = bpnIdentifierSearchApplicationService.search(searchRequest)) {
            is BpnIdentifierSearchResult.Found -> ResponseEntity(result.matches.map(apiV7BpnMapper::toDto).toSet(), HttpStatus.OK)
            is BpnIdentifierSearchResult.Rejected -> throw apiV7BpnMapper.toException(result.errors)
        }
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun findBpnByRequestedIdentifiers(@RequestBody request: BpnRequestIdentifierSearchV7Request): ResponseEntity<Set<BpnRequestIdentifierMappingV7Dto>> {
        val searchRequest = apiV7InboundMapper.toBpnRequestIdentifierSearchRaw(request)
        return when (val result = bpnRequestIdentifierSearchApplicationService.search(searchRequest)) {
            is BpnRequestIdentifierSearchResult.Found -> ResponseEntity(result.matches.map(apiV7BpnMapper::toDto).toSet(), HttpStatus.OK)
            is BpnRequestIdentifierSearchResult.Rejected -> throw apiV7BpnMapper.toRequestIdentifierSearchException(result.errors)
        }
    }
}
