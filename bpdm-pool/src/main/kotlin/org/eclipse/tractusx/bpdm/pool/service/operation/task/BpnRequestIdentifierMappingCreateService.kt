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

package org.eclipse.tractusx.bpdm.pool.service.operation.task

import org.eclipse.tractusx.bpdm.pool.entity.BpnRequestIdentifierMappingDb
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Persists the BPNs a golden record task issued for the request identifiers it stated.
 */
@Service
class BpnRequestIdentifierMappingCreateService(
    private val bpnRequestIdentifierRepository: BpnRequestIdentifierRepository
) {

    /**
     * Stores each request identifier with the BPN it now names.
     */
    @Transactional
    fun create(bpnByRequestIdentifier: Map<String, String>) {
        val mappings = bpnByRequestIdentifier.map { BpnRequestIdentifierMappingDb(requestIdentifier = it.key, bpn = it.value) }
        bpnRequestIdentifierRepository.saveAll(mappings)
    }
}
