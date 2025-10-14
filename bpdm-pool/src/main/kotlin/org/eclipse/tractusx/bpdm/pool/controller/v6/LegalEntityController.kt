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
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolLegalEntityApi
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.springdoc.core.annotations.ParameterObject
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController("LegalEntityControllerLegacy")
class LegalEntityController(
    val legalEntityLegacyServiceMapper: LegalEntityLegacyServiceMapper,
    val bpnConfigProperties: BpnConfigProperties,
) : PoolLegalEntityApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getLegalEntities(
        @ParameterObject searchRequest: LegalEntitySearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseDto> {
        return legalEntityLegacyServiceMapper.searchLegalEntities(
            LegalEntityLegacyServiceMapper.LegalEntitySearchRequest(
                bpnLs = searchRequest.bpnLs,
                legalName = searchRequest.legalName,
                isCatenaXMemberData = null
            ),
            paginationRequest
        )
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getLegalEntity(idValue: String, idType: String?): LegalEntityWithLegalAddressVerboseDto {
        val actualType = idType ?: bpnConfigProperties.id
        return if (actualType == bpnConfigProperties.id) legalEntityLegacyServiceMapper.findLegalEntityIgnoreCase(idValue.uppercase())
        else legalEntityLegacyServiceMapper.findLegalEntityIgnoreCase(actualType, idValue)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun postLegalEntitySearch(
        searchRequest: LegalEntitySearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseDto> {
        return legalEntityLegacyServiceMapper.searchLegalEntities(
            LegalEntityLegacyServiceMapper.LegalEntitySearchRequest(
                searchRequest.bpnLs,
                searchRequest.legalName,
                isCatenaXMemberData = null
            ),
            paginationRequest
        )
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getSites(
        bpnl: String,
        paginationRequest: PaginationRequest
    ): PageDto<SiteVerboseDto> {
        return legalEntityLegacyServiceMapper.findByParentBpn(bpnl.uppercase(), paginationRequest.page, paginationRequest.size)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getAddresses(
        bpnl: String,
        paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseDto> {
        return legalEntityLegacyServiceMapper.findNonSiteAddressesOfLegalEntity(bpnl.uppercase(), paginationRequest.page, paginationRequest.size)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun createBusinessPartners(
        businessPartners: Collection<LegalEntityPartnerCreateRequest>
    ): LegalEntityPartnerCreateResponseWrapper {
        return legalEntityLegacyServiceMapper.createLegalEntities(businessPartners)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun updateBusinessPartners(
        businessPartners: Collection<LegalEntityPartnerUpdateRequest>
    ): LegalEntityPartnerUpdateResponseWrapper {
        return legalEntityLegacyServiceMapper.updateLegalEntities(businessPartners)
    }
}
