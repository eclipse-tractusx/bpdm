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

package org.eclipse.tractusx.bpdm.gate.controller

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.gate.api.GateRelationApi
import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.RelationPutResponse
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.exception.BpdmRelationInputUpsertException
import org.eclipse.tractusx.bpdm.gate.mapper.gatev7.inbound.RelationInputRequestMapper
import org.eclipse.tractusx.bpdm.gate.mapper.gatev7.outbound.RelationInputDtoMapper
import org.eclipse.tractusx.bpdm.gate.mapper.gatev7.outbound.RelationInputUpsertErrorMapper
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputUpsertMode
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputUpsertResult
import org.eclipse.tractusx.bpdm.gate.model.search.RelationInputSearchRequest
import org.eclipse.tractusx.bpdm.gate.service.application.RelationInputSearchApplicationService
import org.eclipse.tractusx.bpdm.gate.service.application.RelationInputUpsertApplicationService
import org.eclipse.tractusx.bpdm.gate.util.PrincipalUtil
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class RelationController(
    private val principalUtil: PrincipalUtil,
    private val inputSearchApplicationService: RelationInputSearchApplicationService,
    private val inputUpsertApplicationService: RelationInputUpsertApplicationService,
    private val inputRequestMapper: RelationInputRequestMapper,
    private val inputDtoMapper: RelationInputDtoMapper,
    private val inputErrorMapper: RelationInputUpsertErrorMapper
): GateRelationApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_INPUT_RELATION})")
    override fun postSearch(
        searchRequest: RelationSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<RelationDto> {
        val request = RelationInputSearchRequest(
            externalIds = searchRequest.externalIds,
            relationType = searchRequest.relationType,
            sourceExternalIds = searchRequest.businessPartnerSourceExternalIds,
            targetExternalIds = searchRequest.businessPartnerTargetExternalIds,
            updatedAfter = searchRequest.updatedAtFrom
        )
        return inputSearchApplicationService.search(request, principalUtil.resolveTenantBpnl().value, paginationRequest.toPageRequest())
            .toPageDto(inputDtoMapper::toDto)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_INPUT_RELATION})")
    override fun put(
        createIfNotExist: Boolean,
        requestBody: RelationPutRequest
    ): RelationPutResponse {
        val requests = requestBody.relations.map(inputRequestMapper::toUpsertRequest)
        val mode = if (createIfNotExist) RelationInputUpsertMode.CreateOrUpdate else RelationInputUpsertMode.UpdateOnly
        return when (val result = inputUpsertApplicationService.upsert(requests, principalUtil.resolveTenantBpnl().value, mode)) {
            is RelationInputUpsertResult.Written -> RelationPutResponse(result.relations.map(inputDtoMapper::toDto))
            is RelationInputUpsertResult.Rejected -> throw BpdmRelationInputUpsertException(result.errors.map(inputErrorMapper::toMessage))
        }
    }
}