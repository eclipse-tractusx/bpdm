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

package org.eclipse.tractusx.bpdm.gate.controller.v7

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.gate.api.v7.GateRelationV7Api
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationPutV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationPutV7Response
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationSearchV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationV7Dto
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.controller.TenantResolver
import org.eclipse.tractusx.bpdm.gate.exception.BpdmRelationInputUpsertException
import org.eclipse.tractusx.bpdm.gate.mapper.apiv7.ApiV7InboundMapper
import org.eclipse.tractusx.bpdm.gate.mapper.apiv7.ApiV7RelationMapper
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationInputUpsertResult
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationInputSearchRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationInputUpsertMode
import org.eclipse.tractusx.bpdm.gate.service.application.RelationInputSearchApplicationService
import org.eclipse.tractusx.bpdm.gate.service.application.RelationInputUpsertApplicationService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class RelationController(
    private val tenantResolver: TenantResolver,
    private val inputSearchApplicationService: RelationInputSearchApplicationService,
    private val inputUpsertApplicationService: RelationInputUpsertApplicationService,
    private val apiV7InboundMapper: ApiV7InboundMapper,
    private val apiV7RelationMapper: ApiV7RelationMapper,
): GateRelationV7Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_INPUT_RELATION})")
    override fun postSearch(
        searchRequest: RelationSearchV7Request,
        paginationRequest: PaginationRequest
    ): PageDto<RelationV7Dto> {
        val request = RelationInputSearchRaw(
            externalIds = searchRequest.externalIds,
            relationType = searchRequest.relationType?.let(apiV7InboundMapper::toModel),
            sourceExternalIds = searchRequest.businessPartnerSourceExternalIds,
            targetExternalIds = searchRequest.businessPartnerTargetExternalIds,
            updatedAfter = searchRequest.updatedAtFrom
        )
        return inputSearchApplicationService.search(request, tenantResolver.resolveTenantBpnl().value, paginationRequest.toPageRequest())
            .toPageDto(apiV7RelationMapper::toDto)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_INPUT_RELATION})")
    override fun put(
        createIfNotExist: Boolean,
        requestBody: RelationPutV7Request
    ): RelationPutV7Response {
        val requests = requestBody.relations.map(apiV7InboundMapper::toUpsertRequest)
        val mode = if (createIfNotExist) RelationInputUpsertMode.CreateOrUpdate else RelationInputUpsertMode.UpdateOnly
        return when (val result = inputUpsertApplicationService.upsert(requests, tenantResolver.resolveTenantBpnl().value, mode)) {
            is RelationInputUpsertResult.Written -> RelationPutV7Response(result.relations.map(apiV7RelationMapper::toDto))
            is RelationInputUpsertResult.Rejected -> throw BpdmRelationInputUpsertException(result.errors.map(apiV7RelationMapper::toMessage))
        }
    }
}