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

import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateWithReferencedAddressAsMainParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteHeaderCreateParsed
import org.eclipse.tractusx.bpdm.pool.service.writer.LogisticAddressWriter
import org.eclipse.tractusx.bpdm.pool.service.writer.SiteWriter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates sites whose main address is an *existing* address referenced by BPN (the cleaning/task path that turns an
 * additional address into a site main address) — the address-reuse counterpart of [SiteCreateService]. It consumes a
 * [SiteCreateWithReferencedAddressAsMainParsed] command (referenced address resolved, header validated by
 * [org.eclipse.tractusx.bpdm.pool.service.parser.SiteCreateWithReferencedAddressAsMainParser]), issues the site BPN and
 * persists the site, then re-parents the referenced address onto it as its main address — so, unlike [SiteCreateService],
 * it builds no new address and issues no address BPN (it does emit an ADDRESS changelog, preserving the previous
 * behavior). Order-preserving positional contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class SiteCreateWithReferencedAddressAsMainService(
    private val siteWriter: SiteWriter,
    private val addressWriter: LogisticAddressWriter
) {

    @Transactional
    fun create(parsed: List<SiteCreateWithReferencedAddressAsMainParsed>): List<SiteDb> {

        val stagedSites = siteWriter.stageCreate(parsed.map { SiteHeaderCreateParsed(it.mainAddress.legalEntity!!, it.siteHeader) })

        //Wire sites to site main address
        val stagedAddressUpdates = parsed.zip(stagedSites).map { (entry, stagedSite) -> addressWriter.stageUpdate(entry.mainAddress) { it.sites.add(stagedSite.site) } }
        stagedSites.zip(stagedAddressUpdates).forEach { (stagedSite, stagedAddressUpdate) -> stagedSite.site.mainAddress = stagedAddressUpdate.address }

        val createdSites = siteWriter.commit(stagedSites).map { it.value }
        addressWriter.commit(stagedAddressUpdates)

        return createdSites
    }
}
