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

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolMetadataV6Api
import org.eclipse.tractusx.bpdm.pool.api.v6.model.CountrySubdivisionDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierBusinessPartnerTypeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierTypeDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalFormDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalFormRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.FieldQualityRuleDtoV6
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController("MetadataControllerLegacy")
class MetadataV6Controller(
    val metadataLegacyServiceMapper: MetadataLegacyServiceMapper
) : PoolMetadataV6Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_METADATA})")
    override fun createIdentifierType(identifierType: IdentifierTypeDtoV6): IdentifierTypeDtoV6 {
        return metadataLegacyServiceMapper.createIdentifierType(identifierType)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getIdentifierTypes(
        paginationRequest: PaginationRequest,
        businessPartnerType: IdentifierBusinessPartnerTypeV6,
        country: CountryCode?
    ): PageDto<IdentifierTypeDtoV6> {
        return metadataLegacyServiceMapper.getIdentifierTypes(PageRequest.of(paginationRequest.page, paginationRequest.size), businessPartnerType, country)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_METADATA})")
    override fun createLegalForm(type: LegalFormRequestV6): LegalFormDtoV6 {
        return metadataLegacyServiceMapper.createLegalForm(type)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getLegalForms(paginationRequest: PaginationRequest): PageDto<LegalFormDtoV6> {
        return metadataLegacyServiceMapper.getLegalForms(PageRequest.of(paginationRequest.page, paginationRequest.size))
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getFieldQualityRules(country: CountryCode): ResponseEntity<Collection<FieldQualityRuleDtoV6>> {
        return ResponseEntity(metadataLegacyServiceMapper.getFieldQualityRules(country), HttpStatus.OK)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getAdminAreasLevel1(paginationRequest: PaginationRequest): PageDto<CountrySubdivisionDtoV6> {
        return metadataLegacyServiceMapper.getAdminAreasLevel1(paginationRequest)
    }

}
