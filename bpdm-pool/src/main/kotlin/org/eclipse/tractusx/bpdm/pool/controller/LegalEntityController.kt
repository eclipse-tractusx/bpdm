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

package org.eclipse.tractusx.bpdm.pool.controller

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.v7.PoolLegalEntityV7Api
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerUpdateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntitySearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.mapper.apiv7.ApiV7InboundMapper
import org.eclipse.tractusx.bpdm.pool.mapper.apiv7.ApiV7LegalEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityGetResult
import org.eclipse.tractusx.bpdm.pool.service.application.LegalEntityGetApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.v7.*
import org.springdoc.core.annotations.ParameterObject
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class LegalEntityController(
    val siteSearchApplicationService: SiteSearchApplicationV7Service,
    val addressSearchApplicationService: AddressSearchApplicationV7Service,
    val legalEntitySearchApplicationService: LegalEntitySearchApplicationV7Service,
    val legalEntityGetApplicationService: LegalEntityGetApplicationService,
    val legalEntityCreateApplicationService: LegalEntityCreateApplicationV7Service,
    val legalEntityUpdateApplicationService: LegalEntityUpdateApplicationV7Service,
    private val apiV7InboundMapper: ApiV7InboundMapper,
    private val apiV7LegalEntityMapper: ApiV7LegalEntityMapper
) : PoolLegalEntityV7Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getLegalEntities(
        @ParameterObject searchRequest: LegalEntitySearchV7Request,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseV7Dto> {
        return legalEntitySearchApplicationService.searchLegalEntities(searchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getLegalEntity(idValue: String, idType: String?): LegalEntityWithLegalAddressVerboseV7Dto =
        when (val result = legalEntityGetApplicationService.get(apiV7InboundMapper.toLegalEntityGetRaw(idValue, idType))) {
            is LegalEntityGetResult.Found -> apiV7LegalEntityMapper.toDto(result.legalEntity)
            is LegalEntityGetResult.Rejected -> throw apiV7LegalEntityMapper.toGetException(result.errors)
        }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun postLegalEntitySearch(
        searchRequest: LegalEntitySearchV7Request,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseV7Dto> {
        return legalEntitySearchApplicationService.searchLegalEntities(searchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getSites(
        bpnl: String,
        paginationRequest: PaginationRequest
    ): PageDto<SiteVerboseV7Dto> {
        return siteSearchApplicationService.searchLegalEntitySites(bpnl, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getAddresses(
        bpnl: String,
        paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseV7Dto> {
        return addressSearchApplicationService.searchLegalEntityAddresses(bpnl, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun createBusinessPartners(
        businessPartners: Collection<LegalEntityPartnerCreateV7Request>
    ): LegalEntityPartnerCreateV7ResponseWrapper {
        return legalEntityCreateApplicationService.createLegalEntities(businessPartners)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun updateBusinessPartners(
        businessPartners: Collection<LegalEntityPartnerUpdateV7Request>
    ): LegalEntityPartnerUpdateV7ResponseWrapper {
        return legalEntityUpdateApplicationService.updateLegalEntities(businessPartners)
    }
}
