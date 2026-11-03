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

package org.eclipse.tractusx.bpdm.pool.api.v7.client

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.ApiCommons
import org.eclipse.tractusx.bpdm.pool.api.v7.PoolLegalEntityV7Api
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerUpdateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntitySearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteVerboseV7Dto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

@HttpExchange
interface LegalEntityV7ApiClient : PoolLegalEntityV7Api {
    @PostExchange(value = ApiCommons.LEGAL_ENTITY_BASE_PATH_V7)
    override fun createBusinessPartners(
        @RequestBody businessPartners: Collection<LegalEntityPartnerCreateV7Request>
    ): LegalEntityPartnerCreateV7ResponseWrapper

    @GetExchange(value = "${ApiCommons.LEGAL_ENTITY_BASE_PATH_V7}/{bpnl}/addresses")
    override fun getAddresses(
        @PathVariable bpnl: String,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseV7Dto>

    @GetExchange(value = ApiCommons.LEGAL_ENTITY_BASE_PATH_V7)
    override fun getLegalEntities(
        @ParameterObject searchRequest: LegalEntitySearchV7Request,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseV7Dto>

    @GetExchange(value = "${ApiCommons.LEGAL_ENTITY_BASE_PATH_V7}/{idValue}")
    override fun getLegalEntity(
        @PathVariable idValue: String,
        @Parameter(description = "Type of identifier to use, defaults to BPN when omitted", schema = Schema(defaultValue = "BPN"))
        @RequestParam idType: String?
    ): LegalEntityWithLegalAddressVerboseV7Dto

    @GetExchange(value = "${ApiCommons.LEGAL_ENTITY_BASE_PATH_V7}/{bpnl}/sites")
    override fun getSites(
        @PathVariable bpnl: String,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<SiteVerboseV7Dto>

    @PostExchange(value = "${ApiCommons.LEGAL_ENTITY_BASE_PATH_V7}/search")
    override fun postLegalEntitySearch(
        @RequestBody searchRequest: LegalEntitySearchV7Request,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseV7Dto>

    @PutExchange(value = ApiCommons.LEGAL_ENTITY_BASE_PATH_V7)
    override fun updateBusinessPartners(
        @RequestBody
        businessPartners: Collection<LegalEntityPartnerUpdateV7Request>
    ): LegalEntityPartnerUpdateV7ResponseWrapper

}