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

package org.eclipse.tractusx.bpdm.pool.service.writer

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.LegalEntityEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.PendingLegalEntityWrite
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.service.BpnIssuingService
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerEquivalenceMapper
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

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
/**
 * The single write primitive for legal entities, analogous to [LogisticAddressWriter] and [SiteWriter]: it owns
 * legal-entity-BPN issuing (create), change detection (update) and LEGAL_ENTITY changelog creation, so no caller has to
 * remember them. Two producers stage an unsaved write; one sink commits it:
 *
 *  - [stageCreate] issues the BPNs and builds fresh entities — always [org.eclipse.tractusx.bpdm.pool.dto.UpsertType.Created]. The returned legal entities
 *    have no `legalAddress` yet; the caller must wire it before [commit], since `legal_entities.legal_address_id` is
 *    non-null.
 *  - [stageUpdate] lets the caller mutate an existing legal entity however it needs; the writer change-detects the
 *    mutation ([org.eclipse.tractusx.bpdm.pool.dto.UpsertType.Updated]/[org.eclipse.tractusx.bpdm.pool.dto.UpsertType.NoChange]).
 *  - [commit] saves the created/updated legal entities and emits the matching CREATE/UPDATE changelog, skipping NoChange.
 *
 * As with the other writers, the two producers do not compose in succession — a caller picks one *per* legal entity,
 * wires each to its (still-unsaved) legal address, then [commit]s the batch. Committing the legal entity before its legal
 * address both keeps the LEGAL_ENTITY changelog ahead of the ADDRESS changelog and lets `cascade = ALL` on
 * `LegalEntityDb.legalAddress` persist the freshly created legal address at flush.
 *
 * Order-preserving positional contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class LegalEntityWriter(
    private val bpnIssuingService: BpnIssuingService,
    private val legalEntityRepository: LegalEntityRepository,
    private val changelogService: PartnerChangelogService,
    private val legalEntityEntityMapper: LegalEntityEntityMapper,
    private val equivalenceMapper: BusinessPartnerEquivalenceMapper
) {
    /**
     * Issues the BPNs and builds the (unsaved) legal-entity entities from already-resolved headers. No persistence and no
     * changelog — the caller wires each legal entity's legal address before calling [commit].
     */
    fun stageCreate(headers: List<LegalEntityHeaderParsed>): List<PendingLegalEntityWrite> {
        val bpns = bpnIssuingService.issueLegalEntityBpns(headers.size)
        val currentness = Instant.now().truncatedTo(ChronoUnit.MICROS)
        // A new legal entity's confidence starts with zero sharing members (preserves the previous create behavior).
        return headers.zip(bpns) { header, bpn ->
            PendingLegalEntityWrite(legalEntityEntityMapper.toEntity(bpn, header, currentness, numberOfSharingMembers = 0), UpsertType.Created)
        }
    }

    /**
     * Applies [mutate] to an existing legal entity and change-detects it against its before/after equivalence. The entity
     * is not saved here — [commit] does that.
     */
    fun stageUpdate(target: LegalEntityDb, mutate: (LegalEntityDb) -> Unit): PendingLegalEntityWrite {
        val before = equivalenceMapper.toEquivalenceDto(target)
        mutate(target)
        val changed = equivalenceMapper.toEquivalenceDto(target) != before
        return PendingLegalEntityWrite(target, if (changed) UpsertType.Updated else UpsertType.NoChange)
    }

    /**
     * Persists the created/updated legal entities and emits their CREATE/UPDATE changelog. NoChange entries are neither
     * saved nor logged; all entries are returned so the caller keeps the positional contract.
     */
    @Transactional
    fun commit(staged: List<PendingLegalEntityWrite>): List<UpsertResult<LegalEntityDb>> {
        legalEntityRepository.saveAll(staged.filter { it.upsertType != UpsertType.NoChange }.map { it.legalEntity })

        changelogService.createChangelogEntries(staged.mapNotNull {
            when (it.upsertType) {
                UpsertType.Created -> ChangelogEntryCreateRequest(it.legalEntity.bpn, ChangelogType.CREATE, BusinessPartnerType.LEGAL_ENTITY)
                UpsertType.Updated -> ChangelogEntryCreateRequest(it.legalEntity.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY)
                UpsertType.NoChange -> null
            }
        })

        return staged.map { UpsertResult(it.legalEntity, it.upsertType) }
    }
}