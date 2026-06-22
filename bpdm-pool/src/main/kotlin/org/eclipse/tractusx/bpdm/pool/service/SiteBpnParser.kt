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

import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.UnresolvableSite
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.stereotype.Service

/**
 * Resolves optional site BPNs to entities, batched and order-preserving (see [ParseResult]): a `null` BPN means "no
 * site parent" and yields `Success(null)`, while a non-null but unresolvable BPN yields an [UnresolvableSite]. Owning
 * the optionality here keeps callers free of the null special-casing when combining parsers via `zipParseResults`.
 */
@Service
class SiteBpnParser(
    private val siteRepository: SiteRepository,
) {

    fun parse(siteBpns: List<String?>): List<ParseResult<SiteDb?, UnresolvableSite>> {
        val sitesByBpn = siteRepository
            .findDistinctByBpnIn(siteBpns.filterNotNull().toSet())
            .associateBy { it.bpn }

        return siteBpns.map { bpn ->
            when {
                bpn == null -> ParseResult.Success(null)
                else -> when (val site = sitesByBpn[bpn]) {
                    null -> ParseResult.ofSingleFailure(UnresolvableSite(bpn))
                    else -> ParseResult.Success(site)
                }
            }
        }
    }
}
