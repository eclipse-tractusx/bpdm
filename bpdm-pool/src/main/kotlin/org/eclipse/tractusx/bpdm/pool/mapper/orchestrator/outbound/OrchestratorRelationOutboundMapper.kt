/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound

import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.RelationValidityPeriodRecord
import org.eclipse.tractusx.bpdm.pool.model.output.task.GoldenRecordRelationTaskResultRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.RelationSubject
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.RelationUpsertError
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationType
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.RelationValidityPeriod
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsErrorDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsErrorType
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.springframework.stereotype.Component
import org.eclipse.tractusx.orchestrator.api.model.RelationType as OrchestratorRelationType

/**
 * Translates the outcomes of golden record relation tasks into the results the Orchestrator expects from the Pool.
 */
@Component
class OrchestratorRelationOutboundMapper {

    /**
     * Returns the task step result that reports [result] to the Orchestrator.
     */
    fun toResultEntry(result: GoldenRecordRelationTaskResultRecord): TaskRelationsStepResultEntryDto =
        when (result) {
            is GoldenRecordRelationTaskResultRecord.LegalEntityRelationResolved -> with(result.relation) {
                toResolvedEntry(result.taskId, toRelationType(type), sourceBpnl, targetBpnl, validityPeriods, reasonCode)
            }
            is GoldenRecordRelationTaskResultRecord.SiteRelationResolved -> with(result.relation) {
                toResolvedEntry(result.taskId, toRelationType(type), sourceBpns, targetBpns, validityPeriods, reasonCode)
            }
            is GoldenRecordRelationTaskResultRecord.AddressRelationResolved -> with(result.relation) {
                toResolvedEntry(result.taskId, toRelationType(type), sourceBpna, targetBpna, validityPeriods, reasonCode)
            }
            is GoldenRecordRelationTaskResultRecord.Rejected ->
                toErrorEntry(result.taskId, toRelations(result.relation), result.errors.map { toDescription(it) })
            is GoldenRecordRelationTaskResultRecord.Failed ->
                toErrorEntry(result.taskId, toRelations(result.relation), listOf(OrchestratorOutboundMapper.UNEXPECTED_TASK_ERROR))
        }

    /**
     * Returns the task step result that reports the golden record relation task with [taskId] as failed without a known cause.
     */
    fun toErrorReportEntry(taskId: String): TaskRelationsStepResultEntryDto =
        toErrorEntry(taskId, BusinessPartnerRelations.empty, listOf(OrchestratorOutboundMapper.UNREPORTED_TASK_ERROR))

    private fun toResolvedEntry(
        taskId: String,
        type: OrchestratorRelationType,
        sourceBpn: String,
        targetBpn: String,
        validityPeriods: List<RelationValidityPeriodRecord>,
        reasonCode: String?
    ) =
        TaskRelationsStepResultEntryDto(
            taskId = taskId,
            businessPartnerRelations = BusinessPartnerRelations(
                relationType = type,
                businessPartnerSourceBpn = sourceBpn,
                businessPartnerTargetBpn = targetBpn,
                validityPeriods = validityPeriods.map { RelationValidityPeriod(validFrom = it.validFrom, validTo = it.validTo) },
                reasonCode = reasonCode
            ),
            errors = emptyList()
        )

    private fun toErrorEntry(taskId: String, relations: BusinessPartnerRelations, descriptions: List<String>) =
        TaskRelationsStepResultEntryDto(
            taskId = taskId,
            businessPartnerRelations = relations,
            errors = descriptions.map { TaskRelationsErrorDto(type = TaskRelationsErrorType.Unspecified, description = it) }
        )

    private fun toRelations(relation: RelationRaw) =
        BusinessPartnerRelations(
            relationType = toRelationType(relation.type),
            businessPartnerSourceBpn = relation.sourceBpn,
            businessPartnerTargetBpn = relation.targetBpn,
            validityPeriods = relation.validityPeriods.map { RelationValidityPeriod(validFrom = it.validFrom, validTo = it.validTo) },
            reasonCode = relation.reasonCode
        )

    private fun toRelationType(type: RelationType): OrchestratorRelationType =
        when (type) {
            RelationType.IsAlternativeHeadquarterFor -> OrchestratorRelationType.IsAlternativeHeadquarterFor
            RelationType.IsManagedBy -> OrchestratorRelationType.IsManagedBy
            RelationType.IsOwnedBy -> OrchestratorRelationType.IsOwnedBy
            RelationType.IsReplacedBy -> OrchestratorRelationType.IsReplacedBy
        }

