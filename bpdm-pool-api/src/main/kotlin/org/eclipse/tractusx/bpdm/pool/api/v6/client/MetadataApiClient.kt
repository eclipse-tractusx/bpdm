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
import org.eclipse.tractusx.bpdm.pool.api.model.CountrySubdivisionDto
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolMetadataApi
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierTypeDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalFormDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalFormRequest
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange
interface MetadataApiClient: PoolMetadataApi {

    @PostExchange(value = "${ApiCommons.BASE_PATH_V6}/identifier-types")
    override fun createIdentifierType(@RequestBody identifierType: IdentifierTypeDto): IdentifierTypeDto

    @PostExchange(value = "${ApiCommons.BASE_PATH_V6}/legal-forms")
    override fun createLegalForm(@RequestBody type: LegalFormRequest): LegalFormDto

    @GetExchange(value = "${ApiCommons.BASE_PATH_V6}/identifier-types")
    override fun getIdentifierTypes(
        @ParameterObject paginationRequest: PaginationRequest,
        @RequestParam businessPartnerType: IdentifierBusinessPartnerType,
        @RequestParam country: CountryCode?
    ): PageDto<IdentifierTypeDto>

    @GetExchange(value = "${ApiCommons.BASE_PATH_V6}/legal-forms")
    override fun getLegalForms(@ParameterObject paginationRequest: PaginationRequest): PageDto<LegalFormDto>

    @GetExchange(value = "${ApiCommons.BASE_PATH_V6}/administrative-areas-level1")
    fun getAdminAreasLevel1(@ParameterObject paginationRequest: PaginationRequest): PageDto<CountrySubdivisionDto>
}