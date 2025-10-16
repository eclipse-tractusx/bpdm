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
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolLegalEntityApi
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.v6.IsPoolV6Test
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test

interface LegalEntityApiAuthV6Test: PoolLegalEntityApi, IsPoolV6Test {

    val expectationGetLegalEntities: AuthExpectationType
    val expectationGetLegalEntity: AuthExpectationType
    val expectationPostLegalEntitySearch: AuthExpectationType
    val expectationGetSites: AuthExpectationType
    val expectationGetAddresses: AuthExpectationType
    val expectationCreateBusinessPartners: AuthExpectationType
    val expectationUpdateBusinessPartners: AuthExpectationType

    @Test
    fun getLegalEntities(){
        authAssertionHelper.assert(expectationGetLegalEntities){ getLegalEntities(LegalEntitySearchRequest(), PaginationRequest()) }
    }

    override fun getLegalEntities(
        searchRequest: LegalEntitySearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseDto> {
       return poolClient.legalEntities.getLegalEntities(searchRequest, paginationRequest)
    }

    @Test
    fun getLegalEntity(){
        authAssertionHelper.assert(expectationGetLegalEntity){ getLegalEntity("any", "any") }
    }

    override fun getLegalEntity(
        idValue: String,
        idType: String?
    ): LegalEntityWithLegalAddressVerboseDto {
       return poolClient.legalEntities.getLegalEntity(idValue, idType)
    }

    @Test
    fun postLegalEntitySearch(){
        authAssertionHelper.assert(expectationPostLegalEntitySearch){ postLegalEntitySearch(LegalEntitySearchRequest(), PaginationRequest()) }
    }

    override fun postLegalEntitySearch(
        searchRequest: LegalEntitySearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseDto> {
        return poolClient.legalEntities.postLegalEntitySearch(searchRequest, paginationRequest)
    }

    @Test
    fun getSites(){
        authAssertionHelper.assert(expectationGetSites){ getSites("any", PaginationRequest()) }
    }

    override fun getSites(
        bpnl: String,
        paginationRequest: PaginationRequest
    ): PageDto<SiteVerboseDto> {
        return poolClient.legalEntities.getSites(bpnl, paginationRequest)
    }

    @Test
    fun getAddresses(){
        authAssertionHelper.assert(expectationGetAddresses){ getAddresses("any", PaginationRequest()) }
    }

    override fun getAddresses(
        bpnl: String,
        paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseDto> {
        return poolClient.legalEntities.getAddresses(bpnl, paginationRequest)
    }

    @Test
    fun createBusinessPartners(){
        val createRequest = testDataFactory.request.buildLegalEntityCreateRequest("any")
        authAssertionHelper.assert(expectationCreateBusinessPartners){ createBusinessPartners(listOf(createRequest)) }
    }

    override fun createBusinessPartners(businessPartners: Collection<LegalEntityPartnerCreateRequest>): LegalEntityPartnerCreateResponseWrapper {
        return poolClient.legalEntities.createBusinessPartners(businessPartners)
    }

    @Test
    fun updateBusinessPartners(){
        val updateRequest = testDataFactory.request.createLegalEntityUpdateRequest("any", "any")
        authAssertionHelper.assert(expectationCreateBusinessPartners){ updateBusinessPartners(listOf(updateRequest)) }
    }

    override fun updateBusinessPartners(businessPartners: Collection<LegalEntityPartnerUpdateRequest>): LegalEntityPartnerUpdateResponseWrapper {
       return poolClient.legalEntities.updateBusinessPartners(businessPartners)
    }
}