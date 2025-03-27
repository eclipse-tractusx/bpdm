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
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.entity.RelationSharingStateDb
import org.eclipse.tractusx.bpdm.gate.exception.BpdmMissingRelationSharingStateException
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RelationSharingStateService(
    private val relationRepository: RelationRepository
) {

    fun findSharingStates(
        tenantBpnL: BpnLString,
        externalIds: Collection<String> = emptyList(),
        sharingStateTypes: Collection<RelationSharingStateType> = emptyList(),
        updatedAfter: Instant? = null,
        paginationRequest: PaginationRequest
    ): PageDto<RelationSharingStateDto>{
        val specs = Specification.allOf(
            RelationRepository.Specs.bySharingStateNotNull(),
            RelationRepository.Specs.byTenantBpnL(tenantBpnL.value),
            RelationRepository.Specs.byExternalIds(externalIds),
            RelationRepository.Specs.bySharingStateTypes(sharingStateTypes),
            RelationRepository.Specs.bySharingStateUpdatedAfter(updatedAfter)
        )

        val relationPage = relationRepository.findAll(specs, PageRequest.of(paginationRequest.page, paginationRequest.size))
        return relationPage.toPageDto{ it.toSharingStateDto() }
    }

    private fun RelationDb.toSharingStateDto(): RelationSharingStateDto{
        return sharingState?.toDto(externalId) ?: throw BpdmMissingRelationSharingStateException(externalId, tenantBpnL)
    }

    private fun RelationSharingStateDb.toDto(externalId: String): RelationSharingStateDto{
        return RelationSharingStateDto(
            externalId, sharingStateType, sharingErrorCode, sharingErrorMessage, updatedAt
        )
    }

    fun setInitial(relation: RelationDb, relationType: RelationType){
        relation.sharingState = RelationSharingStateDb(
            sharingStateType = RelationSharingStateType.Ready,
            sharingErrorCode = null,
            sharingErrorMessage = null,
            updatedAt = Instant.now()
        ).takeIf { canBeShared(relationType) }

        relationRepository.save(relation)
    }

    private fun canBeShared(relationType: RelationType): Boolean{
        return when(relationType){
            RelationType.IsManagedBy -> false
            RelationType.IsAlternativeHeadquarterFor -> true
        }
    }
}