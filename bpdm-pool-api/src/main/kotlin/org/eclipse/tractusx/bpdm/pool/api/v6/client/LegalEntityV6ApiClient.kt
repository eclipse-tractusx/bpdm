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

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.ApiCommons
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolLegalEntityV6Api
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.LogisticAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerUpdateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntitySearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityWithLegalAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteVerboseV6Dto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

@HttpExchange
interface LegalEntityV6ApiClient: PoolLegalEntityV6Api {
    @PostExchange(value = ApiCommons.LEGAL_ENTITY_BASE_PATH_V6)
    override fun createBusinessPartners(
        @RequestBody businessPartners: Collection<LegalEntityPartnerCreateV6Request>
    ): LegalEntityPartnerCreateV6ResponseWrapper

    @GetExchange(value = ApiCommons.LEGAL_ENTITY_BASE_PATH_V6)
    override fun getLegalEntities(
        @ParameterObject searchRequest: LegalEntitySearchV6Request,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseV6Dto>

    @PutExchange(value = ApiCommons.LEGAL_ENTITY_BASE_PATH_V6)
    override fun updateBusinessPartners(
        @RequestBody businessPartners: Collection<LegalEntityPartnerUpdateV6Request>
    ): LegalEntityPartnerUpdateV6ResponseWrapper

    @GetExchange(value = "${ApiCommons.LEGAL_ENTITY_BASE_PATH_V6}/{idValue}")
    override fun getLegalEntity(
        @Parameter(description = "Identifier value") @PathVariable("idValue") idValue: String,
        @Parameter(description = "Type of identifier to use, defaults to BPN when omitted", schema = Schema(defaultValue = "BPN"))
        @RequestParam idType: String?
    ): LegalEntityWithLegalAddressVerboseV6Dto

    @PostExchange(value = "${ApiCommons.LEGAL_ENTITY_BASE_PATH_V6}/search")
    override fun postLegalEntitySearch(
        @RequestBody searchRequest: LegalEntitySearchV6Request,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseV6Dto>

    @GetExchange(value = "${ApiCommons.LEGAL_ENTITY_BASE_PATH_V6}/{bpnl}/sites")
    override fun getSites(
        @Parameter(description = "BPNL value") @PathVariable bpnl: String,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<SiteVerboseV6Dto>

    @GetExchange(value = "${ApiCommons.LEGAL_ENTITY_BASE_PATH_V6}/{bpnl}/addresses")
    override fun getAddresses(
        @Parameter(description = "BPNL value") @PathVariable bpnl: String,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseV6Dto>
}