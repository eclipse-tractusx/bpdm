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

import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.springframework.stereotype.Service

/**
 * Loads the associations an address response is built from, one query per association instead of one per address.
 */
@Service
class AddressAssociationFetchService(
    private val logisticAddressRepository: LogisticAddressRepository
) {

    /**
     * Loads the legal entity, sites, regions, identifiers and states of the given addresses into the persistence
     * context.
     */
    fun fetch(addresses: Set<LogisticAddressDb>) {
        if (addresses.isEmpty()) return

        logisticAddressRepository.joinLegalEntities(addresses)
        logisticAddressRepository.joinSites(addresses)
        logisticAddressRepository.joinRegions(addresses)
        logisticAddressRepository.joinIdentifiers(addresses)
        logisticAddressRepository.joinStates(addresses)
    }
}
