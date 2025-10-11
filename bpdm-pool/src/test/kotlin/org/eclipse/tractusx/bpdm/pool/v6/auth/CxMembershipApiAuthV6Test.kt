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

package org.eclipse.tractusx.bpdm.pool.v6.auth

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolCxMembershipApi
import org.eclipse.tractusx.bpdm.pool.api.v6.model.CxMembershipDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.CxMembershipSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.CxMembershipUpdateRequest
import org.eclipse.tractusx.bpdm.pool.v6.IsPoolV6Test
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test

interface CxMembershipApiAuthV6Test: PoolCxMembershipApi, IsPoolV6Test {

    val expectationGet: AuthExpectationType
    val expectationPut: AuthExpectationType

    @Test
    fun get(){
        authAssertionHelper.assert(expectationGet){ get(CxMembershipSearchRequest(), PaginationRequest()) }
    }

    override fun get(searchRequest: CxMembershipSearchRequest, paginationRequest: PaginationRequest): PageDto<CxMembershipDto> {
        return poolClient.memberships.get(searchRequest, paginationRequest)
    }

    @Test
    fun put(){
        authAssertionHelper.assert(expectationPut){ put(CxMembershipUpdateRequest(emptyList())) }
    }

    override fun put(updateRequest: CxMembershipUpdateRequest) {
        return poolClient.memberships.put(updateRequest)
    }
}