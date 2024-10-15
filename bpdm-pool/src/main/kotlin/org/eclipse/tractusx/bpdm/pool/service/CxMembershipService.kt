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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.exception.BpdmMultipleNotFoundException
import org.eclipse.tractusx.bpdm.common.mapping.ValidationContext.Companion.fromRoot
import org.eclipse.tractusx.bpdm.common.mapping.types.BpnLString
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.CxMembershipDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.CxMembershipSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.CxMembershipUpdateRequest
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CxMembershipService(
    private val legalEntityRepository: LegalEntityRepository,
    private val changelogService: PartnerChangelogService
) {
    fun searchMemberships(searchRequest: CxMembershipSearchRequest, paginationRequest: PaginationRequest): PageDto<CxMembershipDto>{
        searchRequest.bpnLs?.let { BpnLString.assert(it, fromRoot(CxMembershipSearchRequest::class, "searchRequest", CxMembershipSearchRequest::bpnLs)) }

        val spec = Specification.allOf(
            LegalEntityRepository.byBpns(searchRequest.bpnLs),
            LegalEntityRepository.byIsMember(searchRequest.isCatenaXMember)
        )
        val legalEntityPage = legalEntityRepository.findAll(spec, PageRequest.of(paginationRequest.page, paginationRequest.size))

        return legalEntityPage.toPageDto { CxMembershipDto(it.bpn, it.isCatenaXMemberData) }
    }

    @Transactional
    fun updateMemberships(updateRequest: CxMembershipUpdateRequest){
        BpnLString.assert(
            updateRequest.memberships.map { it.bpnL },
            fromRoot(CxMembershipUpdateRequest::class, "updateRequest", CxMembershipUpdateRequest::memberships, CxMembershipDto::bpnL)
        )

        val updatesByBpnL = updateRequest.memberships.associate { Pair(it.bpnL, it.isCatenaXMember) }

        val foundLegalEntities = legalEntityRepository.findDistinctByBpnIn(updatesByBpnL.keys)

        val notFoundBpnLs = updatesByBpnL.keys.minus( foundLegalEntities.map { it.bpn }.toSet())
        if(notFoundBpnLs.isNotEmpty())
            throw BpdmMultipleNotFoundException("Legal Entities", notFoundBpnLs)

        foundLegalEntities.forEach { legalEntity ->
            val updateValue = updatesByBpnL[legalEntity.bpn]!!
            if(legalEntity.isCatenaXMemberData != updateValue){
                legalEntity.isCatenaXMemberData = updateValue
                legalEntityRepository.save(legalEntity)

                changelogService.createChangelogEntry(ChangelogEntryCreateRequest(
                    legalEntity.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY
                ))
            }
        }
    }
}