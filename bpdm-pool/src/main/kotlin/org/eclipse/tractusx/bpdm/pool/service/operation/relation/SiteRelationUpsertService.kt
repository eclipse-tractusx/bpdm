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
import org.eclipse.tractusx.bpdm.common.model.DbTimestamp
import org.eclipse.tractusx.bpdm.pool.api.v7.model.changelog.ChangelogTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteRelationDb
import org.eclipse.tractusx.bpdm.pool.model.ChangelogRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.relation.SiteRelationUpsertParsed
import org.eclipse.tractusx.bpdm.pool.repository.SiteRelationRepository
import org.eclipse.tractusx.bpdm.pool.service.operation.changelog.ChangelogCreateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Writes succession between two sites of one legal entity, in which the source site is replaced by the target site.
 */
@Service
class SiteRelationUpsertService(
    private val siteRelationRepository: SiteRelationRepository,
    private val changelogCreateService: ChangelogCreateService
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Persists [relation] and returns the succession as stored.
     */
    @Transactional
    fun upsert(relation: SiteRelationUpsertParsed): SiteRelationDb {
        val upsertResult = write(relation)

        if (upsertResult.upsertType != UpsertType.NoChange) {
            val updatedAt = DbTimestamp.now()
            relation.source.updatedAt = updatedAt
            relation.target.updatedAt = updatedAt
        }
        logOutcome(upsertResult.upsertType, relation)

        return upsertResult.value
    }

    private fun write(relation: SiteRelationUpsertParsed): UpsertResult<SiteRelationDb> {
        val validityPeriods = relation.validityPeriods.map { RelationValidityPeriodDb(validFrom = it.validFrom, validTo = it.validTo) }
        val existingRelation = relation.existingRelation
            ?: return UpsertResult(create(relation, validityPeriods), UpsertType.Created)

        if (!validityPeriodsDiffer(existingRelation.validityPeriods, validityPeriods)) {
            return UpsertResult(existingRelation, UpsertType.NoChange)
        }

        existingRelation.validityPeriods.clear()
        existingRelation.validityPeriods.addAll(validityPeriods)
        existingRelation.updatedAt = DbTimestamp.now()
        siteRelationRepository.save(existingRelation)

        return UpsertResult(existingRelation, UpsertType.Updated)
    }

    private fun logOutcome(upsertType: UpsertType, relation: SiteRelationUpsertParsed) {
        val succession = "site succession from '${relation.source.bpn}' to '${relation.target.bpn}'"
        when (upsertType) {
            UpsertType.Created -> logger.info { "Created $succession" }
            UpsertType.Updated -> logger.info { "Updated validity periods of $succession" }
            UpsertType.NoChange -> logger.debug { "Left $succession unchanged" }
        }
    }

    private fun create(relation: SiteRelationUpsertParsed, validityPeriods: List<RelationValidityPeriodDb>): SiteRelationDb {
        val newRelation = SiteRelationDb(
            type = SiteRelationTypeV7.IsReplacedBy,
            startSite = relation.source,
            endSite = relation.target,
            validityPeriods = validityPeriods.toMutableList(),
            reasonCode = relation.reasonCode
        )
        siteRelationRepository.save(newRelation)

        changelogCreateService.record(ChangelogRecord(relation.source.bpn, ChangelogTypeV7.UPDATE, BusinessPartnerType.SITE))
        changelogCreateService.record(ChangelogRecord(relation.target.bpn, ChangelogTypeV7.UPDATE, BusinessPartnerType.SITE))

        return newRelation
    }

    private fun validityPeriodsDiffer(existingValidityPeriods: Collection<RelationValidityPeriodDb>, newValidityPeriods: Collection<RelationValidityPeriodDb>): Boolean {
        if (existingValidityPeriods.size != newValidityPeriods.size) return true
        return existingValidityPeriods.zip(newValidityPeriods).any { (existing, new) -> existing.validFrom != new.validFrom || existing.validTo != new.validTo }
    }
}
