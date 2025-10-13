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

package org.eclipse.tractusx.bpdm.gate.service

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.mapping.types.BpnLString
import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.SharableRelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import java.time.Instant

interface IRelationService {

    fun findInputRelations(
        tenantBpnL: BpnLString,
        externalIds: List<String> = emptyList(),
        relationType: RelationType? = null,
        sourceBusinessPartnerExternalIds: List<String> = emptyList(),
        targetBusinessPartnerExternalIds: List<String> = emptyList(),
        updatedAtFrom: Instant? = null,
        paginationRequest: PaginationRequest
    ): PageDto<RelationDto>

    fun findOutputRelations(
        tenantBpnL: BpnLString,
        externalIds: List<String> = emptyList(),
        relationType: SharableRelationType? = null,
        sourceBpnLs: List<String> = emptyList(),
        targetBpnLs: List<String> = emptyList(),
        updatedAtFrom: Instant? = null,
        paginationRequest: PaginationRequest
    ): PageDto<RelationOutputDto>

    fun upsertInputRelations(tenantBpnL: BpnLString, relations: List<RelationPutEntry>): List<RelationDto>

    fun updateInputRelations(tenantBpnL: BpnLString, relations: List<RelationPutEntry>): List<RelationDto>

    fun upsertOutputRelations(relations: List<RelationUpsertRequest>): List<RelationDb>

    data class RelationUpsertRequest(
        val relation: RelationDb,
        val relationType: SharableRelationType,
        val businessPartnerSourceExternalId: String,
        val businessPartnerTargetExternalId: String
    )
}