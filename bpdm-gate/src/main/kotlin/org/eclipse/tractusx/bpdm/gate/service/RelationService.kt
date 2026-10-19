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
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.mapping.types.BpnLString
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.common.util.countForLog
import org.eclipse.tractusx.bpdm.common.util.joinIdentifiersForLog
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.exception.*
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.eclipse.tractusx.bpdm.gate.repository.RelationStageRepository
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RelationService(
    private val relationRepository: RelationRepository,
    private val relationStageRepository: RelationStageRepository,
    private val relationSharingStateService: RelationSharingStateService,
    private val changelogCreateService: ChangelogCreateService
): IRelationService {

    private val logger = KotlinLogging.logger { }

    override fun findInputRelations(
        tenantBpnL: BpnLString,
        externalIds: List<String>,
        relationType: RelationType?,
        sourceBusinessPartnerExternalIds: List<String>,
        targetBusinessPartnerExternalIds: List<String>,
        updatedAtFrom: Instant?,
        paginationRequest: PaginationRequest
    ): PageDto<RelationDto>{

        val querySpecs = Specification.allOf(
            RelationStageRepository.Specs.byTenantBpnL(tenantBpnL.value),
            RelationStageRepository.Specs.byStage(StageType.Input),
            RelationStageRepository.Specs.byExternalIds(externalIds),
            RelationStageRepository.Specs.byRelationshipType(relationType),
            RelationStageRepository.Specs.bySourceExternalIds(sourceBusinessPartnerExternalIds),
            RelationStageRepository.Specs.byTargetExternalIds(targetBusinessPartnerExternalIds),
            RelationStageRepository.Specs.byUpdatedAfter(updatedAtFrom)
        )

        return relationStageRepository.findAll(querySpecs, paginationRequest.toPageRequest())
            .toPageDto { toDto(it) }
    }

    override fun findOutputRelations(
        tenantBpnL: BpnLString,
        externalIds: List<String>,
        relationType: SharableRelationType?,
        sourceBpnLs: List<String>,
        targetBpnLs: List<String>,
        updatedAtFrom: Instant?,
        paginationRequest: PaginationRequest
    ): PageDto<RelationOutputDto> {
        val querySpecs = Specification.allOf(
            RelationRepository.Specs.byOutputIsNotNull(),
            RelationRepository.Specs.byTenantBpnL(tenantBpnL.value),
            RelationRepository.Specs.byExternalIds(externalIds),
            RelationRepository.Specs.byOutputRelationType(relationType),
            RelationRepository.Specs.byOutputSourceBpnLs(sourceBpnLs),
            RelationRepository.Specs.byOutputTargetBpnLs(targetBpnLs),
            RelationRepository.Specs.byOutputUpdatedAfter(updatedAtFrom)
        )

        return relationRepository.findAll(querySpecs, paginationRequest.toPageRequest())
            .toPageDto { toOutputDto(it) }
    }

    @Transactional
    fun deleteRelation(tenantBpnL: BpnLString, externalId: String) {
        val relations = relationRepository.findByTenantBpnLAndExternalId(tenantBpnL.value, externalId) ?: throw BpdmMissingRelationException(externalId)
        val relationStage = relationStageRepository.findByRelationAndStage(relations, stage = StageType.Input) ?: throw BpdmMissingRelationException(externalId)
        relationStageRepository.delete(relationStage)
        relationRepository.delete(relations)
    }


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


    private fun toDto(entity: RelationStageDb): RelationDto{
        return RelationDto(
            externalId = entity.relation.externalId,
            relationType = entity.relationType,
            businessPartnerSourceExternalId = entity.source.externalId,
            businessPartnerTargetExternalId = entity.target.externalId,
            validityPeriods = entity.validityPeriods.map { it.toDto() },
            updatedAt = entity.updatedAt,
            createdAt = entity.createdAt,
            reasonCode = entity.reasonCode
        )
    }

    private fun RelationValidityPeriodDb.toDto(): RelationValidityPeriodDto {
        return RelationValidityPeriodDto(
            validFrom = validFrom,
            validTo = validTo
        )
    }

    private fun toOutputDto(entity: RelationDb): RelationOutputDto{
        return RelationOutputDto(
            externalId = entity.externalId,
            relationType = entity.output!!.relationType,
            sourceBpn = entity.output!!.sourceBpn,
            targetBpn = entity.output!!.targetBpn,
            validityPeriods = entity.output!!.validityPeriods.map { it.toDto() },
            updatedAt = entity.output!!.resultUpdatedAt,
            reasonCode = entity.output!!.reasonCode
        )
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
