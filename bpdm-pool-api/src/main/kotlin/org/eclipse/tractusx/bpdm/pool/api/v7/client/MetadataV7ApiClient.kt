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

package org.eclipse.tractusx.bpdm.pool.api.v7.client

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.ApiCommons
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
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.*

@HttpExchange
interface MetadataV7ApiClient: PoolMetadataV7Api {

    @PostExchange(value = "${ApiCommons.BASE_PATH_V7}/identifier-types")
    override fun createIdentifierType(@RequestBody identifierType: IdentifierTypeV7Dto): IdentifierTypeV7Dto

    @PostExchange(value = "${ApiCommons.BASE_PATH_V7}/legal-forms")
    override fun createLegalForm(@RequestBody type: LegalFormV7Request): LegalFormV7Dto

    @GetExchange(value = "${ApiCommons.BASE_PATH_V7}/administrative-areas-level1")
    override fun getAdminAreasLevel1(@ParameterObject paginationRequest: PaginationRequest): PageDto<CountrySubdivisionV7Dto>

    @GetExchange(value = "${ApiCommons.BASE_PATH_V7}/field-quality-rules/")
    override fun getFieldQualityRules(@RequestParam country: String): ResponseEntity<Collection<FieldQualityRuleV7Dto>>

    @GetExchange(value = "${ApiCommons.BASE_PATH_V7}/identifier-types")
    override fun getIdentifierTypes(
        @ParameterObject paginationRequest: PaginationRequest,
        @RequestParam businessPartnerType: IdentifierBusinessPartnerTypeV7,
        @RequestParam country: String?
    ): PageDto<IdentifierTypeV7Dto>

    @GetExchange(value = "${ApiCommons.BASE_PATH_V7}/legal-forms")
    override fun getLegalForms(@ParameterObject paginationRequest: PaginationRequest): PageDto<LegalFormV7Dto>

    @GetExchange(value = "${ApiCommons.BASE_PATH_V7}/script-codes")
    override fun getScriptCodes(@ParameterObject paginationRequest: PaginationRequest): PageDto<ScriptCodeV7Dto>

    @GetExchange(value = "${ApiCommons.BASE_PATH_V7}/reason-codes")
    override fun getReasonCodes(@ParameterObject paginationRequest: PaginationRequest): PageDto<ReasonCodeV7Dto>

    @GetExchange(value = "${ApiCommons.BASE_PATH_V7}/countries")
    override fun getCountries(@ParameterObject paginationRequest: PaginationRequest): PageDto<CountryV7Dto>
}