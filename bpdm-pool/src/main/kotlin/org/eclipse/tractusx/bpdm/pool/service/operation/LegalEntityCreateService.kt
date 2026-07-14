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
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.LegalEntityEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.PendingLegalEntityWrite
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.service.BpnIssuingService
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Creates legal entities, the top of the business-partner hierarchy — the single owner of the legal-entity-create
 * *operation*. It consumes a [LegalEntityCreateParsed] command (header + legal-address content already validated by
 * [org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntityCreateParser]) and persists the legal entity and its legal
 * address. Both the legal entity (via [LegalEntityEntityMapper]) and its legal address
 * ([LogisticAddressStagedCreateService]) are staged unsaved so the legal entity ⇄ legal address cycle can be wired in
 * memory before persisting. Order-preserving positional contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class LegalEntityCreateService(
    private val addressStagedCreateService: LogisticAddressStagedCreateService,
    private val legalEntityEntityMapper: LegalEntityEntityMapper,
    private val bpnIssuingService: BpnIssuingService,
    private val changelogService: PartnerChangelogService,
    private val legalEntityRepository: LegalEntityRepository
) {

    /**
     * Returns the persisted entities (within the caller's transaction) rather than a detached response model: building
     * version-specific responses is the job of the border/application service at the edge.
     */
    @Transactional
    fun create(parsed: List<LegalEntityCreateParsed>): List<LegalEntityDb> {
        val legalEntities = createHeaders(parsed.map { it.content.header })
        val stagedAddresses = addressStagedCreateService.stageCreate(parsed.zip(legalEntities).map { (entry, legalEntity) ->
            val legalAddress = entry.content.legalAddress
            AddressCreateParsed(legalEntity, site = null, legalAddress.address, legalAddress.scriptVariants)
        })

        legalEntities.zip(stagedAddresses).forEach { (legalEntity, stagedAddress) -> legalEntity.legalAddress = stagedAddress.address }

        changelogService.createChangelogEntries(legalEntities.map { ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.LEGAL_ENTITY) })
        legalEntityRepository.saveAll(legalEntities)

        addressStagedCreateService.commit(stagedAddresses)

        return legalEntities
    }

    private fun createHeaders(headers: List<LegalEntityHeaderParsed>): List<LegalEntityDb>{
        val bpns = bpnIssuingService.issueLegalEntityBpns(headers.size)
        val currentness = Instant.now().truncatedTo(ChronoUnit.MICROS)
        // A new legal entity's confidence starts with zero sharing members (preserves the previous create behavior).
        return headers.zip(bpns) { header, bpn ->
            legalEntityEntityMapper.toEntity(bpn, header, currentness, numberOfSharingMembers = 0)
        }
    }
}
