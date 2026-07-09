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

import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.SiteEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteContentParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteUpdateParsed
import org.eclipse.tractusx.bpdm.pool.service.writer.SiteWriter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Updates sites — the composite site-update *operation*. It consumes a [SiteUpdateParsed] command (target resolved,
 * header + main-address content validated by [org.eclipse.tractusx.bpdm.pool.service.parser.SiteUpdateParser]), applies
 * the header change (delegating change detection, save and SITE changelog to [org.eclipse.tractusx.bpdm.pool.service.writer.SiteWriter]), and delegates the
 * main-address change to [AddressUpdateService] (with no site assignment), netting a single UPDATE when either side
 * changed. Order-preserving positional contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class SiteUpdateService(
    private val addressUpdateService: AddressUpdateService,
    private val siteWriter: SiteWriter,
    private val siteEntityMapper: SiteEntityMapper
) {

    /**
     * Returns the updated entities (within the caller's transaction) rather than a detached response model: building
     * version-specific responses is the job of the border/application service at the edge.
     */
    @Transactional
    fun update(parsed: List<SiteUpdateParsed>): List<UpsertResult<SiteDb>>{
        // Stage + commit the site headers first so all SITE changelogs precede the main-address ADDRESS changelogs below.
        val siteResults = siteWriter.commit(parsed.map { entry -> siteWriter.stageUpdate(entry.target) { doUpdateEntity(it, entry.content) } })

        val mainAddressRequests = parsed.map {
            AddressUpdateParsed(
                it.target.mainAddress,
                null,
                it.content.mainAddress.address,
                it.content.mainAddress.scriptVariants
            )
        }
        val mainAddressResults = addressUpdateService.update(mainAddressRequests)

        return siteResults.zip(mainAddressResults){ siteResult, mainAddressResult ->
            val changed = siteResult.upsertType == UpsertType.Updated || mainAddressResult.upsertType == UpsertType.Updated
            UpsertResult(siteResult.value, if (changed) UpsertType.Updated else UpsertType.NoChange)
        }
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