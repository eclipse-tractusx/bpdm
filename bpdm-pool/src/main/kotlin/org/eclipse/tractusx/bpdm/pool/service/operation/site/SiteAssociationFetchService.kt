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

package org.eclipse.tractusx.bpdm.pool.service.operation.site

import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressAssociationFetchService
import org.springframework.stereotype.Service

/**
 * Loads the associations a site response is built from, one query per association instead of one per site.
 */
@Service
class SiteAssociationFetchService(
    private val siteRepository: SiteRepository,
    private val addressAssociationFetchService: AddressAssociationFetchService
) {

    /**
     * Loads the addresses, states and relations of the given sites into the persistence context, along with everything
     * those addresses are rendered from.
     */
    fun fetch(sites: Set<SiteDb>) {
        if (sites.isEmpty()) return

        siteRepository.joinAddresses(sites)
        siteRepository.joinStates(sites)
        siteRepository.joinRelations(sites)
        addressAssociationFetchService.fetch(sites.flatMap { it.addresses }.toSet())
    }
}
