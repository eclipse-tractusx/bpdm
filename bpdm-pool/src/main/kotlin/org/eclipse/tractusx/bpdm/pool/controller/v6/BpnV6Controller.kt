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

package org.eclipse.tractusx.bpdm.pool.controller.v6

import org.eclipse.tractusx.bpdm.pool.api.v6.PoolBpnV6Api
import org.eclipse.tractusx.bpdm.pool.api.v6.model.bpn.BpnIdentifierMappingV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.bpn.BpnRequestIdentifierMappingV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.bpn.BpnRequestIdentifierSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.bpn.IdentifiersSearchV6Request
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.mapper.apiv6.ApiV6BpnMapper
import org.eclipse.tractusx.bpdm.pool.mapper.apiv6.ApiV6InboundMapper
import org.eclipse.tractusx.bpdm.pool.model.output.bpn.BpnIdentifierSearchResult
import org.eclipse.tractusx.bpdm.pool.service.application.BpnIdentifierSearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.v6.BpnSearchApplicationV6Service
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController("BpnControllerLegacy")
class BpnV6Controller(
    private val bpnIdentifierSearchApplicationService: BpnIdentifierSearchApplicationService,
    private val bpnSearchApplicationService: BpnSearchApplicationV6Service,
    private val apiV6InboundMapper: ApiV6InboundMapper,
    private val apiV6BpnMapper: ApiV6BpnMapper
) : PoolBpnV6Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun findBpnsByIdentifiers(@RequestBody request: IdentifiersSearchV6Request): ResponseEntity<Set<BpnIdentifierMappingV6Dto>> {
        val searchRequest = apiV6InboundMapper.toBpnIdentifierSearchRaw(request)
        return when (val result = bpnIdentifierSearchApplicationService.search(searchRequest)) {
            is BpnIdentifierSearchResult.Found -> ResponseEntity(result.matches.map(apiV6BpnMapper::toDto).toSet(), HttpStatus.OK)
            is BpnIdentifierSearchResult.Rejected -> throw apiV6BpnMapper.toException(result.errors)
        }
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun findBpnByRequestedIdentifiers(@RequestBody request: BpnRequestIdentifierSearchV6Request): ResponseEntity<Set<BpnRequestIdentifierMappingV6Dto>> {
        return ResponseEntity(bpnSearchApplicationService.searchBpnsByRequestedIdentifiers(request), HttpStatus.OK)
    }
}
