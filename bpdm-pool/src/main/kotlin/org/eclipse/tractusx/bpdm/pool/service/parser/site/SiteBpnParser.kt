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

package org.eclipse.tractusx.bpdm.pool.service.parser.site

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.error.UnresolvableSite
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.stereotype.Service

/**
 * Resolves site BPNs to the sites they name, reading a BPN case-insensitively.
 */
@Service
class SiteBpnParser(
    private val siteRepository: SiteRepository,
) {

    /**
     * Resolves each BPN to its site, failing the entry when no site carries that BPN.
     */
    fun parse(siteBpns: List<String>): List<ParseResult<SiteDb, UnresolvableSite>> {
        val sitesByBpn = resolve(siteBpns.toSet())
        return siteBpns.map { bpn -> resolveResult(bpn, sitesByBpn) }
    }

    /**
     * Resolves each entry's BPNs to their sites, failing an entry with one error per BPN no site carries.
     */
    fun parseAll(siteBpnsPerEntry: List<List<String>>): List<ParseResult<List<SiteDb>, UnresolvableSite>> {
        val sitesByBpn = resolve(siteBpnsPerEntry.flatten().toSet())
        return siteBpnsPerEntry.map { resolveAllResult(it, sitesByBpn) }
    }

    private fun resolve(bpns: Set<String>): Map<String, SiteDb> =
        siteRepository.findDistinctByBpnIn(bpns.mapTo(mutableSetOf()) { it.uppercase() }).associateBy { it.bpn }

    private fun resolveAllResult(bpns: List<String>, sitesByBpn: Map<String, SiteDb>): ParseResult<List<SiteDb>, UnresolvableSite> {
        val unresolvable = bpns.filterNot { sitesByBpn.containsKey(it.uppercase()) }.map { UnresolvableSite(it) }
        return when {
            unresolvable.isNotEmpty() -> ParseResult.Failure(unresolvable)
            else -> ParseResult.Success(bpns.map { sitesByBpn.getValue(it.uppercase()) })
        }
    }

    private fun resolveResult(bpn: String, sitesByBpn: Map<String, SiteDb>): ParseResult<SiteDb, UnresolvableSite> =
        when (val site = sitesByBpn[bpn.uppercase()]) {
            null -> ParseResult.ofSingleFailure(UnresolvableSite(bpn))
            else -> ParseResult.Success(site)
        }
}