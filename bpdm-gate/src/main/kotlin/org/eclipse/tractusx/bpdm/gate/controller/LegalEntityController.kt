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

package org.eclipse.tractusx.bpdm.gate.controller

import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageDto
import org.eclipse.tractusx.bpdm.gate.api.GateLegalEntityApi
import org.eclipse.tractusx.bpdm.gate.api.model.request.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.LegalEntityGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityGateInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityGateOutputResponse
import org.eclipse.tractusx.bpdm.gate.config.ApiConfigProperties
import org.eclipse.tractusx.bpdm.gate.containsDuplicates
import org.eclipse.tractusx.bpdm.gate.service.LegalEntityService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class LegalEntityController(
    val legalEntityService: LegalEntityService,
    val apiConfigProperties: ApiConfigProperties
) : GateLegalEntityApi {
    @PreAuthorize("hasAuthority(@gateSecurityConfigProperties.getChangeCompanyInputDataAsRole())")
    override fun upsertLegalEntities(legalEntities: Collection<LegalEntityGateInputRequest>): ResponseEntity<Unit> {
        if (legalEntities.size > apiConfigProperties.upsertLimit || legalEntities.map { it.externalId }.containsDuplicates()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        legalEntityService.upsertLegalEntities(legalEntities)
        return ResponseEntity(HttpStatus.OK)
    }

    @PreAuthorize("hasAuthority(@gateSecurityConfigProperties.getReadCompanyInputDataAsRole())")
    override fun getLegalEntityByExternalId(externalId: String): LegalEntityGateInputDto {
        return legalEntityService.getLegalEntityByExternalId(externalId)
    }

    @PreAuthorize("hasAuthority(@gateSecurityConfigProperties.getReadCompanyInputDataAsRole())")
    override fun getLegalEntitiesByExternalIds(
        paginationRequest: PaginationRequest,
        externalIds: Collection<String>
    ): PageDto<LegalEntityGateInputDto> {
        return legalEntityService.getLegalEntities(page = paginationRequest.page, size = paginationRequest.size, externalIds = externalIds)
    }

    @PreAuthorize("hasAuthority(@gateSecurityConfigProperties.getReadCompanyInputDataAsRole())")
    override fun getLegalEntities(paginationRequest: PaginationRequest): PageDto<LegalEntityGateInputDto> {
        return legalEntityService.getLegalEntities(page = paginationRequest.page, size = paginationRequest.size)
    }

    @PreAuthorize("hasAuthority(@gateSecurityConfigProperties.getReadCompanyOutputDataAsRole())")
    override fun getLegalEntitiesOutput(
        paginationRequest: PaginationRequest,
        externalIds: Collection<String>?
    ): PageDto<LegalEntityGateOutputResponse> {
        return legalEntityService.getLegalEntitiesOutput(externalIds = externalIds, page = paginationRequest.page, size = paginationRequest.size)
    }

    @PreAuthorize("hasAuthority(@gateSecurityConfigProperties.getChangeCompanyOutputDataAsRole())")
    override fun upsertLegalEntitiesOutput(legalEntities: Collection<LegalEntityGateOutputRequest>): ResponseEntity<Unit> {
        if (legalEntities.size > apiConfigProperties.upsertLimit || legalEntities.map { it.externalId }.containsDuplicates()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        legalEntityService.upsertLegalEntitiesOutput(legalEntities)
        return ResponseEntity(HttpStatus.OK)
    }

}