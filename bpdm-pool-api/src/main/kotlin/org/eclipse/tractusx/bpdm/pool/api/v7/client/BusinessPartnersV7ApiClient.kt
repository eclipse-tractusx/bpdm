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

package org.eclipse.tractusx.bpdm.pool.api.v7.client

import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.v7.PoolBusinessPartnerV7Api
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchFilterTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchResultV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPropertiesSearchV7Request
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange(PoolBusinessPartnerV7Api.BUSINESS_PARTNERS_PATH)
interface BusinessPartnersV7ApiClient: PoolBusinessPartnerV7Api {

    @PostExchange(value ="/search")
    override fun searchBusinessPartners(
        @RequestBody searchRequest: LegalEntityPropertiesSearchV7Request,
        @RequestParam searchResultFilter: Set<BusinessPartnerSearchFilterTypeV7>?,
        @ParameterObject @Valid paginationRequest: PaginationRequest
    ): PageDto<BusinessPartnerSearchResultV7Dto>

}