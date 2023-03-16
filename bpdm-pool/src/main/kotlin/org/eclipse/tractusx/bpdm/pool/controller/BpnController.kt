/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.pool.api.model.request.IdentifiersSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.BpnIdentifierMappingResponse
import org.eclipse.tractusx.bpdm.pool.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerFetchService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class BpnController(
    val businessPartnerFetchService: BusinessPartnerFetchService,
    val bpnConfigProperties: BpnConfigProperties
) : PoolBpnApi {

    override fun findBpnsByIdentifiers(identifiersSearchRequest: IdentifiersSearchRequest): ResponseEntity<Set<BpnIdentifierMappingResponse>> {
        if (identifiersSearchRequest.idValues.size > bpnConfigProperties.searchRequestLimit) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val bpnIdentifierMappings = businessPartnerFetchService.findBpnsByIdentifiers(identifiersSearchRequest.idType, identifiersSearchRequest.idValues)
        return ResponseEntity(bpnIdentifierMappings, HttpStatus.OK)
    }
}