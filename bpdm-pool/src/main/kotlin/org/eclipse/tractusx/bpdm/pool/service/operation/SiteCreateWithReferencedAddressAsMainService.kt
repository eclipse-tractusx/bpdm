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
import org.eclipse.tractusx.bpdm.pool.model.AddressSiteAssignment
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateWithReferencedAddressAsMainParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateWithReferencedAddressAsMainRequest
import org.eclipse.tractusx.bpdm.pool.model.SiteHeaderCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.operation.SiteHeaderCreateService
import org.eclipse.tractusx.bpdm.pool.service.parser.SiteHeaderParser
import org.eclipse.tractusx.bpdm.pool.service.parser.AddressBpnParser
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
 * Creates sites whose main address is an *existing* address referenced by BPN (the cleaning/task path that turns an
 * additional address into a site main address), the address-reuse counterpart of [SiteCreateService]. `parse` resolves
 * the legal-entity parent, resolves the referenced address (the re-parent target), and validates the header and main
 * address content — the latter as an update of the referenced address (so it may keep its own identifiers). `create`
 * issues the site BPN and persists the site, re-parents the referenced address onto it as its main address and overwrites
 * its content — so, unlike [SiteCreateService], it builds no new address and issues no address BPN (it does emit an
 * ADDRESS changelog, preserving the previous behavior). Order-preserving positional contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class SiteCreateWithReferencedAddressAsMainService(
    private val siteHeaderParser: SiteHeaderParser,
    private val addressBpnParser: AddressBpnParser,
    private val siteRepository: SiteRepository,
    private val siteHeaderCreateService: SiteHeaderCreateService,
    private val addressSiteAssignmentService: AddressSiteAssignmentService
) {

    fun parse(
        requests: List<SiteCreateWithReferencedAddressAsMainRequest>
    ): List<ParseResult<SiteCreateWithReferencedAddressAsMainParsed, SiteCreateParseError>> {
        val headerResults = siteHeaderParser.parse(requests.map { it.content.header })
        val mainAddressTargetResults = addressBpnParser.parse(requests.map { it.mainAddressBpn })

        return zipParseResults(headerResults, mainAddressTargetResults) { header, mainAddress ->
            SiteCreateWithReferencedAddressAsMainParsed(mainAddress, header)
        }
    }

    @Transactional
    fun create(parsed: List<SiteCreateWithReferencedAddressAsMainParsed>): List<SiteDb> {
        val sites = siteHeaderCreateService.create(parsed.map { SiteHeaderCreateParsed(it.mainAddress.legalEntity!!, it.siteHeader) })
        sites.zip(parsed.map { it.mainAddress }).forEach { (site, address) -> site.mainAddress = address }
        siteRepository.saveAll(sites)

        val mainAddressRequests = parsed.zip(sites) { siteRequest, createdSite -> AddressSiteAssignment(siteRequest.mainAddress, createdSite) }
        addressSiteAssignmentService.assign(mainAddressRequests)

        return sites
    }

    @Transactional
    fun parseAndCreate(
        requests: List<SiteCreateWithReferencedAddressAsMainRequest>
    ): List<ParseResult<SiteDb, SiteCreateParseError>> =
        parseAndExecute(requests, ::parse, ::create)
}