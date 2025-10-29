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
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import org.eclipse.tractusx.bpdm.gate.api.v6.GateRelationApi
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPostRequest
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.RelationDto
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.service.RelationService
import org.eclipse.tractusx.bpdm.gate.util.PrincipalUtil
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto as RelationV7Dto

@RestController("RelationControllerLegacy")
class RelationController(
    private val relationshipService: RelationService,
    private val principalUtil: PrincipalUtil
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
    ): RelationDto {
        return relationshipService.createInputRelations(
            tenantBpnL = principalUtil.resolveTenantBpnl(),
            externalId = requestBody.externalId,
            relationType = requestBody.relationType,
            sourceBusinessPartnerExternalId = requestBody.businessPartnerSourceExternalId,
            targetBusinessPartnerExternalId = requestBody.businessPartnerTargetExternalId
        ).toV6Dto()

    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_INPUT_RELATION})")
    override fun put(
        createIfNotExist: Boolean,
        requestBody: RelationPutEntry
    ): RelationDto {
        val upsertedRelation = if(createIfNotExist) {
            relationshipService.upsertInputRelations(
                tenantBpnL = principalUtil.resolveTenantBpnl(),
                relations = listOf(RelationPutEntry(
                    externalId = requestBody.externalId,
                    relationType = requestBody.relationType,
                    businessPartnerSourceExternalId = requestBody.businessPartnerSourceExternalId,
                    businessPartnerTargetExternalId = requestBody.businessPartnerTargetExternalId
                ))
            ).single()
        } else {
            relationshipService.updateInputRelations(
                tenantBpnL = principalUtil.resolveTenantBpnl(),
                relations = listOf(RelationPutEntry(
                    externalId = requestBody.externalId,
                    relationType = requestBody.relationType,
                    businessPartnerSourceExternalId = requestBody.businessPartnerSourceExternalId,
                    businessPartnerTargetExternalId = requestBody.businessPartnerTargetExternalId
                ))
            ).single()
        }

        return upsertedRelation.toV6Dto()
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_INPUT_RELATION})")
    override fun delete(externalId: String) {
        relationshipService.deleteRelation(
            tenantBpnL = principalUtil.resolveTenantBpnl(),
            externalId = externalId
        )
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