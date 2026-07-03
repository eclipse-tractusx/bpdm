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
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteHeaderCreateParsed
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates sites under an existing legal entity — the single owner of the site-create *operation*. It consumes a
 * [SiteCreateParsed] command (parent resolved, header + main-address content validated by
 * [org.eclipse.tractusx.bpdm.pool.service.parser.SiteCreateParser]) and persists the site and its main address. The main
 * address (whose parent is the still-unsaved site) is delegated to [AddressCreateService]. Order-preserving positional
 * contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class SiteCreateService(
    private val addressCreateService: AddressCreateService,
    private val siteHeaderCreateService: SiteHeaderCreateService,
    private val siteRepository: SiteRepository
) {

    /**
     * Returns the persisted entities (within the caller's transaction) rather than a detached response model: building
     * version-specific responses is the job of the border/application service at the edge.
     */
    @Transactional
    fun create(parsed: List<SiteCreateParsed>): List<SiteDb> {
        val sites = siteHeaderCreateService.create(parsed.map { SiteHeaderCreateParsed(it.legalEntity, it.content.header) })

        val mainAddresses = addressCreateService.create(parsed.zip(sites).map { (entry, site) ->
            val mainAddress = entry.content.mainAddress
            AddressCreateParsed(site.legalEntity, site, mainAddress.address, mainAddress.scriptVariants)
        })

        // The sites are already persistent (saved by siteHeaderCreateService); setting mainAddress mutates the managed
        // entities, so dirty tracking flushes the main_address FK at commit — same as the other site-create paths.
        sites.zip(mainAddresses).forEach { (site, address) -> site.mainAddress = address }

        siteRepository.saveAll(sites)

        return sites
    }
}