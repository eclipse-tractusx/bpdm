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
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteCreateRequestWithLegalAddressAsMain
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolSiteApi
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.v6.IsPoolV6Test
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test

interface SiteApiAuthV6Test: PoolSiteApi, IsPoolV6Test {

    val expectationGetSite: AuthExpectationType
    val expectationPostSiteSearch: AuthExpectationType
    val expectationCreateSite: AuthExpectationType
    val expectationUpdateSite: AuthExpectationType
    val expectationGetSites: AuthExpectationType
    val expectationCreateSiteWithLegalReference: AuthExpectationType

    @Test
    fun getSite(){
        authAssertionHelper.assert(expectationGetSite){ getSite("any") }
    }

    override fun getSite(bpns: String): SiteWithMainAddressVerboseDto {
        return poolClient.sites.getSite(bpns)
    }

    @Test
    fun postSiteSearch(){
        authAssertionHelper.assert(expectationPostSiteSearch){ postSiteSearch(SiteSearchRequest(), PaginationRequest()) }
    }

    override fun postSiteSearch(
        searchRequest: SiteSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseDto> {
        return poolClient.sites.postSiteSearch(searchRequest, paginationRequest)
    }

    @Test
    fun createSite(){
        val createRequest = testDataFactory.request.buildSiteCreateRequest("any", "any")
        authAssertionHelper.assert(expectationCreateSite){ createSite(listOf(createRequest)) }
    }

    override fun createSite(requests: Collection<SitePartnerCreateRequest>): SitePartnerCreateResponseWrapper {
        return poolClient.sites.createSite(requests)
    }

    @Test
    fun updateSite(){
        val updateRequest = testDataFactory.request.createSiteUpdateRequest("any", "any")
        authAssertionHelper.assert(expectationUpdateSite){ updateSite(listOf(updateRequest)) }
    }

    override fun updateSite(requests: Collection<SitePartnerUpdateRequest>): SitePartnerUpdateResponseWrapper {
        return poolClient.sites.updateSite(requests)
    }

    @Test
    fun getSites(){
        authAssertionHelper.assert(expectationGetSites){ getSites(SiteSearchRequest(), PaginationRequest()) }
    }

    override fun getSites(
        searchRequest: SiteSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseDto> {
        return poolClient.sites.getSites(searchRequest, paginationRequest)
    }

    @Test
    fun createSiteWithLegalReference(){
        val createRequest = testDataFactory.request.buildLegalAddressSiteCreateRequest("any", "any")
        authAssertionHelper.assert(expectationCreateSiteWithLegalReference){ createSiteWithLegalReference(listOf(createRequest)) }
    }

    override fun createSiteWithLegalReference(request: Collection<SiteCreateRequestWithLegalAddressAsMain>): SitePartnerCreateResponseWrapper {
        return poolClient.sites.createSiteWithLegalReference(request)
    }
}