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
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.SiteEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.AddressContentUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteContentParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteUpdateParsed
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerEquivalenceMapper
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Updates sites — the composite site-update *operation*. It consumes a [SiteUpdateParsed] command (target resolved,
 * header + main-address content validated by [org.eclipse.tractusx.bpdm.pool.service.parser.SiteUpdateParser]), applies
 * the header change under before/after change detection, and delegates the main-address change to
 * [AddressContentUpdateService], netting a single UPDATE when either side changed. Order-preserving positional contract
 * (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class SiteUpdateService(
    private val addressContentUpdateService: AddressContentUpdateService,
    private val siteRepository: SiteRepository,
    private val changelogService: PartnerChangelogService,
    private val equivalenceMapper: BusinessPartnerEquivalenceMapper,
    private val siteEntityMapper: SiteEntityMapper
) {

    /**
     * Returns the updated entities (within the caller's transaction) rather than a detached response model: building
     * version-specific responses is the job of the border/application service at the edge.
     */
    @Transactional
    fun update(parsed: List<SiteUpdateParsed>): List<UpsertResult<SiteDb>>{
        val siteResults = parsed.map { update(it) }

        val mainAddressRequests = parsed.map {
            AddressContentUpdateParsed(
                it.target.mainAddress,
                it.content.mainAddress.address,
                it.content.mainAddress.scriptVariants,
                true
            )
        }
        val mainAddressResults = addressContentUpdateService.update(mainAddressRequests)

        return siteResults.zip(mainAddressResults){ siteResult, mainAddressResult ->
            val changed = siteResult.upsertType == UpsertType.Updated || mainAddressResult.upsertType == UpsertType.Updated
            UpsertResult(siteResult.value, if (changed) UpsertType.Updated else UpsertType.NoChange)
        }
    }

    private fun update(parsed: SiteUpdateParsed): UpsertResult<SiteDb> {
        val target = parsed.target

        val before = equivalenceMapper.toEquivalenceDto(target)
        doUpdateEntity(target, parsed.content)
        val after = equivalenceMapper.toEquivalenceDto(target)

        val upsertType = if (before != after) {
            siteRepository.save(target)
            changelogService.createChangelogEntries(listOf(ChangelogEntryCreateRequest(target.bpn, ChangelogType.UPDATE, BusinessPartnerType.SITE)))
            UpsertType.Updated
        } else {
            UpsertType.NoChange
        }

        return UpsertResult(target, upsertType)
    }

    private fun doUpdateEntity(target: SiteDb, content: SiteContentParsed) {
        val header = content.header
        target.name = header.name
        // The sharing-member count is Pool-maintained, not part of the update payload, so carry the current value forward.
        target.confidenceCriteria = siteEntityMapper.toConfidence(header.confidenceCriteria, target.confidenceCriteria.numberOfSharingMembers)
        target.states.replace(siteEntityMapper.toStates(header.states, target))
        target.scriptVariants.replace(siteEntityMapper.toScriptVariants(header.scriptVariants))
    }
}