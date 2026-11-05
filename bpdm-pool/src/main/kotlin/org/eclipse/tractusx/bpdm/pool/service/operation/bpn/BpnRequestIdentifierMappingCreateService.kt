/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.operation.bpn

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.util.countForLog
import org.eclipse.tractusx.bpdm.common.util.joinIdentifiersForLog
import org.eclipse.tractusx.bpdm.pool.entity.BpnRequestIdentifierMappingDb
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.springframework.stereotype.Service

/**
 * Maps BPN request identifiers to the BPNs issued for them.
 */
@Service
class BpnRequestIdentifierMappingCreateService(
    private val bpnRequestIdentifierRepository: BpnRequestIdentifierRepository
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Persists each request identifier of [bpnByRequestIdentifier] as mapped to its BPN.
     */
    fun create(bpnByRequestIdentifier: Map<String, String>) {
        if (bpnByRequestIdentifier.isEmpty()) return

        bpnRequestIdentifierRepository.saveAll(bpnByRequestIdentifier.map { (requestIdentifier, bpn) -> BpnRequestIdentifierMappingDb(requestIdentifier, bpn) })
        logger.info {
            "Mapped ${countForLog(bpnByRequestIdentifier.size, "request identifier", "request identifiers")}: " +
                    bpnByRequestIdentifier.map { (requestIdentifier, bpn) -> "$requestIdentifier -> $bpn" }.joinIdentifiersForLog()
        }
    }
}
