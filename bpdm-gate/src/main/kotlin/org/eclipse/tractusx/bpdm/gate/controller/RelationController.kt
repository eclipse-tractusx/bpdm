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
import org.eclipse.tractusx.bpdm.gate.api.GateRelationApi
import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.RelationPutResponse
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.service.IRelationService
import org.eclipse.tractusx.bpdm.gate.util.PrincipalUtil
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class RelationController(
    private val relationshipService: IRelationService,
    private val principalUtil: PrincipalUtil
): GateRelationApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_INPUT_RELATION})")
    override fun postSearch(
        searchRequest: RelationSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<RelationDto> {
       return relationshipService.findInputRelations(
           tenantBpnL = principalUtil.resolveTenantBpnl(),
           externalIds = searchRequest.externalIds ?: emptyList(),
           relationType = searchRequest.relationType,
           sourceBusinessPartnerExternalIds = searchRequest.businessPartnerSourceExternalIds ?: emptyList(),
           targetBusinessPartnerExternalIds = searchRequest.businessPartnerTargetExternalIds ?: emptyList(),
           updatedAtFrom = searchRequest.updatedAtFrom,
           paginationRequest = paginationRequest
       )
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_INPUT_RELATION})")
    override fun put(
        createIfNotExist: Boolean,
        requestBody: RelationPutRequest
    ): RelationPutResponse {
        val upsertedRelations = if(createIfNotExist){
            relationshipService.upsertInputRelations(
                tenantBpnL = principalUtil.resolveTenantBpnl(),
                relations = requestBody.relations
            )
        }else{
            relationshipService.updateInputRelations(
                tenantBpnL = principalUtil.resolveTenantBpnl(),
                relations = requestBody.relations
            )
        }

        return RelationPutResponse(upsertedRelations)
    }
}