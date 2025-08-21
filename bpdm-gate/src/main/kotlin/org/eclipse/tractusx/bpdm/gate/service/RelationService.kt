/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.mapping.types.BpnLString
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.exception.*
import org.eclipse.tractusx.bpdm.gate.model.RelationDefaults
import org.eclipse.tractusx.bpdm.gate.model.RelationDefaults.DEFAULT_STATE
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.eclipse.tractusx.bpdm.gate.repository.RelationStageRepository
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class RelationService(
    private val relationRepository: RelationRepository,
    private val relationStageRepository: RelationStageRepository,
    private val sharingStateRepository: SharingStateRepository,
    private val relationSharingStateService: RelationSharingStateService,
    private val changelogRepository: ChangelogRepository
): IRelationService {

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
    fun createInputRelations(
        tenantBpnL: BpnLString,
        externalId: String?,
        relationType: RelationType,
        sourceBusinessPartnerExternalId: String,
        targetBusinessPartnerExternalId: String
    ): RelationDto {
        if (externalId != null) {
            val existingRelationship = relationRepository.findByTenantBpnLAndExternalId(tenantBpnL.value, externalId)
            if (existingRelationship != null) throw BpdmRelationAlreadyExistsException(externalId)
        }
        return toDto(createInputStage(tenantBpnL, externalId, relationType, sourceBusinessPartnerExternalId, targetBusinessPartnerExternalId, mutableListOf()))
    }

    @Transactional
    fun deleteRelation(tenantBpnL: BpnLString, externalId: String) {
        val relations = relationRepository.findByTenantBpnLAndExternalId(tenantBpnL.value, externalId) ?: throw BpdmMissingRelationException(externalId)
        val relationStage = relationStageRepository.findByRelationAndStage(relations, stage = StageType.Input) ?: throw BpdmMissingRelationException(externalId)
        relationStageRepository.delete(relationStage)
        relationRepository.delete(relations)
    }

    @Transactional
    override fun upsertInputRelations(tenantBpnL: BpnLString, relations: List<RelationPutEntry>): List<RelationDto> {
        return relations.map {
                with(it){
                    upsertInputStage(
                        tenantBpnL = tenantBpnL,
                        externalId = externalId,
                        relationType = relationType,
                        sourceBusinessPartnerExternalId = businessPartnerSourceExternalId,
                        targetBusinessPartnerExternalId = businessPartnerTargetExternalId,
                        states = states
                    )
                }
            }
    }

    @Transactional
    override fun updateInputRelations(tenantBpnL: BpnLString, relations: List<RelationPutEntry>): List<RelationDto> {
        return relations.map {
                with(it){
                    updateInputStage(
                        tenantBpnL = tenantBpnL,
                        externalId = externalId,
                        relationType = relationType,
                        sourceBusinessPartnerExternalId = businessPartnerSourceExternalId,
                        targetBusinessPartnerExternalId = businessPartnerTargetExternalId,
                        states = states
                    )
                }
            }.map(::toDto)
    }

    @Transactional
    override fun upsertOutputRelations(relations: List<IRelationService.RelationUpsertRequest>): List<RelationDb> {
        return relations.map { upsertOutput(it.relation, it.relationType, it.businessPartnerSourceExternalId, it.businessPartnerTargetExternalId, it.states) }
    }

    private fun upsertInputStage(
        tenantBpnL: BpnLString,
        externalId: String,
        relationType: RelationType,
        sourceBusinessPartnerExternalId: String,
        targetBusinessPartnerExternalId: String,
        states: List<RelationStateDto>
    ): RelationDto {
        val existingRelationship = relationRepository.findByTenantBpnLAndExternalId(tenantBpnL.value, externalId)
        val upsertedRelationStage = if(existingRelationship == null)
            createInputStage(tenantBpnL, externalId, relationType, sourceBusinessPartnerExternalId, targetBusinessPartnerExternalId, states)
        else
            updateInputStage(existingRelationship, relationType, sourceBusinessPartnerExternalId, targetBusinessPartnerExternalId, states)

        return toDto(upsertedRelationStage)
    }

    private fun upsertOutput(
        relation: RelationDb,
        relationType: SharableRelationType,
        sourceBpnL: String,
        targetBpnL: String,
        states: Collection<RelationStateDto>
    ): RelationDb{
        if(sourceBpnL == targetBpnL)
            throw BpdmInvalidRelationException("Source and target should not be the same")

        validateStates(states)

        val changelogType = if(relation.output == null) ChangelogType.CREATE else ChangelogType.UPDATE

        val relationOutputStates = states.map {
            RelationStateDb(
                validFrom = it.validFrom,
                validTo = it.validTo,
                type = it.type
            )
        }.toMutableList()

        relation.output = RelationOutputDb(
            relationType = relationType,
            sourceBpnL = sourceBpnL,
            targetBpnL = targetBpnL,
            updatedAt = Instant.now(),
            states = relationOutputStates
        )
        relationSharingStateService.setSuccess(relation)

        changelogRepository.save(ChangelogEntryDb(relation.externalId, relation.tenantBpnL, changelogType, StageType.Output, GoldenRecordType.Relation))
        return relationRepository.save(relation)
    }

    private fun createInputStage(
        tenantBpnL: BpnLString,
        externalId: String?,
        relationType: RelationType,
        sourceBusinessPartnerExternalId: String,
        targetBusinessPartnerExternalId: String,
        states: List<RelationStateDto>
    ): RelationStageDb{
        if(sourceBusinessPartnerExternalId == targetBusinessPartnerExternalId)
            throw BpdmInvalidRelationException("Source and target '$sourceBusinessPartnerExternalId' should not be equal.")

        val safeStates = ensureDefaultStateIfEmpty(states)
        validateStates(safeStates)

        val relation = RelationDb(
            externalId = externalId ?: UUID.randomUUID().toString(),
            tenantBpnL = tenantBpnL.value,
            sharingState = null,
            output = null
        )
        relationSharingStateService.setInitial(relation, relationType)

        val source = sharingStateRepository.findByExternalIdAndTenantBpnl(sourceBusinessPartnerExternalId, relation.tenantBpnL).singleOrNull()
            ?: throw BpdmRelationSourceNotFoundException(sourceBusinessPartnerExternalId, relation.tenantBpnL)
        val target = sharingStateRepository.findByExternalIdAndTenantBpnl(targetBusinessPartnerExternalId, relation.tenantBpnL).singleOrNull()
            ?:  throw BpdmRelationTargetNotFoundException(targetBusinessPartnerExternalId, relation.tenantBpnL)

        val relationStates = safeStates.map {
            RelationStateDb(
                validFrom = it.validFrom,
                validTo = it.validTo,
                type = it.type
            )
        }.toMutableList()

        val relationStage = RelationStageDb(
            relation = relation,
            relationType = relationType,
            stage = StageType.Input,
            source = source,
            target = target,
            states = relationStates
        )

        relationStageRepository.save(relationStage)
        changelogRepository.save(ChangelogEntryDb(relation.externalId, tenantBpnL.value, ChangelogType.CREATE, StageType.Input, GoldenRecordType.Relation))

        return relationStage
    }

    private fun updateInputStage(
        tenantBpnL: BpnLString,
        externalId: String,
        relationType: RelationType,
        sourceBusinessPartnerExternalId: String,
        targetBusinessPartnerExternalId: String,
        states: List<RelationStateDto>
    ): RelationStageDb{
        val existingRelationship = relationRepository.findByTenantBpnLAndExternalId(tenantBpnL.value, externalId) ?: throw BpdmMissingRelationException(externalId)
        return updateInputStage(existingRelationship, relationType, sourceBusinessPartnerExternalId, targetBusinessPartnerExternalId, states)
    }

    private fun updateInputStage(
        relation: RelationDb,
        relationType: RelationType,
        sourceBusinessPartnerExternalId: String,
        targetBusinessPartnerExternalId: String,
        states: List<RelationStateDto>
    ): RelationStageDb{
        if(sourceBusinessPartnerExternalId == targetBusinessPartnerExternalId)
            throw BpdmInvalidRelationException("Source and target '$sourceBusinessPartnerExternalId' should not be equal.")

        val safeStates = ensureDefaultStateIfEmpty(states)
        validateStates(safeStates)

        val existingStage = relationStageRepository.findByRelationAndStage(relation, StageType.Input) ?: throw BpdmMissingRelationException(relation.externalId)

        val source = sharingStateRepository.findByExternalIdAndTenantBpnl(sourceBusinessPartnerExternalId, relation.tenantBpnL).singleOrNull()
            ?: throw BpdmRelationSourceNotFoundException(sourceBusinessPartnerExternalId, relation.tenantBpnL)
        val target = sharingStateRepository.findByExternalIdAndTenantBpnl(targetBusinessPartnerExternalId, relation.tenantBpnL).singleOrNull()
            ?:  throw BpdmRelationTargetNotFoundException(targetBusinessPartnerExternalId, relation.tenantBpnL)

        val proposedStates = safeStates.map {
            RelationStateDb(
                validFrom = it.validFrom,
                validTo = it.validTo,
                type = it.type
            )
        }.toMutableList()

        val newValues = RelationUpdateComparison(relationType, source, target, proposedStates)
        val oldValues = RelationUpdateComparison(existingStage.relationType, existingStage.source, existingStage.target, existingStage.states)
        val hasChanges = newValues != oldValues
        val isInErrorState = relation.sharingState?.sharingStateType == RelationSharingStateType.Error

        if(hasChanges){
            existingStage.relationType = relationType
            existingStage.source = source
            existingStage.target = target
            existingStage.states = proposedStates
            existingStage.updatedAt = Instant.now()

            relationStageRepository.save(existingStage)
            changelogRepository.save(ChangelogEntryDb(relation.externalId, relation.tenantBpnL, ChangelogType.UPDATE, StageType.Input, GoldenRecordType.Relation))
        }

        if(hasChanges || isInErrorState){
            relationSharingStateService.setInitial(relation, relationType)
        }

        return existingStage
    }


    private fun toDto(entity: RelationStageDb): RelationDto{
        return RelationDto(
            externalId = entity.relation.externalId,
            relationType = entity.relationType,
            businessPartnerSourceExternalId = entity.source.externalId,
            businessPartnerTargetExternalId = entity.target.externalId,
            states = entity.states.map { it.toDto() },
            updatedAt = entity.updatedAt,
            createdAt = entity.createdAt
        )
    }

    private fun RelationStateDb.toDto(): RelationStateDto {
        return RelationStateDto(
            validFrom = validFrom,
            validTo = validTo,
            type = type,
        )
    }

    private fun toOutputDto(entity: RelationDb): RelationOutputDto{
        return RelationOutputDto(
            externalId = entity.externalId,
            relationType = entity.output!!.relationType,
            sourceBpnL = entity.output!!.sourceBpnL,
            targetBpnL = entity.output!!.targetBpnL,
            states = entity.output!!.states.map { it.toDto() },
            updatedAt = entity.output!!.updatedAt
        )
    }

    data class RelationUpdateComparison(
        val relationType: RelationType,
        val source: SharingStateDb,
        val target: SharingStateDb,
        val states: MutableList<RelationStateDb>
    )

    private fun ensureDefaultStateIfEmpty(states: List<RelationStateDto>): List<RelationStateDto> {
        return states.ifEmpty {
            listOf(RelationStateDto(
                validFrom = RelationDefaults.VALID_FROM_DEFAULT,
                validTo = RelationDefaults.VALID_TO_DEFAULT,
                type = BusinessStateType.ACTIVE
            ))
        }
    }

    private fun validateStates(states: Collection<RelationStateDto>) {
        // Only one state allowed
        if (states.size > 1) {
            throw BpdmInvalidRelationException("Only one relation state is allowed, found ${states.size}.")
        }

        states.first().let { state ->
            if (state.validFrom.isAfter(state.validTo)) {
                throw BpdmInvalidRelationException(
                    "validFrom '${state.validFrom}' cannot be after validTo '${state.validTo}'."
                )
            }
            if (state.type !in listOf(BusinessStateType.ACTIVE, BusinessStateType.INACTIVE)) {
                throw BpdmInvalidRelationException(
                    "Relation state type must be ACTIVE or INACTIVE, found '${state.type}'."
                )
            }
        }
    }
}