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

package org.eclipse.tractusx.bpdm.pool.api.v6.client

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.ApiCommons
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolMetadataV6Api
import org.eclipse.tractusx.bpdm.pool.api.v6.model.CountrySubdivisionDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierBusinessPartnerTypeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierTypeDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalFormDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalFormRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.FieldQualityRuleDtoV6
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange
interface MetadataV6ApiClient: PoolMetadataV6Api {

    @PostExchange(value = "${ApiCommons.BASE_PATH_V6}/identifier-types")
    override fun createIdentifierType(@RequestBody identifierType: IdentifierTypeDtoV6): IdentifierTypeDtoV6

    @PostExchange(value = "${ApiCommons.BASE_PATH_V6}/legal-forms")
    override fun createLegalForm(@RequestBody type: LegalFormRequestV6): LegalFormDtoV6

    @GetExchange(value = "${ApiCommons.BASE_PATH_V6}/identifier-types")
    override fun getIdentifierTypes(
        @ParameterObject paginationRequest: PaginationRequest,
        @RequestParam businessPartnerType: IdentifierBusinessPartnerTypeV6,
        @RequestParam country: CountryCode?
    ): PageDto<IdentifierTypeDtoV6>

    @GetExchange(value = "${ApiCommons.BASE_PATH_V6}/legal-forms")
    override fun getLegalForms(@ParameterObject paginationRequest: PaginationRequest): PageDto<LegalFormDtoV6>

    @GetExchange(value = "${ApiCommons.BASE_PATH_V6}/field-quality-rules/")
    override fun getFieldQualityRules(@RequestParam country: CountryCode): ResponseEntity<Collection<FieldQualityRuleDtoV6>>

    @GetExchange(value = "${ApiCommons.BASE_PATH_V6}/administrative-areas-level1")
    override fun getAdminAreasLevel1(@ParameterObject paginationRequest: PaginationRequest): PageDto<CountrySubdivisionDtoV6>
}