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

import org.eclipse.tractusx.bpdm.pool.entity.BpnRequestIdentifierMappingDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.BpnRequestIdentifierSearchParsed
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Queries the BPNs that were issued for given request identifiers.
 */
@Service
class BpnRequestIdentifierSearchService(
    private val bpnRequestIdentifierRepository: BpnRequestIdentifierRepository
) {

    /**
     * Returns the mapping of every requested identifier that a BPN was issued for, omitting the request identifiers
     * without one.
     */
    @Transactional(readOnly = true)
    fun search(criteria: BpnRequestIdentifierSearchParsed): Set<BpnRequestIdentifierMappingDb> =
        if (criteria.requestedIdentifiers.isEmpty()) emptySet()
        else bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(criteria.requestedIdentifiers)
}
