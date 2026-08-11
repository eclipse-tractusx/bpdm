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
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteScriptVariantDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteStateDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.SiteEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.ChangelogRecord
import org.eclipse.tractusx.bpdm.pool.model.update.AddressUpdate
import org.eclipse.tractusx.bpdm.pool.model.update.SiteHeaderUpdate
import org.eclipse.tractusx.bpdm.pool.model.update.SiteUpdate
import org.eclipse.tractusx.bpdm.pool.model.update.ifSet
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The single authority for writing an existing site, treated as a whole aggregate. A caller describes the change as
 * data — a header change plus a change for the main address, either of which may leave its side alone — and this
 * service, not the caller, decides how each field is applied. It detects whether the aggregate changed, persists it, and
 * emits exactly one SITE changelog for those that did; every writer reuses it, so none can forget to log.
 */
@Service
class SiteUpdateService(
    private val addressUpdateService: AddressUpdateService,
    private val siteEntityMapper: SiteEntityMapper,
    private val siteRepository: SiteRepository,
    private val changelogCreateService: ChangelogCreateService
) {

    /**
     * Applies the given changes and reports for each site whether the aggregate — its header or its main address —
     * actually changed.
     */
    @Transactional
    fun update(requests: List<SiteUpdate>): List<UpsertResult<SiteDb>> {
        val headerUpdates = requests.map { updateHeader(it) }
        val mainAddressUpdates = addressUpdateService.update(requests.map { AddressUpdate(it.site.mainAddress, it.mainAddress) })

        val siteChangeResults = headerUpdates.zip(mainAddressUpdates) { headerResult, mainAddressResult ->
            val hasChanged = headerResult.upsertType != UpsertType.NoChange || mainAddressResult.upsertType != UpsertType.NoChange
            UpsertResult(headerResult.value, if (hasChanged) UpsertType.Updated else UpsertType.NoChange)
        }

        val updatedSites = siteChangeResults.filter { it.upsertType == UpsertType.Updated }

        siteRepository.saveAll(updatedSites.map { it.value })
        changelogCreateService.record(updatedSites.map { ChangelogRecord(it.value.bpn, ChangelogType.UPDATE, BusinessPartnerType.SITE) })

        return siteChangeResults
    }

    private fun updateHeader(request: SiteUpdate): UpsertResult<SiteDb> {
        // The verdict is taken before the change is applied, but the change is applied either way: a field that does
        // not count towards the verdict is still written.
        val changed = willChange(request.site, request.header)
        applyHeader(request.site, request.header)

        return UpsertResult(request.site, if (changed) UpsertType.Updated else UpsertType.NoChange)
    }

    /**
     * Reports whether applying the given change would leave the site's own fields different from how they stand now;
     * the main address answers for itself.
     *
     * Each field is compared as the value [applyHeader] would write, built through the same entity mapper so the two
     * cannot drift apart. A script code is a stored entity and is compared by its technical key rather than by
     * reference, because navigating to one can yield a lazy proxy while the parsed value holds the initialised instance.
     */
    private fun willChange(target: SiteDb, header: SiteHeaderUpdate): Boolean {
        header.name.ifSet { if (it != target.name) return true }
        header.confidenceCriteria.ifSet {
            if (siteEntityMapper.toConfidence(it, target.confidenceCriteria.numberOfSharingMembers) != target.confidenceCriteria) return true
        }
        header.states.ifSet {
            if (stateKeys(siteEntityMapper.toStates(it, target)) != stateKeys(target.states)) return true
        }
        header.scriptVariants.ifSet {
            if (scriptVariantKeys(siteEntityMapper.toScriptVariants(it)) != scriptVariantKeys(target.scriptVariants)) return true
        }

        return false
    }

    private fun stateKeys(states: Collection<SiteStateDb>): Set<Any?> =
        states.map { listOf(it.validFrom, it.validTo, it.type) }.toSet()

    private fun scriptVariantKeys(variants: Collection<SiteScriptVariantDb>): Set<Any?> =
        variants.map { it.scriptCode.technicalKey to it.name }.toSet()

    private fun applyHeader(target: SiteDb, header: SiteHeaderUpdate) {
        // The sharing-member count is Pool-maintained, not part of the update payload, so carry the current value forward.
        val numberOfSharingMembers = target.confidenceCriteria.numberOfSharingMembers
        header.name.ifSet { target.name = it }
        header.confidenceCriteria.ifSet { target.confidenceCriteria = siteEntityMapper.toConfidence(it, numberOfSharingMembers) }
        header.states.ifSet { target.states.replace(siteEntityMapper.toStates(it, target)) }
        header.scriptVariants.ifSet { target.scriptVariants.replace(siteEntityMapper.toScriptVariants(it)) }
    }
}
