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
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerCreateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerUpdateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntitySearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerCreateResponseWrapperV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerUpdateResponseWrapperV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityWithLegalAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.service.application.v6.*
import org.springdoc.core.annotations.ParameterObject
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController("LegalEntityControllerLegacy")
class LegalEntityV6Controller(
    val siteSearchApplicationService: SiteSearchApplicationV6Service,
    val addressSearchApplicationService: AddressSearchApplicationV6Service,
    val legalEntitySearchApplicationService: LegalEntitySearchApplicationV6Service,
    val legalEntityGetApplicationService: LegalEntityGetApplicationV6Service,
    val legalEntityCreateApplicationService: LegalEntityCreateApplicationV6Service,
    val legalEntityUpdateApplicationService: LegalEntityUpdateApplicationV6Service
) : PoolLegalEntityV6Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getLegalEntities(
        @ParameterObject searchRequest: LegalEntitySearchRequestV6,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseDtoV6> {
        return legalEntitySearchApplicationService.searchLegalEntities(searchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getLegalEntity(idValue: String, idType: String?): LegalEntityWithLegalAddressVerboseDtoV6 {
        return legalEntityGetApplicationService.getLegalEntity(idValue, idType)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun postLegalEntitySearch(
        searchRequest: LegalEntitySearchRequestV6,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseDtoV6> {
        return legalEntitySearchApplicationService.searchLegalEntities(searchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getSites(
        bpnl: String,
        paginationRequest: PaginationRequest
    ): PageDto<SiteVerboseDtoV6> {
        return siteSearchApplicationService.searchLegalEntitySites(bpnl, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getAddresses(
        bpnl: String,
        paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseDtoV6> {
        return addressSearchApplicationService.searchLegalEntityAddresses(bpnl, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun createBusinessPartners(
        businessPartners: Collection<LegalEntityPartnerCreateRequestV6>
    ): LegalEntityPartnerCreateResponseWrapperV6 {
        return legalEntityCreateApplicationService.createLegalEntities(businessPartners)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun updateBusinessPartners(
        businessPartners: Collection<LegalEntityPartnerUpdateRequestV6>
    ): LegalEntityPartnerUpdateResponseWrapperV6 {
        return legalEntityUpdateApplicationService.updateLegalEntities(businessPartners)
    }
}
