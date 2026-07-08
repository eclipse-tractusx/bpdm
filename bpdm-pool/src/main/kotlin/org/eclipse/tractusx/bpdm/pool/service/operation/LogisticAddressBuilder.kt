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
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.AddressEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.service.BpnIssuingService
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The two-phase primitive for materialising a logistic address, so a caller that owns a cyclic parent relationship
 * (e.g. [LegalEntityCreateService], whose legal entity and legal address reference each other) can wire the object graph
 * without reasoning about persistence order:
 *
 *  1. [build] issues the BPNs and constructs the still-unsaved entities. The caller may then connect each entity to its
 *     (also still-unsaved) parent freely — nothing is persisted yet, so there is no save-ordering constraint.
 *  2. [persist] saves the entities and emits the changelog. The caller decides *when* this happens relative to saving the
 *     parent, which is how it controls changelog ordering.
 *
 * The cyclic insert (legal entity ⇄ legal address / site ⇄ main address) is still resolved at flush by the nullable
 * `logistic_address.legal_entity_id` back-FK together with `order_inserts` — that nullable FK remains load-bearing.
 *
 * Create-only for now: [build] always yields [UpsertType.Created]. [persist] already branches on the change state so the
 * update path (which may produce [UpsertType.Updated]/[UpsertType.NoChange]) can slot in later without touching callers.
 *
 * Order-preserving positional contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class LogisticAddressBuilder(
    private val bpnIssuingService: BpnIssuingService,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val changelogService: PartnerChangelogService,
    private val addressEntityMapper: AddressEntityMapper
) {

    /**
     * Issues the BPNs and builds the (unsaved) address entities from already-resolved commands. No persistence and no
     * changelog — the caller wires the returned entities into their parents before calling [persist].
     */
    fun build(parsed: List<AddressCreateParsed>): List<UpsertResult<LogisticAddressDb>> {
        val bpns = bpnIssuingService.issueAddressBpns(parsed.size)
        // A freshly created address has no shared history yet, so its sharing-member count starts at zero.
        return parsed.zip(bpns) { entry, bpn ->
            UpsertResult(addressEntityMapper.toEntity(bpn, entry, numberOfSharingMembers = 0), UpsertType.Created)
        }
    }

    /**
     * Persists the built entities and emits their CREATE changelog. Only [UpsertType.Created] entries are saved and
     * logged; other states are the update path's concern and are passed through untouched.
     */
    @Transactional
    fun persist(built: List<UpsertResult<LogisticAddressDb>>): List<UpsertResult<LogisticAddressDb>> {
        val created = built.filter { it.upsertType == UpsertType.Created }.map { it.value }

        logisticAddressRepository.saveAll(created)
        changelogService.createChangelogEntries(created.map {
            ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.ADDRESS)
        })

        return built
    }
}
