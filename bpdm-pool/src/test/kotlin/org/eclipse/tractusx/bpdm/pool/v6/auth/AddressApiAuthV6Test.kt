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
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolAddressApi
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.v6.IsPoolV6Test
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test

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


interface AddressApiAuthV6Test: PoolAddressApi, IsPoolV6Test {

    val expectationGetAddresses: AuthExpectationType
    val expectationGetAddress: AuthExpectationType
    val expectationSearchAddresses: AuthExpectationType
    val expectationCreateAddresses: AuthExpectationType
    val expectationUpdateAddresses: AuthExpectationType

    @Test
    fun getAddresses(){
        authAssertionHelper.assert(expectationGetAddresses){ getAddresses(AddressSearchRequest(), PaginationRequest()) }
    }

    override fun getAddresses(
        addressSearchRequest: AddressSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseDto> {
        return poolClient.addresses.getAddresses(addressSearchRequest, paginationRequest)
    }

    @Test
    fun getAddress(){
        authAssertionHelper.assert(expectationGetAddress){ getAddress("BPNA") }
    }

    override fun getAddress(bpna: String): LogisticAddressVerboseDto {
        return poolClient.addresses.getAddress(bpna)
    }

    @Test
    fun searchAddresses(){
        authAssertionHelper.assert(expectationSearchAddresses){ searchAddresses(AddressSearchRequest(), PaginationRequest()) }
    }

    override fun searchAddresses(
        searchRequest: AddressSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseDto> {
        return poolClient.addresses.searchAddresses(searchRequest, paginationRequest)
    }

    @Test
    fun createAddresses(){
        val createRequest = testDataFactory.request.buildAdditionalAddressCreateRequest("seed", "any")
        authAssertionHelper.assert(expectationCreateAddresses){ createAddresses(listOf(createRequest)) }
    }

    override fun createAddresses(requests: Collection<AddressPartnerCreateRequest>): AddressPartnerCreateResponseWrapper {
        return poolClient.addresses.createAddresses(requests)
    }

    @Test
    fun updateAddresses(){
        val updateRequest = testDataFactory.request.buildAddressUpdateRequest("seed", "any")
        authAssertionHelper.assert(expectationUpdateAddresses){ updateAddresses(listOf(updateRequest)) }
    }

    override fun updateAddresses(requests: Collection<AddressPartnerUpdateRequest>): AddressPartnerUpdateResponseWrapper {
        return poolClient.addresses.updateAddresses(requests)
    }


}