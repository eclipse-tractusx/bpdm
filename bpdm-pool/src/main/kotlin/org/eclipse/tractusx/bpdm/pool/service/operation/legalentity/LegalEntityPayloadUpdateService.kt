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

import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.AddressUpdateMapper
import org.eclipse.tractusx.bpdm.pool.mapper.entity.LegalEntityHeaderUpdateMapper
import org.eclipse.tractusx.bpdm.pool.model.update.LegalEntityUpdate
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpdateParsed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Applies a full parsed legal-entity-update payload — header content plus legal address — to an already-resolved legal
 * entity, replacing every field the payload covers, and afterwards restores the ownership invariant the payload may have
 * broken.
 *
 * The ownership recalculation is hooked here rather than in each caller because this is the one door every payload update
 * goes through: both API versions and the golden-record task path.
 */
@Service
class LegalEntityPayloadUpdateService(
    private val legalEntityUpdateService: LegalEntityUpdateService,
    private val ultimateOwnerRecalculationService: UltimateOwnerRecalculationService,
    private val legalEntityHeaderUpdateMapper: LegalEntityHeaderUpdateMapper,
    private val addressUpdateMapper: AddressUpdateMapper
) {

    /**
     * Applies the given payloads in full, re-derives the ultimate owners they may have moved, and reports for each legal
     * entity whether it actually changed.
     */
    @Transactional
    fun update(parsed: List<LegalEntityUpdateParsed>): List<UpsertResult<LegalEntityDb>> {
        val currentness = Instant.now().truncatedTo(ChronoUnit.MICROS)

        // Read before the update mutates the targets: only a changed ownership flag can move an ultimate owner, so only
        // those entities need their subtree re-derived.
        val ownershipFlagChanges = parsed.filter { entry ->
            val requestedFlag = entry.content.header.ownershipUltimate
            requestedFlag != null && requestedFlag != entry.target.ownershipUltimate
        }

        val updateRequests = parsed.map {
            LegalEntityUpdate(
                it.target,
                legalEntityHeaderUpdateMapper.toFullUpdate(it.content.header, currentness),
                addressUpdateMapper.toFullUpdate(it.content.legalAddress)
            )
        }

        val results = legalEntityUpdateService.update(updateRequests)

        // After the write, so the recalculation sees the new flags.
        ultimateOwnerRecalculationService.recalculate(ownershipFlagChanges.map { it.target })

        return results
    }
}
