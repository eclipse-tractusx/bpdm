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

package org.eclipse.tractusx.bpdm.gate.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.common.util.countForLog
import org.eclipse.tractusx.bpdm.common.util.joinIdentifiersForLog
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationValidityPeriodDto
import org.eclipse.tractusx.bpdm.gate.api.model.SharableRelationType
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.exception.BpdmInvalidRelationException
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RelationService(
    private val relationRepository: RelationRepository,
    private val relationSharingStateService: RelationSharingStateService,
    private val changelogCreateService: ChangelogCreateService
): IRelationService {

    private val logger = KotlinLogging.logger { }

    @Transactional
    override fun upsertOutputRelations(relations: List<IRelationService.RelationUpsertRequest>): List<RelationDb> {
        val writes = relations.map { upsertOutput(
            it.relation,
            it.relationType,
            it.businessPartnerSourceExternalId,
            it.businessPartnerTargetExternalId,
            it.validityPeriods,
            it.reasonCode
        ) }

        report(writes, StageType.Output)

        return writes.map { it.value }
    }

    private fun report(writes: List<RelationWrite<*>>, stage: StageType) {
        val stageName = when (stage) {
            StageType.Input -> "relation input"
            StageType.Output -> "relation output"
        }

        writes.groupBy { it.change }
            .forEach { (changeType, written) ->
                if (changeType == null) return@forEach

                val verb = when (changeType) {
                    ChangelogType.CREATE -> "Created"
                    ChangelogType.UPDATE -> "Updated"
                }

                logger.info {
                    "$verb ${countForLog(written.size, stageName, "${stageName}s")}: ${written.map { it.identifier }.joinIdentifiersForLog()}"
                }
            }
    }

    private fun upsertOutput(
        relation: RelationDb,
        relationType: SharableRelationType,
        sourceBpnL: String,
        targetBpnL: String,
        validityPeriods: Collection<RelationValidityPeriodDto>,
        reasonCode: String?
    ): RelationWrite<RelationDb>{
        if(sourceBpnL == targetBpnL)
            throw BpdmInvalidRelationException("Source and target should not be the same")

        validateValidityPeriods(validityPeriods)

        val changelogType = if(relation.output == null) ChangelogType.CREATE else ChangelogType.UPDATE

        relation.output = RelationOutputDb(
            relationType = relationType,
            sourceBpn = sourceBpnL,
            targetBpn = targetBpnL,
            resultUpdatedAt = Instant.now(),
            validityPeriods = validityPeriods.map { RelationValidityPeriodDb(validFrom = it.validFrom, validTo = it.validTo) }.toMutableList(),
            reasonCode =  reasonCode
        )
        relationSharingStateService.setSuccess(relation)

        changelogCreateService.record(
            ChangelogEntryDb(relation.externalId, relation.tenantBpnL, changelogType, StageType.Output, GoldenRecordType.Relation)
        )

        return RelationWrite(relationRepository.save(relation), changelogType, "${relation.externalId} ($sourceBpnL -> $targetBpnL)")
    }


    /**
     * The outcome of writing one relation: what was persisted, and no change type where nothing was.
     */
    private data class RelationWrite<T>(
        val value: T,
        val change: ChangelogType?,
        val identifier: String
    )

    private fun validateValidityPeriods(validityPeriods: Collection<RelationValidityPeriodDto>) {
        validityPeriods.forEach { state ->
            val isAfter = if(state.validTo != null) state.validFrom.isAfter(state.validTo) else false

            if (state.validFrom == state.validTo || isAfter) {
                throw BpdmInvalidRelationException(
                    "validFrom '${state.validFrom}' cannot be same or after validTo '${state.validTo}'."
                )
            }
        }
    }
}
