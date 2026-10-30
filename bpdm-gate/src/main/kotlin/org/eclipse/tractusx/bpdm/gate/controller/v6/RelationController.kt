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
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.v6.GateRelationApi
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPostRequest
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPutEntryV6
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.RelationDto
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.mapper.gatev6.inbound.RelationInputRequestMapperV6
import org.eclipse.tractusx.bpdm.gate.mapper.gatev6.outbound.RelationInputDeleteErrorMapperV6
import org.eclipse.tractusx.bpdm.gate.mapper.gatev6.outbound.RelationInputDtoMapperV6
import org.eclipse.tractusx.bpdm.gate.mapper.gatev6.outbound.RelationInputUpsertErrorMapperV6
import org.eclipse.tractusx.bpdm.gate.mapper.gatev7.inbound.EnumRequestMapper
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationInputDeleteResult
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationInputUpsertResult
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationInputDeleteRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationInputSearchRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationInputUpsertMode
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationInputUpsertRaw
import org.eclipse.tractusx.bpdm.gate.service.application.RelationInputDeleteApplicationService
import org.eclipse.tractusx.bpdm.gate.service.application.RelationInputSearchApplicationService
import org.eclipse.tractusx.bpdm.gate.service.application.RelationInputUpsertApplicationService
import org.eclipse.tractusx.bpdm.gate.util.PrincipalUtil
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController("RelationControllerLegacy")
class RelationController(
    private val principalUtil: PrincipalUtil,
    private val inputSearchApplicationService: RelationInputSearchApplicationService,
    private val inputUpsertApplicationService: RelationInputUpsertApplicationService,
    private val inputRequestMapper: RelationInputRequestMapperV6,
    private val inputDtoMapper: RelationInputDtoMapperV6,
    private val inputErrorMapper: RelationInputUpsertErrorMapperV6,
    private val inputDeleteApplicationService: RelationInputDeleteApplicationService,
    private val inputDeleteErrorMapper: RelationInputDeleteErrorMapperV6,
    private val enumRequestMapper: EnumRequestMapper
): GateRelationApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_INPUT_RELATION})")
    override fun get(
        externalIds: List<String>?,
        relationType: RelationType?,
        businessPartnerSourceExternalIds: List<String>?,
        businessPartnerTargetExternalIds: List<String>?,
        updatedAtFrom: Instant?,
        paginationRequest: PaginationRequest
    ): PageDto<RelationDto> {
        val request = RelationInputSearchRaw(
            externalIds = externalIds,
            relationType = relationType?.let(enumRequestMapper::toModel),
            sourceExternalIds = businessPartnerSourceExternalIds,
            targetExternalIds = businessPartnerTargetExternalIds,
            updatedAfter = updatedAtFrom
        )
        return inputSearchApplicationService.search(request, principalUtil.resolveTenantBpnl().value, paginationRequest.toPageRequest())
            .toPageDto(inputDtoMapper::toDto)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_INPUT_RELATION})")
    override fun post(
        requestBody: RelationPostRequest
    ): RelationDto =
        upsert(inputRequestMapper.toUpsertRequest(requestBody), RelationInputUpsertMode.CreateOnly)

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_INPUT_RELATION})")
    override fun put(
        createIfNotExist: Boolean,
        requestBody: RelationPutEntryV6
    ): RelationDto {
        val mode = if (createIfNotExist) RelationInputUpsertMode.CreateOrUpdate else RelationInputUpsertMode.UpdateOnly
        return upsert(inputRequestMapper.toUpsertRequest(requestBody), mode)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_INPUT_RELATION})")
    override fun delete(externalId: String) {
        when (val result = inputDeleteApplicationService.delete(RelationInputDeleteRaw(externalId), principalUtil.resolveTenantBpnl().value)) {
            is RelationInputDeleteResult.Deleted -> Unit
            is RelationInputDeleteResult.Rejected -> throw inputDeleteErrorMapper.toException(result.errors)
        }
    }

    private fun upsert(request: RelationInputUpsertRaw, mode: RelationInputUpsertMode): RelationDto =
        when (val result = inputUpsertApplicationService.upsert(listOf(request), principalUtil.resolveTenantBpnl().value, mode)) {
            is RelationInputUpsertResult.Written -> inputDtoMapper.toDto(result.relations.single())
            is RelationInputUpsertResult.Rejected -> throw inputErrorMapper.toException(result.errors)
        }
}