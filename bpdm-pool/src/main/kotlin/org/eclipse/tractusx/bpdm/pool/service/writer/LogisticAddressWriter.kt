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
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.AddressEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.PendingAddressWrite
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.service.BpnIssuingService
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerEquivalenceMapper
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
 * The single write primitive for logistic addresses: it owns BPN issuing (create), change detection (update) and
 * changelog creation, so no caller has to remember them. Two producers stage an unsaved write; one sink commits it:
 *
 *  - [stageCreate] issues the BPNs and builds fresh entities — always [org.eclipse.tractusx.bpdm.pool.dto.UpsertType.Created].
 *  - [stageUpdate] lets the caller mutate an existing address however it needs; the writer change-detects the mutation
 *    ([org.eclipse.tractusx.bpdm.pool.dto.UpsertType.Updated]/[org.eclipse.tractusx.bpdm.pool.dto.UpsertType.NoChange]).
 *  - [commit] saves the created/updated addresses and emits the matching CREATE/UPDATE changelog, skipping NoChange.
 *
 * The two producers do not compose in succession — a caller picks one *per* address, collects the [org.eclipse.tractusx.bpdm.pool.model.PendingAddressWrite]s,
 * wires each into its (possibly still-unsaved) cyclic parent, then [commit]s the batch. Deferring the save to [commit] is
 * what lets callers that own a cyclic parent relationship (legal entity ⇄ legal address, site ⇄ main address) wire the
 * object graph before anything is persisted, and control changelog ordering relative to the parent. The cyclic insert is
 * still resolved at flush by the nullable `logistic_address.legal_entity_id` back-FK together with `order_inserts`.
 *
 * Order-preserving positional contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class LogisticAddressWriter(
    private val bpnIssuingService: BpnIssuingService,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val changelogService: PartnerChangelogService,
    private val addressEntityMapper: AddressEntityMapper,
    private val equivalenceMapper: BusinessPartnerEquivalenceMapper
) {
    /**
     * Issues the BPNs and builds the (unsaved) address entities from already-resolved commands. No persistence and no
     * changelog — the caller wires the returned entities into their parents before calling [commit].
     */
    fun stageCreate(parsed: List<AddressCreateParsed>): List<PendingAddressWrite> {
        val bpns = bpnIssuingService.issueAddressBpns(parsed.size)
        // A freshly created address has no shared history yet, so its sharing-member count starts at zero.
        return parsed.zip(bpns) { entry, bpn ->
            PendingAddressWrite(addressEntityMapper.toEntity(bpn, entry, numberOfSharingMembers = 0), UpsertType.Created)
        }
    }

    /**
     * Applies [mutate] to an existing address and change-detects it against its before/after equivalence. The address is
     * not saved here — [commit] does that after the caller has finished wiring. Compose several changes into a single
     * [mutate] (e.g. a site assignment and a content change) so they net one ADDRESS UPDATE.
     */
    fun stageUpdate(target: LogisticAddressDb, mutate: (LogisticAddressDb) -> Unit): PendingAddressWrite {
        val before = equivalenceMapper.toEquivalenceDto(target)
        mutate(target)
        val changed = equivalenceMapper.toEquivalenceDto(target) != before
        return PendingAddressWrite(target, if (changed) UpsertType.Updated else UpsertType.NoChange)
    }

    /**
     * Persists the created/updated addresses and emits their CREATE/UPDATE changelog. NoChange entries are neither saved
     * nor logged; all entries are returned so the caller keeps the positional contract.
     */
    @Transactional
    fun commit(staged: List<PendingAddressWrite>): List<UpsertResult<LogisticAddressDb>> {
        logisticAddressRepository.saveAll(staged.filter { it.upsertType != UpsertType.NoChange }.map { it.address })

        changelogService.createChangelogEntries(staged.mapNotNull {
            when (it.upsertType) {
                UpsertType.Created -> ChangelogEntryCreateRequest(it.address.bpn, ChangelogType.CREATE, BusinessPartnerType.ADDRESS)
                UpsertType.Updated -> ChangelogEntryCreateRequest(it.address.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS)
                UpsertType.NoChange -> null
            }
        })

        return staged.map { UpsertResult(it.address, it.upsertType) }
    }
}