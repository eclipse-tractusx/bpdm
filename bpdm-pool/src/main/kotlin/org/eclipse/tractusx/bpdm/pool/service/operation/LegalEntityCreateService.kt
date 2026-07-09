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

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityCreateParsed
import org.eclipse.tractusx.bpdm.pool.service.writer.LegalEntityWriter
import org.eclipse.tractusx.bpdm.pool.service.writer.LogisticAddressWriter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates legal entities, the top of the business-partner hierarchy — the single owner of the legal-entity-create
 * *operation*. It consumes a [LegalEntityCreateParsed] command (header + legal-address content already validated by
 * [org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntityCreateParser]) and persists the legal entity and its legal
 * address. Both the legal entity ([org.eclipse.tractusx.bpdm.pool.service.writer.LegalEntityWriter]) and its legal address ([org.eclipse.tractusx.bpdm.pool.service.writer.LogisticAddressWriter]) are staged unsaved
 * so the legal entity ⇄ legal address cycle can be wired in memory before persisting. Order-preserving positional
 * contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class LegalEntityCreateService(
    private val addressWriter: LogisticAddressWriter,
    private val legalEntityWriter: LegalEntityWriter
) {

    /**
     * Returns the persisted entities (within the caller's transaction) rather than a detached response model: building
     * version-specific responses is the job of the border/application service at the edge.
     */
    @Transactional
    fun create(parsed: List<LegalEntityCreateParsed>): List<LegalEntityDb> {
        val stagedLegalEntities = legalEntityWriter.stageCreate(parsed.map { it.content.header })
        val stagedAddresses = addressWriter.stageCreate(parsed.zip(stagedLegalEntities).map { (entry, staged) ->
            val legalAddress = entry.content.legalAddress
            AddressCreateParsed(staged.legalEntity, site = null, legalAddress.address, legalAddress.scriptVariants)
        })

        stagedLegalEntities.zip(stagedAddresses).forEach { (staged, stagedAddress) -> staged.legalEntity.legalAddress = stagedAddress.address }

        val legalEntities = legalEntityWriter.commit(stagedLegalEntities).map { it.value }
        addressWriter.commit(stagedAddresses)

        return legalEntities
    }
}
