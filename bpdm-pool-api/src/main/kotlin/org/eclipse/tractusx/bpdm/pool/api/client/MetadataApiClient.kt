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

package org.eclipse.tractusx.bpdm.pool.api.client

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.PoolMetadataApi
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalFormRequest
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange
interface MetadataApiClient: PoolMetadataApi {

    @PostExchange("/identifier-types")
    override fun createIdentifierType(@RequestBody identifierType: IdentifierTypeDto): IdentifierTypeDto

    @PostExchange("/legal-forms")
    override fun createLegalForm(@RequestBody type: LegalFormRequest): LegalFormDto

    @GetExchange("/administrative-areas-level1")
    override fun getAdminAreasLevel1(@ParameterObject paginationRequest: PaginationRequest): PageDto<CountrySubdivisionDto>

    @GetExchange("/field-quality-rules/")
    override fun getFieldQualityRules(@RequestParam country: CountryCode): ResponseEntity<Collection<FieldQualityRuleDto>>

    @GetExchange("/identifier-types")
    override fun getIdentifierTypes(
        paginationRequest: PaginationRequest,
        @RequestParam businessPartnerType: IdentifierBusinessPartnerType,
        @RequestParam country: CountryCode?
    ): PageDto<IdentifierTypeDto>

    @GetExchange("/legal-forms")
    override fun getLegalForms(paginationRequest: PaginationRequest): PageDto<LegalFormDto>
}