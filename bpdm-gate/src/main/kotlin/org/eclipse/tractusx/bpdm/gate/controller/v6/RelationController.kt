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

package org.eclipse.tractusx.bpdm.gate.controller.v6

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.gate.api.v6.GateRelationV6Api
import org.eclipse.tractusx.bpdm.gate.api.v6.model.relation.RelationTypeV6
import org.eclipse.tractusx.bpdm.gate.api.v6.model.relation.input.RelationPostV6Request
import org.eclipse.tractusx.bpdm.gate.api.v6.model.relation.input.RelationPutV6Entry
import org.eclipse.tractusx.bpdm.gate.api.v6.model.relation.input.RelationV6Dto
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.controller.TenantResolver
import org.eclipse.tractusx.bpdm.gate.mapper.apiv6.ApiV6InboundMapper
import org.eclipse.tractusx.bpdm.gate.mapper.apiv6.ApiV6RelationMapper
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationInputDeleteResult
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationInputUpsertResult
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationInputDeleteRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationInputSearchRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationInputUpsertMode
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationInputUpsertRaw
import org.eclipse.tractusx.bpdm.gate.service.application.RelationInputDeleteApplicationService
import org.eclipse.tractusx.bpdm.gate.service.application.RelationInputSearchApplicationService
import org.eclipse.tractusx.bpdm.gate.service.application.RelationInputUpsertApplicationService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController("RelationControllerLegacy")
class RelationController(
    private val tenantResolver: TenantResolver,
    private val inputSearchApplicationService: RelationInputSearchApplicationService,
    private val inputUpsertApplicationService: RelationInputUpsertApplicationService,
    private val apiV6InboundMapper: ApiV6InboundMapper,
    private val apiV6RelationMapper: ApiV6RelationMapper,
    private val inputDeleteApplicationService: RelationInputDeleteApplicationService
): GateRelationV6Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_INPUT_RELATION})")
    override fun get(
        externalIds: List<String>?,
        relationType: RelationTypeV6?,
        businessPartnerSourceExternalIds: List<String>?,
        businessPartnerTargetExternalIds: List<String>?,
        updatedAtFrom: Instant?,
        paginationRequest: PaginationRequest
    ): PageDto<RelationV6Dto> {
        val request = RelationInputSearchRaw(
            externalIds = externalIds,
            relationType = relationType?.let(apiV6InboundMapper::toModel),
            sourceExternalIds = businessPartnerSourceExternalIds,
            targetExternalIds = businessPartnerTargetExternalIds,
            updatedAfter = updatedAtFrom
        )
        return inputSearchApplicationService.search(request, tenantResolver.resolveTenantBpnl().value, paginationRequest.toPageRequest())
            .toPageDto(apiV6RelationMapper::toDto)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_INPUT_RELATION})")
    override fun post(
        requestBody: RelationPostV6Request
    ): RelationV6Dto =
        upsert(apiV6InboundMapper.toUpsertRequest(requestBody), RelationInputUpsertMode.CreateOnly)

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_INPUT_RELATION})")
    override fun put(
        createIfNotExist: Boolean,
        requestBody: RelationPutV6Entry
    ): RelationV6Dto {
        val mode = if (createIfNotExist) RelationInputUpsertMode.CreateOrUpdate else RelationInputUpsertMode.UpdateOnly
        return upsert(apiV6InboundMapper.toUpsertRequest(requestBody), mode)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_INPUT_RELATION})")
    override fun delete(externalId: String) {
        when (val result = inputDeleteApplicationService.delete(RelationInputDeleteRaw(externalId), tenantResolver.resolveTenantBpnl().value)) {
            is RelationInputDeleteResult.Deleted -> Unit
            is RelationInputDeleteResult.Rejected -> throw apiV6RelationMapper.toDeleteException(result.errors)
        }
    }

    private fun upsert(request: RelationInputUpsertRaw, mode: RelationInputUpsertMode): RelationV6Dto =
        when (val result = inputUpsertApplicationService.upsert(listOf(request), tenantResolver.resolveTenantBpnl().value, mode)) {
            is RelationInputUpsertResult.Written -> apiV6RelationMapper.toDto(result.relations.single())
            is RelationInputUpsertResult.Rejected -> throw apiV6RelationMapper.toUpsertException(result.errors)
        }
}