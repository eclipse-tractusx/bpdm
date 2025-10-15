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

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ChangelogEntryVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolMembersApi
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.v6.IsPoolV6Test
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test

interface MembersApiAuthV6Test: PoolMembersApi, IsPoolV6Test {

    val expectationSearchLegalEntities: AuthExpectationType
    val expectationPostSiteSearch: AuthExpectationType
    val expectationSearchAddresses: AuthExpectationType
    val expectationSearchChangelogEntries: AuthExpectationType

    @Test
    fun searchLegalEntities(){
        authAssertionHelper.assert(expectationSearchLegalEntities){ searchLegalEntities(LegalEntitySearchRequest(), PaginationRequest())}
    }

    override fun searchLegalEntities(
        searchRequest: LegalEntitySearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseDto> {
        return poolClient.members.searchLegalEntities(searchRequest, paginationRequest)
    }

    @Test
    fun postSiteSearch(){
        authAssertionHelper.assert(expectationPostSiteSearch){ postSiteSearch(SiteSearchRequest(), PaginationRequest())}
    }

    override fun postSiteSearch(
        searchRequest: SiteSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseDto> {
        return poolClient.members.postSiteSearch(searchRequest, paginationRequest)
    }

    @Test
    fun searchAddresses(){
        authAssertionHelper.assert(expectationSearchLegalEntities){ searchAddresses(AddressSearchRequest(), PaginationRequest())}
    }

    override fun searchAddresses(
        searchRequest: AddressSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseDto> {
        return poolClient.members.searchAddresses(searchRequest, paginationRequest)
    }

    @Test
    fun searchChangelogEntries(){
        authAssertionHelper.assert(expectationSearchChangelogEntries){ searchChangelogEntries(ChangelogSearchRequest(), PaginationRequest())}
    }

    override fun searchChangelogEntries(
        changelogSearchRequest: ChangelogSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<ChangelogEntryVerboseDto> {
        return poolClient.members.searchChangelogEntries(changelogSearchRequest, paginationRequest)
    }
}