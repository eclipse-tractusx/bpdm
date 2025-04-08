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

package org.eclipse.tractusx.bpdm.gate.api.client

import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.ApiCommons
import org.eclipse.tractusx.bpdm.gate.api.GateBusinessPartnerApi
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

@HttpExchange
interface BusinessPartnerApiClient : GateBusinessPartnerApi {
    @PutExchange(value = "${ApiCommons.BASE_PATH_V7}/input/business-partners")
    override fun upsertBusinessPartnersInput(
        @RequestBody businessPartners: Collection<BusinessPartnerInputRequest>
    ): ResponseEntity<Collection<BusinessPartnerInputDto>>

    @PostExchange(value = "${ApiCommons.BASE_PATH_V7}/input/business-partners/search")
    override fun getBusinessPartnersInput(
        @RequestBody externalIds: Collection<String>?,
        @ParameterObject @Valid paginationRequest: PaginationRequest
    ): PageDto<BusinessPartnerInputDto>

    @PostExchange(value = "${ApiCommons.BASE_PATH_V7}/output/business-partners/search")
    override fun getBusinessPartnersOutput(
        @RequestBody externalIds: Collection<String>?,
        @ParameterObject @Valid paginationRequest: PaginationRequest
    ): PageDto<BusinessPartnerOutputDto>
}