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

package org.eclipse.tractusx.bpdm.pool.service.operation

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.LegalEntityEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityHeaderParsed
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.service.BpnIssuingService
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * The single authority for creating legal entities, the top of the business-partner hierarchy, together with their
 * legal address: issues the legal-entity BPN, builds and persists the entity and its legal address, and emits their
 * CREATE changelogs.
 */
@Service
class LegalEntityCreateService(
    private val addressCreateService: AddressCreateService,
    private val legalEntityEntityMapper: LegalEntityEntityMapper,
    private val bpnIssuingService: BpnIssuingService,
    private val changelogService: PartnerChangelogService,
    private val legalEntityRepository: LegalEntityRepository
) {

    @Transactional
    fun create(parsed: List<LegalEntityCreateParsed>): List<LegalEntityDb> {
        val legalEntities = createHeaders(parsed.map { it.content.header })
        val stagedAddresses = addressCreateService.stageCreate(parsed.zip(legalEntities).map { (entry, legalEntity) ->
            AddressCreateParsed(legalEntity, site = null, entry.content.legalAddress)
        })

        legalEntities.zip(stagedAddresses).forEach { (legalEntity, stagedAddress) -> legalEntity.legalAddress = stagedAddress.address }

        changelogService.createChangelogEntries(legalEntities.map { ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.LEGAL_ENTITY) })
        legalEntityRepository.saveAll(legalEntities)

        addressCreateService.commit(stagedAddresses)

        return legalEntities
    }

    private fun createHeaders(headers: List<LegalEntityHeaderParsed>): List<LegalEntityDb>{
        val bpns = bpnIssuingService.issueLegalEntityBpns(headers.size)
        val currentness = Instant.now().truncatedTo(ChronoUnit.MICROS)
        // A new legal entity starts with zero sharing members.
        return headers.zip(bpns) { header, bpn ->
            legalEntityEntityMapper.toEntity(bpn, header, currentness, numberOfSharingMembers = 0)
        }
    }
}
