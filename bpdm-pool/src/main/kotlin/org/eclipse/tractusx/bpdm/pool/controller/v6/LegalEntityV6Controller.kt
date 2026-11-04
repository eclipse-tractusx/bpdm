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

package org.eclipse.tractusx.bpdm.pool.controller.v6

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolLegalEntityV6Api
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.LogisticAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerUpdateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntitySearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityWithLegalAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.mapper.apiv6.ApiV6InboundMapper
import org.eclipse.tractusx.bpdm.pool.mapper.apiv6.ApiV6LegalEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityGetResult
import org.eclipse.tractusx.bpdm.pool.service.application.LegalEntityGetApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.v6.*
import org.springdoc.core.annotations.ParameterObject
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController("LegalEntityControllerLegacy")
class LegalEntityV6Controller(
    val siteSearchApplicationService: SiteSearchApplicationV6Service,
    val addressSearchApplicationService: AddressSearchApplicationV6Service,
    val legalEntitySearchApplicationService: LegalEntitySearchApplicationV6Service,
    val legalEntityGetApplicationService: LegalEntityGetApplicationService,
    val legalEntityCreateApplicationService: LegalEntityCreateApplicationV6Service,
    val legalEntityUpdateApplicationService: LegalEntityUpdateApplicationV6Service,
    private val apiV6InboundMapper: ApiV6InboundMapper,
    private val apiV6LegalEntityMapper: ApiV6LegalEntityMapper
) : PoolLegalEntityV6Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getLegalEntities(
        @ParameterObject searchRequest: LegalEntitySearchV6Request,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseV6Dto> {
        return legalEntitySearchApplicationService.searchLegalEntities(searchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getLegalEntity(idValue: String, idType: String?): LegalEntityWithLegalAddressVerboseV6Dto =
        when (val result = legalEntityGetApplicationService.get(apiV6InboundMapper.toLegalEntityGetRaw(idValue, idType))) {
            is LegalEntityGetResult.Found -> apiV6LegalEntityMapper.toDto(result.legalEntity)
            is LegalEntityGetResult.Rejected -> throw apiV6LegalEntityMapper.toGetException(result.errors)
        }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun postLegalEntitySearch(
        searchRequest: LegalEntitySearchV6Request,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseV6Dto> {
        return legalEntitySearchApplicationService.searchLegalEntities(searchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getSites(
        bpnl: String,
        paginationRequest: PaginationRequest
    ): PageDto<SiteVerboseV6Dto> {
        return siteSearchApplicationService.searchLegalEntitySites(bpnl, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getAddresses(
        bpnl: String,
        paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseV6Dto> {
        return addressSearchApplicationService.searchLegalEntityAddresses(bpnl, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun createBusinessPartners(
        businessPartners: Collection<LegalEntityPartnerCreateV6Request>
    ): LegalEntityPartnerCreateV6ResponseWrapper {
        return legalEntityCreateApplicationService.createLegalEntities(businessPartners)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun updateBusinessPartners(
        businessPartners: Collection<LegalEntityPartnerUpdateV6Request>
    ): LegalEntityPartnerUpdateV6ResponseWrapper {
        return legalEntityUpdateApplicationService.updateLegalEntities(businessPartners)
    }
}
