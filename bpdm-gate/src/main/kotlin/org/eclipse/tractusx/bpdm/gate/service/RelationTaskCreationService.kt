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

import jakarta.persistence.EntityManager
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateErrorCode
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.eclipse.tractusx.bpdm.gate.repository.RelationStageRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class RelationTaskCreationService(
    private val taskConfigProperties: GoldenRecordTaskConfigProperties,
    private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val relationRepository: RelationRepository,
    private val relationStageRepository: RelationStageRepository,
    private val orchestratorClient: OrchestrationApiClient,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val relationSharingStateService: RelationSharingStateService
) {
    private val logger = KotlinLogging.logger { }

    fun sendTasks(): Int{
        logger.debug { "Started scheduled task to create golden record tasks from business partner relations" }

        stageRelations()
        val sentTasks = sendStagedRelations()

        if (sentTasks.isNotEmpty())
            logger.info { "Created ${sentTasks.size} new golden record tasks from business partner relations: ${sentTasks.toLogIdentifiers()}" }
        else
            logger.debug { "No business partner relations to create golden record tasks from" }

        return sentTasks.size
    }

    private fun stageRelations(){
        var stagedCount = 0
        do{
            stagedCount = transactionTemplate.execute { stageRelationsForSending(taskConfigProperties.relationCreation.batchSize) } ?: 0

            entityManager.clear()
        }while (stagedCount > 0)
    }

    private fun sendStagedRelations(): List<CreatedGoldenRecordTask>{
        val totalSentTasks = mutableListOf<CreatedGoldenRecordTask>()
        var sentTasks: List<CreatedGoldenRecordTask>
        do{
            sentTasks = transactionTemplate.execute { sendTaskBatch(taskConfigProperties.relationCreation.batchSize) } ?: emptyList()
            totalSentTasks.addAll(sentTasks)
            entityManager.clear()
        }while (sentTasks.isNotEmpty())

        return totalSentTasks
    }

    fun stageRelationsForSending(batchSize: Int): Int{
        val toStagePage = relationRepository.findBySharingStateAndStaged(RelationSharingStateType.Ready, false, PageRequest.ofSize(batchSize))
        toStagePage.content.forEach(::stage)
        return toStagePage.content.size
    }

    fun sendTaskBatch(batchSize: Int): List<CreatedGoldenRecordTask>{
        val toSendPage = relationRepository.findBySharingStateAndStaged(RelationSharingStateType.Ready, true, PageRequest.ofSize(batchSize))
        val toSendRelations = toSendPage.content
        if (toSendRelations.isEmpty()) return emptyList()

        val toSendStages = relationStageRepository.findByRelationInAndStage(toSendRelations.toSet(), StageType.Input)
        val stagesByRelation = toSendStages.associateBy { it.relation.id }

        val sharingStates = toSendStages.map { it.source } + toSendStages.map { it.target }
        val outputs = businessPartnerRepository.findBySharingStateInAndStage(sharingStates.toSet(), StageType.Output)
        val outputsBySharingState = outputs.associateBy { it.sharingState }

        val taskCreateRequests = toSendRelations.map { relation ->
            val sharingState = relation.sharingState ?: run {
                logger.debug { "Relation has no sharingState; skipping" }
                return@map null
            }
            val relationStage = stagesByRelation[relation.id] ?: run {
                logger.debug { "No relation stage found for relation; skipping" }
                return@map null
            }

            val sourceOutput = outputsBySharingState[relationStage.source] ?: run {
                logger.debug { "Source output not available yet for relation; skipping" }
                return@map null
            }

            val targetOutput = outputsBySharingState[relationStage.target] ?: run {
                logger.debug { "Target output not available yet for relation; skipping" }
                return@map null
            }

            //Determine which kind of golden record relation task we need
            val taskKind = determineTaskKind(sourceOutput.postalAddress.addressType, targetOutput.postalAddress.addressType, relationStage.relationType)
            if (taskKind == null) {
                failRelation(
                    relation,
                    "Unsupported relation combination for relation: relationType='${relationStage.relationType}', " +
                            "sourceAddressType='${sourceOutput.postalAddress.addressType}', targetAddressType='${targetOutput.postalAddress.addressType}'"
                )
                return@map null
            }

            //Select BPN values strictly based on the determined task kind
            val (sourceBpn, targetBpn) = when (taskKind) {
                RelationTaskKind.LegalEntity -> sourceOutput.bpnL to targetOutput.bpnL
                RelationTaskKind.Site -> sourceOutput.bpnS to targetOutput.bpnS
                RelationTaskKind.Address -> sourceOutput.bpnA to targetOutput.bpnA
            }

            if (sourceBpn.isNullOrBlank() || targetBpn.isNullOrBlank()) {
                failRelation(
                    relation,
                    "${taskKind.name} task requires ${taskKind.bpnType} for both sides but missing for relation: " +
                            "source${taskKind.bpnType}='$sourceBpn', target${taskKind.bpnType}='$targetBpn'"
                )
                return@map null
            }

            val relationType = relationStage.relationType.toOrchestratorModel() ?: run {
                logger.debug { "Cannot map relation type for relation; skipping" }
                return@map null
            }

            val validityPeriods = relationStage.validityPeriods.map { it.toOrchestratorModel() }

            TaskCreateRelationsRequestEntry(
                recordId = sharingState.recordId,
                businessPartnerRelations = BusinessPartnerRelations(
                    relationType = relationType,
                    businessPartnerSourceBpn = sourceBpn,
                    businessPartnerTargetBpn = targetBpn,
                    validityPeriods = validityPeriods,
                    reasonCode = relationStage.reasonCode
                )
            )
        }

        val createdTasks = taskCreateRequests.letNonNull { sendTasks(it) }

        return toSendRelations.zip(createdTasks){ relation, createdTask ->
            if (createdTask != null){
                relationSharingStateService.setPending(relation, createdTask.taskId, createdTask.recordId)
                CreatedGoldenRecordTask(relation.externalId, createdTask.taskId)
            }else{
                unstage(relation)
                null
            }
        }.filterNotNull()
    }

    private fun determineTaskKind(
        sourceAddressType: AddressType?,
        targetAddressType: AddressType?,
        relationType: RelationType
    ): RelationTaskKind? {

        fun isLegalEntityLike(t: AddressType) = when (t) {
            AddressType.LegalAndSiteMainAddress,
            AddressType.LegalAddress -> true

            else -> false
        }

        // A site relation requires SiteMainAddress on both sides rather than everything a site can be reached through:
        // LegalAndSiteMainAddress is a legal entity too, and IsReplacedBy between two of those already means the legal
        // entities succeed each other. A site sharing its legal entity's address can therefore not be replaced.
        fun isSiteMainOnly(t: AddressType) = t == AddressType.SiteMainAddress

        if (sourceAddressType == null || targetAddressType == null) return null

        val bothLegalEntityLike = isLegalEntityLike(sourceAddressType) && isLegalEntityLike(targetAddressType)

        return when (relationType) {
            RelationType.IsAlternativeHeadquarterFor,
            RelationType.IsOwnedBy,
            RelationType.IsManagedBy -> if (bothLegalEntityLike) RelationTaskKind.LegalEntity else null

            // An address succession is what remains once neither higher level claims the pair: every refined record has
            // a BPNA, so any other IsReplacedBy pair is a succession between the two addresses themselves.
            RelationType.IsReplacedBy -> when {
                bothLegalEntityLike -> RelationTaskKind.LegalEntity
                isSiteMainOnly(sourceAddressType) && isSiteMainOnly(targetAddressType) -> RelationTaskKind.Site
                else -> RelationTaskKind.Address
            }
        }
    }

    private fun failRelation(relation: RelationDb, message: String) {
        logger.warn { message }
        unstage(relation)
        try {
            relationSharingStateService.setError(relation, RelationSharingStateErrorCode.SharingProcessError, message)
        } catch (t: Throwable) {
            logger.debug(t) { "setError failed" }
        }
    }

    private enum class RelationTaskKind(val bpnType: String) {
        LegalEntity("BPNL"),
        Site("BPNS"),
        Address("BPNA")
    }

    private fun sendTasks(taskCreateRequests: List<TaskCreateRelationsRequestEntry>): List<TaskClientRelationsStateDto?>{
        return try{
            if(taskCreateRequests.isEmpty())
                emptyList()
            else
                orchestratorClient.relationsGoldenRecordTasks.createTasks(TaskCreateRelationsRequest(TaskMode.UpdateFromSharingMember, taskCreateRequests)).createdTasks
        }catch (_: Throwable){
            logger.error { "Error encountered when trying to create tasks at the orchestrator" }
            taskCreateRequests.map { null }
        }
    }

    private fun RelationType.toOrchestratorModel(): org.eclipse.tractusx.orchestrator.api.model.RelationType?{
        return when(this){
            RelationType.IsManagedBy -> org.eclipse.tractusx.orchestrator.api.model.RelationType.IsManagedBy
            RelationType.IsAlternativeHeadquarterFor -> org.eclipse.tractusx.orchestrator.api.model.RelationType.IsAlternativeHeadquarterFor
            RelationType.IsOwnedBy -> org.eclipse.tractusx.orchestrator.api.model.RelationType.IsOwnedBy
            RelationType.IsReplacedBy -> org.eclipse.tractusx.orchestrator.api.model.RelationType.IsReplacedBy
        }
    }

    private fun stage(relation: RelationDb){
        relation.sharingState?.isStaged = true
        relationRepository.save(relation)
    }

    private fun unstage(relation: RelationDb){
        relation.sharingState?.isStaged = false
        relationRepository.save(relation)
    }


    private fun <INPUT, OUTPUT> List<INPUT?>.letNonNull(transform: (List<INPUT>) -> List<OUTPUT?>): List<OUTPUT?>{
        val result = transform(filterNotNull())
        var resultIndex = 0
        return map { element ->
            if(element == null) return@map null
            result[resultIndex++]
        }
    }

    // map a single RelationStateDb to the orchestrator DTO (never nullable unless you expect failure cases)
    private fun RelationValidityPeriodDb.toOrchestratorModel(): RelationValidityPeriod {
        return RelationValidityPeriod(validFrom = this.validFrom, validTo = this.validTo)
    }

}