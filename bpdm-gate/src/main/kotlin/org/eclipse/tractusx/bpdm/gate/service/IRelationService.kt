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

package org.eclipse.tractusx.bpdm.gate.service

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.mapping.types.BpnLString
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import java.time.Instant

interface IRelationService {

    fun findRelations(
        tenantBpnL: BpnLString,
        stageType: StageType,
        externalIds: List<String> = emptyList(),
        relationType: RelationType? = null,
        sourceBusinessPartnerExternalIds: List<String> = emptyList(),
        targetBusinessPartnerExternalIds: List<String> = emptyList(),
        updatedAtFrom: Instant? = null,
        paginationRequest: PaginationRequest
    ): PageDto<RelationDto>

    fun upsertRelations(tenantBpnL: BpnLString, stageType: StageType, relations: List<RelationPutEntry>): List<RelationDto>

    fun updateRelations(tenantBpnL: BpnLString, stageType: StageType, relations: List<RelationPutEntry>): List<RelationDto>

    fun checkConstraints(
        tenantBpnL: BpnLString,
        stageType: StageType,
        businessPartnerExternalIds: List<String>
    ): List<ConstraintError>

    data class ConstraintError(
        val externalId: String,
        val errorType: ConstraintErrorType,
        val erroneousValue: String
    ){
        val message = errorType.msg
    }

    enum class ConstraintErrorType(val msg: String){
        SourceEqualsTarget("Source and target are the same"),
        SourceNotOwnCompanyData("Source is not own company data"),
        TargetNotOwnCompanyData("Target is not own company data"),
        SourceNotAllowedType("Source is not of allowed business partner type"),
        TargetNotAllowedType("Target is not of allowed business partner type"),
        SourceUsedAsTarget("Source is already used as target of relationship type"),
        TargetUsedAsSource("Target is already used as source of relationship type"),
        TargetMultipleSources("Target has multiple sources"),
        SourceMultipleTargets("Source has multiple targets"),
    }
}