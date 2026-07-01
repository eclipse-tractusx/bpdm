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
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.AddressSiteAssignment
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Assigns a site as a member of an *existing* logistic address — the shared "an address gained a site" step, reusable by
 * the site-create paths (e.g. [SiteCreateWithLegalAddressAsMainService], which reuses a legal address verbatim) and the
 * address-update services. It mutates only the address's `sites` membership; it does **not** set `SiteDb.mainAddress`
 * (main-ness is the site's concern) so it is equally usable for additional-address assignment.
 *
 * Membership is part of the address's state (see [BusinessPartnerEquivalenceMapper] — `sites` is in the address
 * equivalence), so the assignment is change-detected against the same before/after equivalence as [AddressUpdateService]:
 * it emits **at most one** ADDRESS UPDATE changelog, and only when the site was genuinely newly added (an idempotent
 * re-assign is a `NoChange`). Mirrors [AddressUpdateService]'s `UpsertResult` semantics.
 *
 * Scope/usage constraints:
 * - The target address must already be persistent. A freshly created address records its site at creation time
 *   (`ADDRESS CREATE` covers it), so those paths do not route through here.
 * - [assign] owns its own changelog, so do not call it on an address that another service also changelogs in the same
 *   transaction. To combine a membership change with a content change, reuse the pure [applyTo] mutation under the
 *   caller's single change detection instead (this is what [AddressUpdateService] does), so the two changes net one
 *   ADDRESS UPDATE.
 *
 * Order-preserving positional contract (see [ParseResult]).
 */
@Service
class AddressSiteAssignmentService(
    private val logisticAddressRepository: LogisticAddressRepository,
    private val changelogService: PartnerChangelogService
) {

    @Transactional
    fun assign(assignments: List<AddressSiteAssignment>): List<UpsertResult<LogisticAddressDb>> =
        assignments.map { assign(it) }

    private fun assign(assignment: AddressSiteAssignment): UpsertResult<LogisticAddressDb> {
        val address = assignment.address

        if(address.sites.contains(assignment.site)) return UpsertResult(address, UpsertType.NoChange)

        address.sites.add(assignment.site)

        logisticAddressRepository.save(address)
        changelogService.createChangelogEntries(
            listOf(ChangelogEntryCreateRequest(address.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS))
        )

        return UpsertResult(address, UpsertType.Updated)
    }
}
