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
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.SiteEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.PendingSiteWrite
import org.eclipse.tractusx.bpdm.pool.model.SiteHeaderCreateParsed
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
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
 * The single write primitive for sites, analogous to [LogisticAddressWriter]: it owns site-BPN issuing (create), change
 * detection (update) and SITE changelog creation, so no caller has to remember them. Two producers stage an unsaved
 * write; one sink commits it:
 *
 *  - [stageCreate] issues the site BPNs and builds fresh entities — always [org.eclipse.tractusx.bpdm.pool.dto.UpsertType.Created]. The returned sites have
 *    no `mainAddress` yet; the caller must wire it before [commit], since `site.main_address_id` is non-null.
 *  - [stageUpdate] lets the caller mutate an existing site however it needs; the writer change-detects the mutation
 *    ([org.eclipse.tractusx.bpdm.pool.dto.UpsertType.Updated]/[org.eclipse.tractusx.bpdm.pool.dto.UpsertType.NoChange]).
 *  - [commit] saves the created/updated sites and emits the matching SITE CREATE/UPDATE changelog, skipping NoChange.
 *
 * As with the address writer, the two producers do not compose in succession — a caller picks one *per* site, wires each
 * site to its (possibly still-unsaved) main address, then [commit]s the batch. Deferring the save to [commit] lets the
 * caller wire the site ⇄ main-address cycle before anything is persisted; committing the site before its main address
 * both keeps the SITE changelog ahead of the ADDRESS changelog and lets `cascade = ALL` on `SiteDb.mainAddress` persist a
 * freshly created main address at flush.
 *
 * Order-preserving positional contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class SiteWriter(
    private val bpnIssuingService: BpnIssuingService,
    private val siteRepository: SiteRepository,
    private val changelogService: PartnerChangelogService,
    private val siteEntityMapper: SiteEntityMapper,
    private val equivalenceMapper: BusinessPartnerEquivalenceMapper
) {
    /**
     * Issues the site BPNs and builds the (unsaved) site entities from already-resolved headers. No persistence and no
     * changelog — the caller wires each site's main address before calling [commit].
     */
    fun stageCreate(parsed: List<SiteHeaderCreateParsed>): List<PendingSiteWrite> {
        val bpns = bpnIssuingService.issueSiteBpns(parsed.size)
        // A new site's confidence starts with one sharing member (preserves the previous create behavior).
        return parsed.zip(bpns) { entry, bpn ->
            PendingSiteWrite(siteEntityMapper.toEntity(bpn, entry.legalEntity, entry.header, numberOfSharingMembers = 1), UpsertType.Created)
        }
    }

    /**
     * Applies [mutate] to an existing site and change-detects it against its before/after equivalence. The site is not
     * saved here — [commit] does that.
     */
    fun stageUpdate(target: SiteDb, mutate: (SiteDb) -> Unit): PendingSiteWrite {
        val before = equivalenceMapper.toEquivalenceDto(target)
        mutate(target)
        val changed = equivalenceMapper.toEquivalenceDto(target) != before
        return PendingSiteWrite(target, if (changed) UpsertType.Updated else UpsertType.NoChange)
    }

    /**
     * Persists the created/updated sites and emits their CREATE/UPDATE changelog. NoChange entries are neither saved nor
     * logged; all entries are returned so the caller keeps the positional contract.
     */
    @Transactional
    fun commit(staged: List<PendingSiteWrite>): List<UpsertResult<SiteDb>> {
        siteRepository.saveAll(staged.filter { it.upsertType != UpsertType.NoChange }.map { it.site })

        changelogService.createChangelogEntries(staged.mapNotNull {
            when (it.upsertType) {
                UpsertType.Created -> ChangelogEntryCreateRequest(it.site.bpn, ChangelogType.CREATE, BusinessPartnerType.SITE)
                UpsertType.Updated -> ChangelogEntryCreateRequest(it.site.bpn, ChangelogType.UPDATE, BusinessPartnerType.SITE)
                UpsertType.NoChange -> null
            }
        })

        return staged.map { UpsertResult(it.site, it.upsertType) }
    }
}