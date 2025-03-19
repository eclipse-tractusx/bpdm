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

package org.eclipse.tractusx.bpdm.gate.controller

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.GateRelationApi
import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPostRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.service.RelationService
import org.eclipse.tractusx.bpdm.gate.util.PrincipalUtil
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
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
       return relationshipService.findRelations(
           tenantBpnL = principalUtil.resolveTenantBpnl(),
           stageType = StageType.Input,
           externalIds = externalIds ?: emptyList(),
           relationType = relationType,
           sourceBusinessPartnerExternalIds = businessPartnerSourceExternalIds ?: emptyList(),
           targetBusinessPartnerExternalIds = businessPartnerTargetExternalIds ?: emptyList(),
           updatedAtFrom = updatedAtFrom,
           paginationRequest = paginationRequest
       )
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_INPUT_RELATION})")
    override fun post(
        requestBody: RelationPostRequest
    ): RelationDto {
       return relationshipService.createRelation(
           tenantBpnL = principalUtil.resolveTenantBpnl(),
           stageType = StageType.Input,
           externalId = requestBody.externalId,
           relationType = requestBody.relationType,
           sourceBusinessPartnerExternalId = requestBody.businessPartnerSourceExternalId,
           targetBusinessPartnerExternalId = requestBody.businessPartnerTargetExternalId
       )
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_INPUT_RELATION})")
    override fun put(
        createIfNotExist: Boolean,
        requestBody: RelationPutRequest
    ): RelationDto {
        return if(createIfNotExist){
            relationshipService.upsertRelation(
                tenantBpnL = principalUtil.resolveTenantBpnl(),
                stageType = StageType.Input,
                externalId = requestBody.externalId,
                relationType = requestBody.relationType,
                sourceBusinessPartnerExternalId = requestBody.businessPartnerSourceExternalId,
                targetBusinessPartnerExternalId = requestBody.businessPartnerTargetExternalId
            )
        }else{
            relationshipService.updateRelation(
                tenantBpnL = principalUtil.resolveTenantBpnl(),
                stageType = StageType.Input,
                externalId = requestBody.externalId,
                relationType = requestBody.relationType,
                sourceBusinessPartnerExternalId = requestBody.businessPartnerSourceExternalId,
                targetBusinessPartnerExternalId = requestBody.businessPartnerTargetExternalId
            )
        }
    }
}