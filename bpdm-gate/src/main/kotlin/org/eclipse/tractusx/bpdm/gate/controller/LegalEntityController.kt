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
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.gate.api.GateLegalEntityApi
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateOutput
import org.eclipse.tractusx.bpdm.gate.api.model.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageOutputResponse
import org.eclipse.tractusx.bpdm.gate.config.ApiConfigProperties
import org.eclipse.tractusx.bpdm.gate.containsDuplicates
import org.eclipse.tractusx.bpdm.gate.service.LegalEntityService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class LegalEntityController(
    val legalEntityService: LegalEntityService,
    val apiConfigProperties: ApiConfigProperties
) : GateLegalEntityApi {

    override fun upsertLegalEntities(legalEntities: Collection<LegalEntityGateInputRequest>): ResponseEntity<Unit> {
        if (legalEntities.size > apiConfigProperties.upsertLimit || legalEntities.map { it.externalId }.containsDuplicates()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        legalEntityService.upsertLegalEntities(legalEntities)
        return ResponseEntity(HttpStatus.OK)
    }

    override fun getLegalEntityByExternalId(externalId: String): LegalEntityGateInputResponse {
        return legalEntityService.getLegalEntityByExternalId(externalId)
    }

    override fun getLegalEntitiesByExternalIds(
        paginationRequest: PaginationRequest,
        externalIds: Collection<String>
    ): PageResponse<LegalEntityGateInputResponse> {
        return legalEntityService.getLegalEntities(page = paginationRequest.page, size = paginationRequest.size, externalIds = externalIds)
    }

    override fun getLegalEntities(paginationRequest: PaginationRequest): PageResponse<LegalEntityGateInputResponse> {
        return legalEntityService.getLegalEntities(page = paginationRequest.page, size = paginationRequest.size)
    }

    override fun getLegalEntitiesOutput(
        paginationRequest: PaginationStartAfterRequest,
        externalIds: Collection<String>?
    ): PageOutputResponse<LegalEntityGateOutput> {
        return legalEntityService.getLegalEntitiesOutput(externalIds, paginationRequest.limit, paginationRequest.startAfter)
    }

    override fun upsertLegalEntitiesOutput(legalEntities: Collection<LegalEntityGateInputRequest>): ResponseEntity<Unit> {
        if (legalEntities.size > apiConfigProperties.upsertLimit || legalEntities.map { it.externalId }.containsDuplicates()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        legalEntityService.upsertLegalEntitiesOutput(legalEntities)
        return ResponseEntity(HttpStatus.OK)
    }

}