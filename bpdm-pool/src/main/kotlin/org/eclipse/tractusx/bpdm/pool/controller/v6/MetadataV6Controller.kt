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
import org.eclipse.tractusx.bpdm.common.exception.BpdmValidationErrorException
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolMetadataV6Api
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.CountrySubdivisionV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.CountryV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.FieldQualityRuleV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.IdentifierBusinessPartnerTypeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.IdentifierTypeV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.LegalFormV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.LegalFormV6Request
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.mapper.apiv6.ApiV6InboundMapper
import org.eclipse.tractusx.bpdm.pool.mapper.apiv6.ApiV6MetadataMapper
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.IdentifierTypeSearchResult
import org.eclipse.tractusx.bpdm.pool.service.application.AdministrativeAreaSearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.CountrySearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.IdentifierTypeSearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.LegalFormSearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.v6.FieldQualityRuleSearchApplicationV6Service
import org.eclipse.tractusx.bpdm.pool.service.application.v6.IdentifierTypeCreateApplicationV6Service
import org.eclipse.tractusx.bpdm.pool.service.application.v6.LegalFormCreateApplicationV6Service
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController("MetadataControllerLegacy")
class MetadataV6Controller(
    val identifierTypeCreateApplicationV6Service: IdentifierTypeCreateApplicationV6Service,
    val identifierTypeSearchApplicationService: IdentifierTypeSearchApplicationService,
    val legalFormCreateApplicationV6Service: LegalFormCreateApplicationV6Service,
    val legalFormSearchApplicationService: LegalFormSearchApplicationService,
    val fieldQualityRuleSearchApplicationV6Service: FieldQualityRuleSearchApplicationV6Service,
    val administrativeAreaSearchApplicationService: AdministrativeAreaSearchApplicationService,
    val countrySearchApplicationService: CountrySearchApplicationService,
    val apiV6MetadataMapper: ApiV6MetadataMapper,
    val apiV6InboundMapper: ApiV6InboundMapper
) : PoolMetadataV6Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_METADATA})")
    override fun createIdentifierType(identifierType: IdentifierTypeV6Dto): IdentifierTypeV6Dto {
        return identifierTypeCreateApplicationV6Service.createIdentifierType(identifierType)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getIdentifierTypes(
        paginationRequest: PaginationRequest,
        businessPartnerType: IdentifierBusinessPartnerTypeV6,
        country: String?
    ): PageDto<IdentifierTypeV6Dto> {
        val request = apiV6InboundMapper.toIdentifierTypeSearchRaw(businessPartnerType, country)
        return when (val result = identifierTypeSearchApplicationService.search(request, paginationRequest.toPageRequest())) {
            is IdentifierTypeSearchResult.Found -> result.page.toPageDto(apiV6MetadataMapper::toDto)
            is IdentifierTypeSearchResult.Rejected -> throw BpdmValidationErrorException(result.errors.map(apiV6MetadataMapper::toValidationError))
        }
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_METADATA})")
    override fun createLegalForm(type: LegalFormV6Request): LegalFormV6Dto {
        return legalFormCreateApplicationV6Service.createLegalForm(type)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getLegalForms(paginationRequest: PaginationRequest): PageDto<LegalFormV6Dto> {
        return legalFormSearchApplicationService.search(paginationRequest.toPageRequest())
            .toPageDto(apiV6MetadataMapper::toDto)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getFieldQualityRules(country: String): ResponseEntity<Collection<FieldQualityRuleV6Dto>> {
        return ResponseEntity(fieldQualityRuleSearchApplicationV6Service.searchFieldQualityRules(country), HttpStatus.OK)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getAdminAreasLevel1(paginationRequest: PaginationRequest): PageDto<CountrySubdivisionV6Dto> {
        return administrativeAreaSearchApplicationService.search(paginationRequest.toPageRequest())
            .toPageDto(apiV6MetadataMapper::toDto)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getCountries(paginationRequest: PaginationRequest): PageDto<CountryV6Dto> {
        return countrySearchApplicationService.search(paginationRequest.toPageRequest())
            .toPageDto(apiV6MetadataMapper::toDto)
    }

}
