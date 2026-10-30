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
import org.eclipse.tractusx.bpdm.gate.api.GateRelationOutputApi
import org.eclipse.tractusx.bpdm.gate.api.model.RelationOutputDto
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.mapper.gatev7.inbound.EnumRequestMapper
import org.eclipse.tractusx.bpdm.gate.mapper.gatev7.outbound.RelationOutputDtoMapper
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationOutputSearchRaw
import org.eclipse.tractusx.bpdm.gate.service.application.RelationOutputSearchApplicationService
import org.eclipse.tractusx.bpdm.gate.util.PrincipalUtil
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationOutputSearchRequest as RelationOutputSearchRequestDto

@RestController
class RelationOutputController(
    private val principalUtil: PrincipalUtil,
    private val outputSearchApplicationService: RelationOutputSearchApplicationService,
    private val outputDtoMapper: RelationOutputDtoMapper,
    private val enumRequestMapper: EnumRequestMapper
): GateRelationOutputApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_OUTPUT_PARTNER})")
    override fun postSearch(
        searchRequest: RelationOutputSearchRequestDto,
        paginationRequest: PaginationRequest
    ): PageDto<RelationOutputDto> {
        val request = RelationOutputSearchRaw(
            externalIds = searchRequest.externalIds,
            relationType = searchRequest.relationType?.let(enumRequestMapper::toModel),
            sourceBpns = searchRequest.sourceBpns,
            targetBpns = searchRequest.targetBpns,
            updatedAfter = searchRequest.updatedAtFrom
        )
        return outputSearchApplicationService.search(request, principalUtil.resolveTenantBpnl().value, paginationRequest.toPageRequest())
            .toPageDto(outputDtoMapper::toDto)
    }
}