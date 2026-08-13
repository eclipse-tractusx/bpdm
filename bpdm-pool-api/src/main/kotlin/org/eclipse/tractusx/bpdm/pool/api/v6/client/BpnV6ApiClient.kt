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

package org.eclipse.tractusx.bpdm.pool.api.v6.client

import org.eclipse.tractusx.bpdm.common.util.CommonApiPathNames
import org.eclipse.tractusx.bpdm.pool.api.ApiCommons
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolBpnV6Api
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.BpnRequestIdentifierSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.IdentifiersSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.BpnIdentifierMappingDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.BpnRequestIdentifierMappingDtoV6
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange
interface BpnV6ApiClient : PoolBpnV6Api {

    @PostExchange(value = "${ApiCommons.BPN_BASE_PATH_V6}${CommonApiPathNames.SUBPATH_SEARCH}")
    override fun findBpnsByIdentifiers(@RequestBody request: IdentifiersSearchRequestV6): ResponseEntity<Set<BpnIdentifierMappingDtoV6>>

    @PostExchange(value = "${ApiCommons.BPN_BASE_PATH_V6}/request-ids/search")
    override fun findBpnByRequestedIdentifiers(@RequestBody request: BpnRequestIdentifierSearchRequestV6): ResponseEntity<Set<BpnRequestIdentifierMappingDtoV6>>
}