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

import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.UnresolvableSite
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.stereotype.Service

/**
 * Resolves site BPNs to the sites they name.
 */
@Service
class SiteBpnParser(
    private val siteRepository: SiteRepository,
) {

    /**
     * Resolves each BPN to its site and a null BPN to a legitimately absent site, failing the entry when no site carries
     * a given BPN. Owning the optionality here keeps callers free of null special-casing.
     */
    fun parse(siteBpns: List<String?>): List<ParseResult<SiteDb?, UnresolvableSite>> {
        val sitesByBpn = resolve(siteBpns.filterNotNull().toSet())
        return siteBpns.map { bpn ->
            when (bpn) {
                null -> ParseResult.Success(null)
                else -> resolveResult(bpn, sitesByBpn)
            }
        }
    }

    /**
     * Resolves each BPN to its site, failing the entry when no site carries that BPN.
     */
    fun parseRequired(siteBpns: List<String>): List<ParseResult<SiteDb, UnresolvableSite>> {
        val sitesByBpn = resolve(siteBpns.toSet())
        return siteBpns.map { bpn -> resolveResult(bpn, sitesByBpn) }
    }

    private fun resolve(bpns: Set<String>): Map<String, SiteDb> =
        siteRepository.findDistinctByBpnIn(bpns).associateBy { it.bpn }

    private fun resolveResult(bpn: String, sitesByBpn: Map<String, SiteDb>): ParseResult<SiteDb, UnresolvableSite> =
        when (val site = sitesByBpn[bpn]) {
            null -> ParseResult.ofSingleFailure(UnresolvableSite(bpn))
            else -> ParseResult.Success(site)
        }
}