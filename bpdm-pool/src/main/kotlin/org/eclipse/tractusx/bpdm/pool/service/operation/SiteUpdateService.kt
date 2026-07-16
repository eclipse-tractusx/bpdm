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
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteContentParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpdateParsed
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerEquivalenceMapper
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The single authority for updating sites together with their main address: applies and change-detects the header,
 * applies the main-address change, and reports one UPDATE when either side actually changed. The parent SITE changelog
 * is emitted before the child ADDRESS changelog. Returns managed entities in the caller's transaction, not response
 * models.
 */
@Service
class SiteUpdateService(
    private val addressFullUpdateService: AddressFullUpdateService,
    private val siteRepository: SiteRepository,
    private val changelogService: PartnerChangelogService,
    private val siteEntityMapper: SiteEntityMapper,
    private val equivalenceMapper: BusinessPartnerEquivalenceMapper
) {

    @Transactional
    fun update(parsed: List<SiteUpdateParsed>): List<UpsertResult<SiteDb>>{
        val headerResults = parsed.map { updateHeader(it) }

        val mainAddressRequests = parsed.map {
            AddressUpdateParsed(
                it.target.mainAddress,
                null,
                it.content.mainAddress
            )
        }
        val stagedMainAddresses = addressFullUpdateService.stageUpdate(mainAddressRequests)

        val overallResults = headerResults.zip(stagedMainAddresses){ headerResult, stagedMainAddress ->
            val changed = headerResult.upsertType == UpsertType.Updated || stagedMainAddress.upsertType == UpsertType.Updated
            UpsertResult(headerResult.value, if (changed) UpsertType.Updated else UpsertType.NoChange)
        }

        // Emit the parent changelog before committing the staged address so the parent UPDATE precedes the child.
        changelogService.createChangelogEntries(
            overallResults
                .filter { it.upsertType == UpsertType.Updated }
                .map { ChangelogEntryCreateRequest(it.value.bpn, ChangelogType.UPDATE, BusinessPartnerType.SITE) }
        )
        siteRepository.saveAll(headerResults.filter { it.upsertType == UpsertType.Updated }.map { it.value })

        addressFullUpdateService.commit(stagedMainAddresses)

        return overallResults
    }

    private fun updateHeader(request: SiteUpdateParsed): UpsertResult<SiteDb>{
        val before = equivalenceMapper.toEquivalenceDto(request.target)
        doUpdateEntity(request.target, request.content)
        val after = equivalenceMapper.toEquivalenceDto(request.target)

        return UpsertResult(request.target, if (before != after) UpsertType.Updated else UpsertType.NoChange)
    }

    private fun doUpdateEntity(target: SiteDb, content: SiteContentParsed) {
        val header = content.header
        target.name = header.name
        // Sharing-member count is Pool-maintained and absent from the payload, so carry it forward.
        target.confidenceCriteria = siteEntityMapper.toConfidence(header.confidenceCriteria, target.confidenceCriteria.numberOfSharingMembers)
        target.states.replace(siteEntityMapper.toStates(header.states, target))
        target.scriptVariants.replace(siteEntityMapper.toScriptVariants(header.scriptVariants))
    }
}