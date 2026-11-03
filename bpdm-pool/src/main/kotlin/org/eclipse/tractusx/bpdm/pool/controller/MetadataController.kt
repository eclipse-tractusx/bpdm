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
import org.eclipse.tractusx.bpdm.pool.service.application.v7.AdministrativeAreaSearchApplicationV7Service
import org.eclipse.tractusx.bpdm.pool.service.application.v7.CountrySearchApplicationV7Service
import org.eclipse.tractusx.bpdm.pool.service.application.v7.FieldQualityRuleSearchApplicationV7Service
import org.eclipse.tractusx.bpdm.pool.service.application.v7.IdentifierTypeCreateApplicationV7Service
import org.eclipse.tractusx.bpdm.pool.service.application.v7.IdentifierTypeSearchApplicationV7Service
import org.eclipse.tractusx.bpdm.pool.service.application.v7.LegalFormCreateApplicationV7Service
import org.eclipse.tractusx.bpdm.pool.service.application.v7.LegalFormSearchApplicationV7Service
import org.eclipse.tractusx.bpdm.pool.service.application.v7.ReasonCodeSearchApplicationV7Service
import org.eclipse.tractusx.bpdm.pool.service.application.v7.ScriptCodeSearchApplicationV7Service
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class MetadataController(
    val identifierTypeCreateApplicationV7Service: IdentifierTypeCreateApplicationV7Service,
    val identifierTypeSearchApplicationV7Service: IdentifierTypeSearchApplicationV7Service,
    val legalFormCreateApplicationV7Service: LegalFormCreateApplicationV7Service,
    val legalFormSearchApplicationV7Service: LegalFormSearchApplicationV7Service,
    val fieldQualityRuleSearchApplicationV7Service: FieldQualityRuleSearchApplicationV7Service,
    val administrativeAreaSearchApplicationV7Service: AdministrativeAreaSearchApplicationV7Service,
    val reasonCodeSearchApplicationV7Service: ReasonCodeSearchApplicationV7Service,
    val scriptCodeSearchApplicationV7Service: ScriptCodeSearchApplicationV7Service,
    val countrySearchApplicationV7Service: CountrySearchApplicationV7Service
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
        return identifierTypeSearchApplicationV7Service.searchIdentifierTypes(businessPartnerType, country, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_METADATA})")
    override fun createLegalForm(type: LegalFormV7Request): LegalFormV7Dto {
        return legalFormCreateApplicationV7Service.createLegalForm(type)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getLegalForms(paginationRequest: PaginationRequest): PageDto<LegalFormV7Dto> {
        return legalFormSearchApplicationV7Service.searchLegalForms(paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getFieldQualityRules(country: String): ResponseEntity<Collection<FieldQualityRuleV7Dto>> {
        return ResponseEntity(fieldQualityRuleSearchApplicationV7Service.searchFieldQualityRules(country), HttpStatus.OK)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getAdminAreasLevel1(paginationRequest: PaginationRequest): PageDto<CountrySubdivisionV7Dto> {
        return administrativeAreaSearchApplicationV7Service.searchAdministrativeAreas(paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getReasonCodes(paginationRequest: PaginationRequest): PageDto<ReasonCodeV7Dto> {
        return reasonCodeSearchApplicationV7Service.searchReasonCodes(paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getScriptCodes(paginationRequest: PaginationRequest): PageDto<ScriptCodeV7Dto> {
        return scriptCodeSearchApplicationV7Service.searchScriptCodes(paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_METADATA})")
    override fun getCountries(paginationRequest: PaginationRequest): PageDto<CountryV7Dto> {
        return countrySearchApplicationV7Service.searchCountries(paginationRequest)
    }

}