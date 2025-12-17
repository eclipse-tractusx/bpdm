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

package org.eclipse.tractusx.bpdm.gate.v6.auth

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.v6.client.BusinessPartnerApiClientV6
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType.Authorized
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType.Forbidden
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity

class BusinessPartnerAuthV6IT: GateAuthV6Test(), BusinessPartnerApiClientV6 {

    @Test
    fun upsertBusinessPartnersInput(){
        assertExpectations(
            inputManager = Authorized,
            inputConsumer = Forbidden,
            outputConsumer = Forbidden
        ){ upsertBusinessPartnersInput(emptyList()) }
    }

    @Test
    fun getBusinessPartnersInput(){
        assertExpectations(
            inputManager = Authorized,
            inputConsumer = Authorized,
            outputConsumer = Forbidden
        ){ getBusinessPartnersInput(emptyList()) }
    }

    @Test
    fun getBusinessPartnersOutput(){
        assertExpectations(
            inputManager = Forbidden,
            inputConsumer = Forbidden,
            outputConsumer = Authorized
        ){ getBusinessPartnersOutput(emptyList()) }
    }

    override fun upsertBusinessPartnersInput(businessPartners: Collection<BusinessPartnerInputRequest>): ResponseEntity<Collection<BusinessPartnerInputDto>> {
        return gateClient.businessPartners.upsertBusinessPartnersInput(businessPartners)
    }

    override fun getBusinessPartnersInput(
        externalIds: Collection<String>?,
        paginationRequest: PaginationRequest
    ): PageDto<BusinessPartnerInputDto> {
        return gateClient.businessPartners.getBusinessPartnersInput(externalIds, paginationRequest)
    }

    override fun getBusinessPartnersOutput(
        externalIds: Collection<String>?,
        paginationRequest: PaginationRequest
    ): PageDto<BusinessPartnerOutputDto> {
        return gateClient.businessPartners.getBusinessPartnersOutput(externalIds, paginationRequest)
    }
}