    private fun toRelationType(type: LegalEntityRelationTypeV7): OrchestratorRelationType =
        when (type) {
            LegalEntityRelationTypeV7.IsAlternativeHeadquarterFor -> OrchestratorRelationType.IsAlternativeHeadquarterFor
            LegalEntityRelationTypeV7.IsManagedBy -> OrchestratorRelationType.IsManagedBy
            LegalEntityRelationTypeV7.IsOwnedBy -> OrchestratorRelationType.IsOwnedBy
            LegalEntityRelationTypeV7.IsReplacedBy -> OrchestratorRelationType.IsReplacedBy
        }

    private fun toRelationType(type: SiteRelationTypeV7): OrchestratorRelationType =
        when (type) {
            SiteRelationTypeV7.IsReplacedBy -> OrchestratorRelationType.IsReplacedBy
        }

    private fun toRelationType(type: AddressRelationTypeV7): OrchestratorRelationType =
        when (type) {
            AddressRelationTypeV7.IsReplacedBy -> OrchestratorRelationType.IsReplacedBy
        }

    private fun toDescription(error: RelationUpsertError): String =
        when (error) {
            is RelationUpsertError.UnsupportedRelation ->
                "Invalid relation: source and target must be of the same business partner type and carry a relation type " +
                        "supported for it (source=${error.sourceBpn}, target=${error.targetBpn}, relationType=${error.type})"
            is RelationUpsertError.SelfReference -> when (error.subject) {
                RelationSubject.LegalEntity -> "A legal entity cannot have a relation to itself (BPNL: ${error.bpn})."
                RelationSubject.Site -> "A site cannot have a relation to itself (BPNS: ${error.bpn})."
                RelationSubject.Address -> "An Address cannot have a relation to itself (BPNA: ${error.bpn})."
            }
            is RelationUpsertError.SourceNotFound -> when (error.subject) {
                RelationSubject.LegalEntity -> "Source legal entity with specified BPNL : ${error.bpn} not found"
                RelationSubject.Site -> "Source site BPNS ${error.bpn} not found"
                RelationSubject.Address -> "Source address BPNA ${error.bpn} not found"
            }
            is RelationUpsertError.TargetNotFound -> when (error.subject) {
                RelationSubject.LegalEntity -> "Target legal entity with specified BPNL : ${error.bpn} not found"
                RelationSubject.Site -> "Target site BPNS ${error.bpn} not found"
                RelationSubject.Address -> "Target address BPNA ${error.bpn} not found"
            }
            is RelationUpsertError.ReasonCodeNotFound -> "Relation reason code '${error.technicalKey}' not found"
            RelationUpsertError.ValidityPeriodsEmpty -> "Relation validity periods cannot be empty, at least one validity needed."
            is RelationUpsertError.ValidityPeriodInverted ->
                "Relation validity period validFrom '${error.validFrom}' cannot be after validTo '${error.validTo}'."
            RelationUpsertError.ValidityPeriodsOverlap -> "Relation validity periods must not overlap."
            is RelationUpsertError.MultipleOwners ->
                "Multiple owning entities assigned to the same owned entity: legal entity '${error.ownedBpn}' can't be owned by " +
                        "'${error.ownerBpn}' as its already owned by '${error.existingOwnerBpn}'"
            is RelationUpsertError.OwnershipCycle ->
                "Circular ownership detected in entity hierarchy: legal entity '${error.ownedBpn}' is (transitively) owning " +
                        "'${error.ownerBpn}' and therefore can't be owned by '${error.ownerBpn}'."
            is RelationUpsertError.MultipleUltimateOwners ->
                "Multiple ultimate owners in entity hierarchy: legal entity '${error.ownedBpn}' can't be owned by '${error.ownerBpn}' " +
                        "as that would put more than one ultimate owner into one ownership hierarchy " +
                        "(flagged: ${error.flaggedBpns.joinToString(", ")})."
            is RelationUpsertError.OwnershipOfAlternative ->
                "Invalid IsOwnedBy relation: Legal entity '${error.legalEntityBpn}' cannot participate in ownership because " +
                        "it is an alternative headquarter to '${error.mainBpn}' in an overlapping period."
            is RelationUpsertError.AlternativeAlreadyMain ->
                "Star topology violated: Legal entity '${error.alternativeBpn}' is already the main of an " +
                        "alternative relation with '${error.otherAlternativeBpn}' in an overlapping period. " +
                        "Cannot also be alternative to '${error.mainBpn}'."
            is RelationUpsertError.MainAlreadyAlternativeTo ->
                "Star topology violated: Legal entity '${error.mainBpn}' is already alternative to " +
                        "'${error.otherMainBpn}' in an overlapping period. Cannot also be main to '${error.alternativeBpn}'."
            is RelationUpsertError.AlternativeAlreadyAlternative ->
                "Star topology violated: Legal entity '${error.alternativeBpn}' is already alternative to " +
                        "'${error.otherMainBpn}' in an overlapping period. Cannot also be alternative to '${error.mainBpn}'."
            is RelationUpsertError.MainAlreadyAlternative ->
                "Star topology violated: Legal entity '${error.mainBpn}' is already alternative to '${error.otherMainBpn}' in an overlapping period. " +
                        "Cannot also be main for '${error.alternativeBpn}'."
            is RelationUpsertError.ReversedAlternative ->
                "Cannot reverse alternative headquarter relation: '${error.mainBpn}' cannot be alternative to '${error.alternativeBpn}' " +
                        "while the reverse relation ('${error.alternativeBpn}' alternative to '${error.mainBpn}') exists in an overlapping period. " +
                        "End the original relation first."
            is RelationUpsertError.AlternativeInOwnership ->
                "Invalid alternative headquarter relation: Legal entity '${error.alternativeBpn}' cannot be alternative " +
                        "because it participates in an overlapping IsOwnedBy relation " +
                        "(as ${if (error.isOwned) "owned" else "owning"} entity with '${error.otherBpn}')."
            is RelationUpsertError.AlternativeFlaggedUltimate ->
                "Invalid alternative headquarter relation: Legal entity '${error.alternativeBpn}' cannot be alternative " +
                        "because it carries the ownershipUltimate flag."
            is RelationUpsertError.ManagerNotParticipant ->
                "Invalid 'IsManagedBy' relation: The Managing Legal Entity with BPNL '${error.managerBpn}' is not a dataspace participant. " +
                        "Only dataspace participants can manage other entities."
            is RelationUpsertError.MultipleManagers ->
                "Invalid 'IsManagedBy' relation: The Managed Legal Entity with BPNL '${error.managedBpn}' is already managed by another " +
                        "Managing Legal Entity '${error.managerBpns.joinToString()}'. A Managed Legal Entity may only have one Managing Legal Entity at a time."
            is RelationUpsertError.ManagedIsManager ->
                "Invalid 'IsManagedBy' relation: The legal entity with BPNL '${error.managedBpn}' is already a Managing Legal Entity. " +
                        "A Managing Legal Entity cannot also act as a Managed Legal Entity."
            is RelationUpsertError.ManagerIsManaged ->
                "Invalid 'IsManagedBy' relation: The legal entity with BPNL '${error.managerBpn}' is already a Managed Legal Entity. " +
                        "A Managed Legal Entity cannot also act as a Managing Legal Entity."
            is RelationUpsertError.PastValidityPeriodAdded -> "Can't add a new relation validity period from ${error.validFrom} as it lies in the past."
            is RelationUpsertError.PastValidityPeriodLimited -> "Existing relation validity period from ${error.validFrom} can't be limited to past date."
            is RelationUpsertError.PastValidityPeriodDeleted -> "Existing relation validity period from ${error.validFrom} can't be deleted as it lies in the past."
            is RelationUpsertError.MultipleSuccessors -> {
                val partner = partnerNoun(error.subject)
                "Multiple successors assigned to the same $partner: $partner '${error.predecessorBpn}' can't be replaced by " +
                        "'${error.successorBpn}' as it is already replaced by '${error.existingSuccessorBpn}' in an overlapping validity period."
            }
            is RelationUpsertError.SuccessionCycle ->
                "Circular replacement detected: ${partnerNoun(error.subject)} '${error.predecessorBpn}' is (transitively) replacing " +
                        "'${error.successorBpn}' and therefore can't be replaced by '${error.successorBpn}'."
            is RelationUpsertError.SuccessionAcrossLegalEntities -> describeSuccessionAcrossLegalEntities(error)
        }

    private fun describeSuccessionAcrossLegalEntities(error: RelationUpsertError.SuccessionAcrossLegalEntities): String {
        val (partner, partners, bpnType) = when (error.subject) {
            RelationSubject.LegalEntity -> Triple("legal entity", "legal entities", "BPNL")
            RelationSubject.Site -> Triple("site", "sites", "BPNS")
            RelationSubject.Address -> Triple("address", "addresses", "BPNA")
        }
        return "Invalid 'IsReplacedBy' relation: The source $partner with $bpnType '${error.predecessorBpn}' and target $partner with $bpnType " +
                "'${error.successorBpn}' do not belong to the same Legal Entity (BPNL '${error.predecessorLegalEntityBpn}' and " +
                "'${error.successorLegalEntityBpn}'). Both $partners must belong to the same Legal Entity to create an " +
                "'IsReplacedBy' relation."
    }

    private fun partnerNoun(subject: RelationSubject): String =
        when (subject) {
            RelationSubject.LegalEntity -> "legal entity"
            RelationSubject.Site -> "site"
            RelationSubject.Address -> "address"
        }
}
