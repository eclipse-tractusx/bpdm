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

package org.eclipse.tractusx.bpdm.pool.service.operation.relation

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.SiteRelationType
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.SiteRelationDb
import org.eclipse.tractusx.bpdm.pool.model.ChangelogRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteSuccessionParsed
import org.eclipse.tractusx.bpdm.pool.repository.SiteRelationRepository
import org.eclipse.tractusx.bpdm.pool.service.operation.changelog.ChangelogCreateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The single authority for writing a succession between two sites of one legal entity.
 */
@Service
class SiteSuccessionUpsertService(
    private val siteRelationRepository: SiteRelationRepository,
    private val changelogCreateService: ChangelogCreateService
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Persists the succession and reports whether it was created, whether its validity changed, or whether it already
     * stood as stated.
     */
    @Transactional
    fun upsert(parsed: SiteSuccessionParsed): UpsertResult<SiteRelationDb> =
        write(parsed).also { logOutcome(it.upsertType, parsed) }

    private fun write(parsed: SiteSuccessionParsed): UpsertResult<SiteRelationDb> {
        val existingRelation = parsed.existingRelation
            ?: return UpsertResult(create(parsed), UpsertType.Created)

        if (existingRelation.validityPeriods == listOf(parsed.validityPeriod))
            return UpsertResult(existingRelation, UpsertType.NoChange)

        existingRelation.validityPeriods.clear()
        existingRelation.validityPeriods.add(parsed.validityPeriod)
        siteRelationRepository.save(existingRelation)

        return UpsertResult(existingRelation, UpsertType.Updated)
    }

    private fun create(parsed: SiteSuccessionParsed): SiteRelationDb {
        val newRelation = SiteRelationDb(
            type = SiteRelationType.IsReplacedBy,
            startSite = parsed.predecessor,
            endSite = parsed.successor,
            validityPeriods = mutableListOf(parsed.validityPeriod),
            reasonCode = parsed.reasonCode
        )

        siteRelationRepository.save(newRelation)

        changelogCreateService.record(ChangelogRecord(parsed.predecessor.bpn, ChangelogType.UPDATE, BusinessPartnerType.SITE))
        changelogCreateService.record(ChangelogRecord(parsed.successor.bpn, ChangelogType.UPDATE, BusinessPartnerType.SITE))

        return newRelation
    }

    private fun logOutcome(upsertType: UpsertType, parsed: SiteSuccessionParsed) {
        val succession = "site succession from '${parsed.predecessor.bpn}' to '${parsed.successor.bpn}'"
        when (upsertType) {
            UpsertType.Created -> logger.info { "Created $succession" }
            UpsertType.Updated -> logger.info { "Updated validity of $succession" }
            UpsertType.NoChange -> logger.debug { "Left $succession unchanged" }
        }
    }
}
