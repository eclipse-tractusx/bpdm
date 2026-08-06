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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.exception.BpdmMultipleNotFoundException
import org.eclipse.tractusx.bpdm.common.mapping.ValidationContext.Companion.fromRoot
import org.eclipse.tractusx.bpdm.common.mapping.types.BpnLString
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.DataSpaceParticipantDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.DataSpaceParticipantUpdateRequest
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DataSpaceParticipantsService(
    private val legalEntityRepository: LegalEntityRepository,
    private val changelogService: PartnerChangelogService
) {
    @Transactional
    fun updateMemberships(updateRequest: DataSpaceParticipantUpdateRequest){
        BpnLString.assert(
            updateRequest.participants.map { it.bpnL },
            fromRoot(DataSpaceParticipantUpdateRequest::class, "updateRequest", DataSpaceParticipantUpdateRequest::participants, DataSpaceParticipantDto::bpnL)
        )

        val updatesByBpnL = updateRequest.participants.associate { Pair(it.bpnL, it.isDataSpaceParticipant) }

        val foundLegalEntities = legalEntityRepository.findDistinctByBpnIn(updatesByBpnL.keys)

        val notFoundBpnLs = updatesByBpnL.keys.minus( foundLegalEntities.map { it.bpn }.toSet())
        if(notFoundBpnLs.isNotEmpty())
            throw BpdmMultipleNotFoundException("Legal Entities", notFoundBpnLs)

        foundLegalEntities.forEach { legalEntity ->
            val updateValue = updatesByBpnL[legalEntity.bpn]!!
            if(legalEntity.isDataSpaceParticipant != updateValue){
                legalEntity.isDataSpaceParticipant = updateValue
                legalEntityRepository.save(legalEntity)

                changelogService.createChangelogEntry(ChangelogEntryCreateRequest(
                    legalEntity.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY
                ))
            }
        }
    }
}