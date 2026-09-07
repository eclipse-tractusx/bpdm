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

package org.eclipse.tractusx.bpdm.pool.service.parser.legalentity

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.UnresolvableLegalEntity
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.springframework.stereotype.Service

/**
 * Resolves legal-entity BPNs to the legal entities they name.
 */
@Service
class LegalEntityBpnParser(
    private val legalEntityRepository: LegalEntityRepository,
) {

    /**
     * Resolves each BPN to its legal entity, failing the entry when no legal entity carries that BPN.
     */
    fun parse(legalEntityBpns: List<String>): List<ParseResult<LegalEntityDb, UnresolvableLegalEntity>> {
        val legalEntitiesByBpn = legalEntityRepository
            .findDistinctByBpnIn(legalEntityBpns.toSet())
            .associateBy { it.bpn }

        return legalEntityBpns.map { bpn ->
            when (val legalEntity = legalEntitiesByBpn[bpn]) {
                null -> ParseResult.ofSingleFailure(UnresolvableLegalEntity(bpn))
                else -> ParseResult.Success(legalEntity)
            }
        }
    }
}