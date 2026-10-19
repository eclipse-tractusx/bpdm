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
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.v6.GateRelationApi
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPostRequest
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPutEntryV6
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.RelationDto
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.mapper.gatev6.inbound.RelationInputRequestMapperV6
import org.eclipse.tractusx.bpdm.gate.mapper.gatev6.outbound.RelationInputDtoMapperV6
import org.eclipse.tractusx.bpdm.gate.mapper.gatev6.outbound.RelationInputUpsertErrorMapperV6
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputUpsertMode
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputUpsertRequest
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputUpsertResult
import org.eclipse.tractusx.bpdm.gate.service.RelationService
import org.eclipse.tractusx.bpdm.gate.service.application.RelationInputUpsertApplicationService
import org.eclipse.tractusx.bpdm.gate.util.PrincipalUtil
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto as RelationV7Dto

@RestController("RelationControllerLegacy")
class RelationController(
    private val relationshipService: RelationService,
    private val principalUtil: PrincipalUtil,
    private val inputUpsertApplicationService: RelationInputUpsertApplicationService,
    private val inputRequestMapper: RelationInputRequestMapperV6,
    private val inputDtoMapper: RelationInputDtoMapperV6,
    private val inputErrorMapper: RelationInputUpsertErrorMapperV6
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
        return relationshipService.findInputRelations(
            tenantBpnL = principalUtil.resolveTenantBpnl(),
            externalIds = externalIds ?: emptyList(),
            relationType = relationType,
            sourceBusinessPartnerExternalIds = businessPartnerSourceExternalIds ?: emptyList(),
            targetBusinessPartnerExternalIds = businessPartnerTargetExternalIds ?: emptyList(),
            updatedAtFrom = updatedAtFrom,
            paginationRequest = paginationRequest
        ).toV6Dto()
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
        relationshipService.deleteRelation(
            tenantBpnL = principalUtil.resolveTenantBpnl(),
            externalId = externalId
        )
    }

    private fun upsert(request: RelationInputUpsertRequest, mode: RelationInputUpsertMode): RelationDto =
        when (val result = inputUpsertApplicationService.upsert(listOf(request), principalUtil.resolveTenantBpnl().value, mode)) {
            is RelationInputUpsertResult.Written -> inputDtoMapper.toDto(result.relations.single())
            is RelationInputUpsertResult.Rejected -> throw inputErrorMapper.toException(result.errors)
        }

    private fun PageDto<RelationV7Dto>.toV6Dto(): PageDto<RelationDto>{
        return PageDto(
            totalElements = totalElements,
            totalPages = totalPages,
            page = page,
            contentSize = contentSize,
            content = content.map { it.toV6Dto() }
        )
    }

    private fun RelationV7Dto.toV6Dto(): RelationDto{
        return RelationDto(
                externalId = externalId,
                relationType = relationType,
                businessPartnerSourceExternalId = businessPartnerSourceExternalId,
                businessPartnerTargetExternalId = businessPartnerTargetExternalId,
                updatedAt = updatedAt,
                createdAt = createdAt
            )
    }
}