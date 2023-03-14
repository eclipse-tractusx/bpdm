/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.LegalFormResponse
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.pool.api.PoolMetadataApi
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalFormRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.CountryIdentifierTypeResponse
import org.eclipse.tractusx.bpdm.pool.service.MetadataService
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.RestController

@RestController
class MetadataController(
    val metadataService: MetadataService
) : PoolMetadataApi {

    override fun createIdentifierType(type: TypeKeyNameDto<String>): TypeKeyNameDto<String> {
        return metadataService.createIdentifierType(type)
    }

    override fun getIdentifierTypes(paginationRequest: PaginationRequest): PageResponse<TypeKeyNameDto<String>> {
        return metadataService.getIdentifierTypes(PageRequest.of(paginationRequest.page, paginationRequest.size))
    }

    override fun getValidIdentifierTypesForCountry(country: CountryCode): Collection<CountryIdentifierTypeResponse> {
        return metadataService.getValidIdentifierTypesForCountry(country)
    }

    @Deprecated("IdentifierStatus removed")
    override fun createIdentifierStatus(status: TypeKeyNameDto<String>): TypeKeyNameDto<String> {
        return metadataService.createIdentifierStatus(status)
    }

    @Deprecated("IdentifierStatus removed")
    override fun getIdentifierStati(paginationRequest: PaginationRequest): PageResponse<TypeKeyNameDto<String>> {
        return metadataService.getIdentifierStati(PageRequest.of(paginationRequest.page, paginationRequest.size))
    }

    override fun createIssuingBody(type: TypeKeyNameDto<String>): TypeKeyNameDto<String> {
        return metadataService.createIssuingBody(type)
    }

    override fun getIssuingBodies(paginationRequest: PaginationRequest): PageResponse<TypeKeyNameDto<String>> {
        return metadataService.getIssuingBodies(PageRequest.of(paginationRequest.page, paginationRequest.size))
    }

    override fun createLegalForm(type: LegalFormRequest): LegalFormResponse {
        return metadataService.createLegalForm(type)
    }

    override fun getLegalForms(paginationRequest: PaginationRequest): PageResponse<LegalFormResponse> {
        return metadataService.getLegalForms(PageRequest.of(paginationRequest.page, paginationRequest.size))
    }
}