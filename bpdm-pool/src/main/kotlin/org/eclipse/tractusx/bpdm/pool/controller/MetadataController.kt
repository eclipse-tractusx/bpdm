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
import org.eclipse.tractusx.bpdm.common.exception.BpdmValidationErrorException
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.v7.PoolMetadataV7Api
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.CountrySubdivisionV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.CountryV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.FieldQualityRuleV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierBusinessPartnerTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierTypeV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.LegalFormV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.LegalFormV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.ReasonCodeV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.ScriptCodeV7Dto
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.mapper.apiv7.ApiV7InboundMapper
import org.eclipse.tractusx.bpdm.pool.mapper.apiv7.ApiV7MetadataMapper
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.IdentifierTypeSearchResult
import org.eclipse.tractusx.bpdm.pool.service.application.AdministrativeAreaSearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.CountrySearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.IdentifierTypeSearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.LegalFormSearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.ReasonCodeSearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.ScriptCodeSearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.v7.FieldQualityRuleSearchApplicationV7Service
import org.eclipse.tractusx.bpdm.pool.service.application.v7.IdentifierTypeCreateApplicationV7Service
import org.eclipse.tractusx.bpdm.pool.service.application.v7.LegalFormCreateApplicationV7Service
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class MetadataController(
    val identifierTypeCreateApplicationV7Service: IdentifierTypeCreateApplicationV7Service,
    val identifierTypeSearchApplicationService: IdentifierTypeSearchApplicationService,
    val legalFormCreateApplicationV7Service: LegalFormCreateApplicationV7Service,
    val legalFormSearchApplicationService: LegalFormSearchApplicationService,
    val fieldQualityRuleSearchApplicationV7Service: FieldQualityRuleSearchApplicationV7Service,
    val administrativeAreaSearchApplicationService: AdministrativeAreaSearchApplicationService,
    val reasonCodeSearchApplicationService: ReasonCodeSearchApplicationService,
    val scriptCodeSearchApplicationService: ScriptCodeSearchApplicationService,
    val countrySearchApplicationService: CountrySearchApplicationService,
    val apiV7MetadataMapper: ApiV7MetadataMapper,
    val apiV7InboundMapper: ApiV7InboundMapper
) : PoolMetadataV7Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_METADATA})")
    override fun createIdentifierType(identifierType: IdentifierTypeV7Dto): IdentifierTypeV7Dto {
        return identifierTypeCreateApplicationV7Service.createIdentifierType(identifierType)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getIdentifierTypes(
        paginationRequest: PaginationRequest,
        businessPartnerType: IdentifierBusinessPartnerTypeV7,
        country: String?
    ): PageDto<IdentifierTypeV7Dto> {
        val request = apiV7InboundMapper.toIdentifierTypeSearchRaw(businessPartnerType, country)
        return when (val result = identifierTypeSearchApplicationService.search(request, paginationRequest.toPageRequest())) {
            is IdentifierTypeSearchResult.Found -> result.page.toPageDto(apiV7MetadataMapper::toDto)
            is IdentifierTypeSearchResult.Rejected -> throw BpdmValidationErrorException(result.errors.map(apiV7MetadataMapper::toValidationError))
        }
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_METADATA})")
    override fun createLegalForm(type: LegalFormV7Request): LegalFormV7Dto {
        return legalFormCreateApplicationV7Service.createLegalForm(type)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getLegalForms(paginationRequest: PaginationRequest): PageDto<LegalFormV7Dto> {
        return legalFormSearchApplicationService.search(paginationRequest.toPageRequest())
            .toPageDto(apiV7MetadataMapper::toDto)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getFieldQualityRules(country: String): ResponseEntity<Collection<FieldQualityRuleV7Dto>> {
        return ResponseEntity(fieldQualityRuleSearchApplicationV7Service.searchFieldQualityRules(country), HttpStatus.OK)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getAdminAreasLevel1(paginationRequest: PaginationRequest): PageDto<CountrySubdivisionV7Dto> {
        return administrativeAreaSearchApplicationService.search(paginationRequest.toPageRequest())
            .toPageDto(apiV7MetadataMapper::toDto)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getReasonCodes(paginationRequest: PaginationRequest): PageDto<ReasonCodeV7Dto> {
        return reasonCodeSearchApplicationService.search(paginationRequest.toPageRequest())
            .toPageDto(apiV7MetadataMapper::toDto)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getScriptCodes(paginationRequest: PaginationRequest): PageDto<ScriptCodeV7Dto> {
        return scriptCodeSearchApplicationService.search(paginationRequest.toPageRequest())
            .toPageDto(apiV7MetadataMapper::toDto)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getCountries(paginationRequest: PaginationRequest): PageDto<CountryV7Dto> {
        return countrySearchApplicationService.search(paginationRequest.toPageRequest())
            .toPageDto(apiV7MetadataMapper::toDto)
    }

}