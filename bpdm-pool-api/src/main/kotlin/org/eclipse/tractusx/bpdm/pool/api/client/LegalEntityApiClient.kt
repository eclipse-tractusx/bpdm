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

package org.eclipse.tractusx.bpdm.pool.api.client

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.PoolLegalEntityApi
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

@HttpExchange("/legal-entities")
interface LegalEntityApiClient : PoolLegalEntityApi {
    @PostExchange
    override fun createBusinessPartners(
        @RequestBody businessPartners: Collection<LegalEntityPartnerCreateRequest>
    ): LegalEntityPartnerCreateResponseWrapper

    @GetExchange("/{bpnl}/addresses")
    override fun getAddresses(
        @PathVariable bpnl: String,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseDto>

    @GetExchange
    override fun getLegalEntities(
        @ParameterObject bpSearchRequest: LegalEntityPropertiesSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LegalEntityMatchVerboseDto>

    @GetExchange("/{idValue}")
    override fun getLegalEntity(
        @PathVariable idValue: String,
        @Parameter(description = "Type of identifier to use, defaults to BPN when omitted", schema = Schema(defaultValue = "BPN"))
        @RequestParam idType: String?
    ): LegalEntityWithLegalAddressVerboseDto

    @GetExchange("/{bpnl}/sites")
    override fun getSites(
        @PathVariable bpnl: String,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<SiteVerboseDto>

    @PostExchange("/legal-addresses/search")
    override fun searchLegalAddresses(
        @RequestBody bpnLs: Collection<String>
    ): Collection<LegalAddressVerboseDto>

    @PostExchange("/search")
    override fun searchLegalEntitys(
        @RequestBody bpnLs: Collection<String>
    ): ResponseEntity<Collection<LegalEntityWithLegalAddressVerboseDto>>

    @PostExchange("/{bpnl}/confirm-up-to-date")
    override fun setLegalEntityCurrentness(
        @PathVariable bpnl: String
    )

    @PutExchange
    override fun updateBusinessPartners(
        @RequestBody
        businessPartners: Collection<LegalEntityPartnerUpdateRequest>
    ): LegalEntityPartnerUpdateResponseWrapper

}