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

package org.eclipse.tractusx.bpdm.gate.api.client

import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.GateLegalEntityApi
import org.eclipse.tractusx.bpdm.gate.api.model.request.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.LegalEntityGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityGateInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityGateOutputResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

@HttpExchange
interface LegalEntityApiClient : GateLegalEntityApi {
    @GetExchange("/input/legal-entities")
    override fun getLegalEntities(
        @ParameterObject @Valid paginationRequest: PaginationRequest
    ): PageDto<LegalEntityGateInputDto>

    @PostExchange("/input/legal-entities/search")
    override fun getLegalEntitiesByExternalIds(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody externalIds: Collection<String>
    ): PageDto<LegalEntityGateInputDto>

    @PostExchange("/output/legal-entities/search")
    override fun getLegalEntitiesOutput(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody(required = false) externalIds: Collection<String>?
    ): PageDto<LegalEntityGateOutputResponse>

    @GetExchange("/input/legal-entities/{externalId}")
    override fun getLegalEntityByExternalId(
        @Parameter(description = "External ID") @PathVariable externalId: String
    ): LegalEntityGateInputDto

    @PutExchange("/input/legal-entities")
    override fun upsertLegalEntities(
        @RequestBody legalEntities: Collection<LegalEntityGateInputRequest>
    ): ResponseEntity<Unit>

    @PutExchange("/output/legal-entities")
    override fun upsertLegalEntitiesOutput(
        @RequestBody legalEntities: Collection<LegalEntityGateOutputRequest>
    ): ResponseEntity<Unit>


}