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
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.mapping.types.BpnLString
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.exception.*
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.gate.service.IRelationService.ConstraintErrorType.*
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class RelationService(
    private val relationRepository: RelationRepository,
    private val sharingStateRepository: SharingStateRepository,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val constraintProvider: IRelationConstraintProvider
): IRelationService {

    override fun findRelations(
        tenantBpnL: BpnLString,
        stageType: StageType,
        externalIds: List<String>,
        relationType: RelationType?,
        sourceBusinessPartnerExternalIds: List<String>,
        targetBusinessPartnerExternalIds: List<String>,
        updatedAtFrom: Instant?,
        paginationRequest: PaginationRequest
    ): PageDto<RelationDto>{

        val querySpecs = Specification.allOf(
            RelationRepository.Specs.byTenantBpnL(tenantBpnL.value),
            RelationRepository.Specs.byStage(stageType),
            RelationRepository.Specs.byExternalIds(externalIds),
            RelationRepository.Specs.byRelationshipType(relationType),
            RelationRepository.Specs.bySourceExternalIds(sourceBusinessPartnerExternalIds),
            RelationRepository.Specs.byTargetExternalIds(targetBusinessPartnerExternalIds),
            RelationRepository.Specs.byUpdatedAfter(updatedAtFrom)
        )

        return relationRepository.findAll(querySpecs, paginationRequest.toPageRequest())
            .toPageDto { toDto(it) }
    }

    @Transactional
    fun createRelation(
        tenantBpnL: BpnLString,
        stageType: StageType,
        externalId: String?,
        relationType: RelationType,
        sourceBusinessPartnerExternalId: String,
        targetBusinessPartnerExternalId: String
    ): RelationDto {
        if(externalId != null) relationRepository.findByTenantBpnLAndStageAndExternalId(tenantBpnL.value, stageType, externalId)?.run { throw BpdmRelationAlreadyExistsException(externalId) }

        val source = sharingStateRepository.findByExternalIdAndTenantBpnl(sourceBusinessPartnerExternalId, tenantBpnL.value).singleOrNull()
            ?: throw BpdmRelationSourceNotFoundException(sourceBusinessPartnerExternalId, tenantBpnL.value)
        val target = sharingStateRepository.findByExternalIdAndTenantBpnl(targetBusinessPartnerExternalId, tenantBpnL.value).singleOrNull()
            ?:  throw BpdmRelationTargetNotFoundException(targetBusinessPartnerExternalId, tenantBpnL.value)

        val newRelationship = RelationDb(
            externalId = externalId ?: UUID.randomUUID().toString(),
            relationType = relationType,
            stage = StageType.Input,
            source = source,
            target = target,
            tenantBpnL = tenantBpnL.value,
        )

        assertValid(newRelationship)

        relationRepository.save(newRelationship)

        return toDto(newRelationship)
    }

    @Transactional
    override fun upsertRelation(
        tenantBpnL: BpnLString,
        stageType: StageType,
        externalId: String,
        relationType: RelationType,
        sourceBusinessPartnerExternalId: String,
        targetBusinessPartnerExternalId: String
    ): RelationDto {
        val existingRelationship = relationRepository.findByTenantBpnLAndStageAndExternalId(tenantBpnL.value, stageType, externalId)
        return if(existingRelationship == null){
            createRelation(
                tenantBpnL = tenantBpnL,
                stageType = stageType,
                externalId = externalId,
                relationType = relationType,
                sourceBusinessPartnerExternalId = sourceBusinessPartnerExternalId,
                targetBusinessPartnerExternalId = targetBusinessPartnerExternalId
            )
        }else{
            updateRelationship(
                relationship = existingRelationship,
                relationType = relationType,
                sourceBusinessPartnerExternalId = sourceBusinessPartnerExternalId,
                targetBusinessPartnerExternalId = targetBusinessPartnerExternalId
            )
        }
    }

    @Transactional
    override fun updateRelation(
        tenantBpnL: BpnLString,
        stageType: StageType,
        externalId: String,
        relationType: RelationType,
        sourceBusinessPartnerExternalId: String,
        targetBusinessPartnerExternalId: String
    ): RelationDto {
        val relationshipToUpdate = findRelationshipOrThrow(tenantBpnL = tenantBpnL.value, stageType = stageType, externalId = externalId)
        return updateRelationship(
            relationship = relationshipToUpdate,
            relationType = relationType,
            sourceBusinessPartnerExternalId = sourceBusinessPartnerExternalId,
            targetBusinessPartnerExternalId = targetBusinessPartnerExternalId
        )
    }

    private fun updateRelationship(
        relationship: RelationDb,
        relationType: RelationType,
        sourceBusinessPartnerExternalId: String,
        targetBusinessPartnerExternalId: String
    ): RelationDto {
        val source = sharingStateRepository.findByExternalIdAndTenantBpnl(sourceBusinessPartnerExternalId, relationship.tenantBpnL).singleOrNull() ?: throw BpdmRelationSourceNotFoundException(sourceBusinessPartnerExternalId, relationship.tenantBpnL)
        val target = sharingStateRepository.findByExternalIdAndTenantBpnl(targetBusinessPartnerExternalId, relationship.tenantBpnL).singleOrNull() ?:  throw BpdmRelationTargetNotFoundException(targetBusinessPartnerExternalId, relationship.tenantBpnL)
        RelationPutRequest(
            externalId = relationship.externalId,
            relationType = relationType.also { relationship.relationType = it },
            businessPartnerSourceExternalId = sourceBusinessPartnerExternalId.also { relationship.source = source },
            businessPartnerTargetExternalId = targetBusinessPartnerExternalId.also { relationship.target = target }
        )

        assertValid(relationship)

        relationRepository.save(relationship)

        return toDto(relationship)
    }

    override fun checkConstraints(
        tenantBpnL: BpnLString,
        stageType: StageType,
        businessPartnerExternalIds: List<String>
    ): List<IRelationService.ConstraintError> {
        val sharingStates = sharingStateRepository.findByExternalIdInAndTenantBpnl(businessPartnerExternalIds, tenantBpnL.value).toSet()
        val sources = relationRepository.findBySourceInAndStage(sharingStates, stageType)
        val targets = relationRepository.findByTargetInAndStage(sharingStates, stageType)

        val allRelationships = sources.union(targets)
        return allRelationships.flatMap { check(it) }
    }

    private fun toDto(entity: RelationDb): RelationDto{
        return RelationDto(
            externalId = entity.externalId,
            relationType = entity.relationType,
            businessPartnerSourceExternalId = entity.source.externalId,
            businessPartnerTargetExternalId = entity.target.externalId,
            updatedAt = entity.updatedAt,
            createdAt = entity.createdAt
        )
    }

    private fun assertValid(relationship: RelationDb){
        check(relationship).let { constraintErrors ->
            if (constraintErrors.isNotEmpty())
                throw BpdmInvalidRelationConstraintsException.fromConstraintErrors(constraintErrors)
        }
    }

    private fun check(relationship: RelationDb): List<IRelationService.ConstraintError> {
        val validationErrors = mutableListOf <IRelationService.ConstraintError>()
        val externalId = relationship.externalId

        val constraints = constraintProvider.getConstraints(relationship.relationType)

        if(!constraints.selfRelateAllowed){
            if(relationship.source == relationship.target) validationErrors.add(SourceEqualsTarget.toError(externalId, relationship.source.externalId))
        }

        val sourceInput = businessPartnerRepository.findBySharingStateInAndStage(listOf(relationship.source), StageType.Input).single()
        val targetInput = businessPartnerRepository.findBySharingStateInAndStage(listOf(relationship.target), StageType.Input).single()


        if(!constraints.sourceCanBeTarget)
        {
            val sourceUsedAsTarget = relationRepository.findByRelationTypeAndTarget(relationship.relationType, relationship.source)
                .minusElement(relationship).isNotEmpty()
            if(sourceUsedAsTarget) validationErrors.add(SourceUsedAsTarget.toError(externalId, relationship.source.externalId))

            val targetUsedAsSource = relationRepository.findByRelationTypeAndSource(relationship.relationType, relationship.target)
                .minusElement(relationship).isNotEmpty()
            if(targetUsedAsSource) validationErrors.add(TargetUsedAsSource.toError(externalId, relationship.target.externalId))
        }

        if(!constraints.multipleSourcesAllowed){
            val targetHasMultipleSources = relationRepository.findByRelationTypeAndTarget(relationship.relationType, relationship.target)
                .minusElement(relationship).isNotEmpty()
            if(targetHasMultipleSources) validationErrors.add(TargetMultipleSources.toError(externalId, relationship.target.externalId))
        }

        if(!constraints.multipleTargetsAllowed){
            val sourceHasMultipleTargets = relationRepository.findByRelationTypeAndSource(relationship.relationType, relationship.source)
                .minusElement(relationship).isNotEmpty()
            if(sourceHasMultipleTargets) validationErrors.add(SourceMultipleTargets.toError(externalId, relationship.source.externalId))
        }

        if(constraints.sourceMustBeOwnCompanyData){
            if(!sourceInput.isOwnCompanyData) validationErrors.add(SourceNotOwnCompanyData.toError(externalId, relationship.source.externalId))
        }

        if(constraints.targetMustBeOwnCompanyData){
            if(!targetInput.isOwnCompanyData) validationErrors.add(TargetNotOwnCompanyData.toError(externalId, relationship.target.externalId))
        }

        val sourceBusinessPartnerTypes =  sourceInput.postalAddress.addressType?.businessPartnerTypes ?: listOf(BusinessPartnerType.GENERIC)
        if(sourceBusinessPartnerTypes.intersect(constraints.sourceAllowedTypes).isEmpty())
            validationErrors.add(SourceNotAllowedType.toError(externalId, relationship.source.externalId))

        val targetBusinessPartnerTypes =  targetInput.postalAddress.addressType?.businessPartnerTypes ?: listOf(BusinessPartnerType.GENERIC)
        if(targetBusinessPartnerTypes.intersect(constraints.targetAllowedTypes).isEmpty())
            validationErrors.add(TargetNotAllowedType.toError(externalId, relationship.target.externalId))

        return validationErrors
    }

    private fun findRelationshipOrThrow(
        tenantBpnL: String,
        stageType: StageType,
        externalId: String
    ): RelationDb{
        return relationRepository.findByTenantBpnLAndStageAndExternalId(
            tenantBpnL = tenantBpnL,
            stageType = stageType,
            externalId = externalId
        ) ?:  throw BpdmMissingRelationException(externalId)
    }

    fun IRelationService.ConstraintErrorType.toError(externalId: String, erroneousValue: String) =
        IRelationService.ConstraintError(externalId, this, erroneousValue)
}