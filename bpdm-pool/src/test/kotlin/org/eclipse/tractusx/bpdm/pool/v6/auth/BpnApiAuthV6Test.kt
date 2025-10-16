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

package org.eclipse.tractusx.bpdm.pool.v6.auth

import org.eclipse.tractusx.bpdm.pool.api.PoolBpnApi
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.request.BpnRequestIdentifierSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.IdentifiersSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.BpnIdentifierMappingDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.BpnRequestIdentifierMappingDto
import org.eclipse.tractusx.bpdm.pool.v6.IsPoolV6Test
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity

interface BpnApiAuthV6Test: PoolBpnApi, IsPoolV6Test {

    val expectationFindBpnsByIdentifiers: AuthExpectationType
    val expectationFindBpnByRequestedIdentifiers: AuthExpectationType


    @Test
    fun findBpnsByIdentifiers(){
        val searchRequest = IdentifiersSearchRequest(IdentifierBusinessPartnerType.LEGAL_ENTITY, "any", emptyList())
        return authAssertionHelper.assert(expectationFindBpnsByIdentifiers){ findBpnsByIdentifiers(searchRequest) }
    }

    override fun findBpnsByIdentifiers(request: IdentifiersSearchRequest): ResponseEntity<Set<BpnIdentifierMappingDto>> {
        return poolClient.bpns.findBpnsByIdentifiers(request)
    }

    @Test
    fun findBpnByRequestedIdentifiers(){
        val searchRequest = BpnRequestIdentifierSearchRequest(emptySet())
        return authAssertionHelper.assert(expectationFindBpnByRequestedIdentifiers){ findBpnByRequestedIdentifiers(searchRequest) }
    }

    override fun findBpnByRequestedIdentifiers(request: BpnRequestIdentifierSearchRequest): ResponseEntity<Set<BpnRequestIdentifierMappingDto>> {
        return poolClient.bpns.findBpnByRequestedIdentifiers(request)
    }
}