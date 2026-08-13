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

package org.eclipse.tractusx.bpdm.pool.service.operation.legalentity

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Keeps a legal entity named only in the scripts its legal address covers.
 *
 * Requests that would break the rule are rejected by the parsers instead. This maintainer exists for the one writer with
 * no request to reject — the headquarter relocation trigger, which moves a legal entity onto an address it never chose.
 * Call it *after* the write it reacts to; it reads the legal address as it now stands.
 */
@Service
class ScriptVariantCoverageService(
    private val legalEntityRepository: LegalEntityRepository
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Removes the script variants of [legalEntity] that its legal address does not cover and reports whether any were.
     */
    @Transactional
    fun pruneUncoveredScriptVariants(legalEntity: LegalEntityDb): Boolean {
        val coveredScriptCodes = legalEntity.legalAddress.scriptCodes().toSet()
        val uncovered = legalEntity.scriptVariants.filterNot { it.scriptCode.technicalKey in coveredScriptCodes }

        if (uncovered.isEmpty()) return false

        legalEntity.scriptVariants.removeAll(uncovered)
        legalEntityRepository.save(legalEntity)
        logger.info {
            "Removed script variants of legal entity '${legalEntity.bpn}' that its legal address does not cover: " +
                    uncovered.joinToString(", ") { it.scriptCode.technicalKey }
        }

        return true
    }
}