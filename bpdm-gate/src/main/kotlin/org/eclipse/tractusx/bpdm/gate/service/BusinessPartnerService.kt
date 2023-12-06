/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.response.PageDto
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.util.copyAndSync
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntry
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.bpdm.gate.exception.BpdmMissingStageException
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BusinessPartnerService(
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val businessPartnerMappings: BusinessPartnerMappings,
    private val sharingStateService: SharingStateService,
    private val changelogRepository: ChangelogRepository,
    private val orchestrationApiClient: OrchestrationApiClient,
    private val orchestratorMappings: OrchestratorMappings,
    private val sharingStateRepository: SharingStateRepository
) {

    @Transactional
    fun upsertBusinessPartnersInput(dtos: List<BusinessPartnerInputRequest>): List<BusinessPartnerInputDto> {
        val entities = dtos.map { dto -> businessPartnerMappings.toBusinessPartnerInput(dto) }
        //Validation method
        val validatedEntities = filterUpdateCandidates(entities, StageType.Input)
        return upsertBusinessPartnersInputFromCandidates(validatedEntities).map(businessPartnerMappings::toBusinessPartnerInputDto)
    }

    @Transactional
    fun upsertBusinessPartnersOutput(dtos: Collection<BusinessPartnerOutputRequest>): Collection<BusinessPartnerOutputDto> {
        val entities = dtos.map { dto -> businessPartnerMappings.toBusinessPartnerOutput(dto) }
        return upsertBusinessPartnersOutputFromCandidates(entities).map(businessPartnerMappings::toBusinessPartnerOutputDto)
    }

    fun getBusinessPartnersInput(pageRequest: PageRequest, externalIds: Collection<String>?): PageDto<BusinessPartnerInputDto> {
        val stage = StageType.Input
        return getBusinessPartners(pageRequest, externalIds, stage)
            .toPageDto(businessPartnerMappings::toBusinessPartnerInputDto)
    }

    fun getBusinessPartnersOutput(pageRequest: PageRequest, externalIds: Collection<String>?): PageDto<BusinessPartnerOutputDto> {
        val stage = StageType.Output
        return getBusinessPartners(pageRequest, externalIds, stage)
            .toPageDto(businessPartnerMappings::toBusinessPartnerOutputDto)
    }


    private fun upsertBusinessPartnersInputFromCandidates(entityCandidates: List<BusinessPartner>): List<BusinessPartner> {
        val resolutionResults = resolveCandidatesForStage(entityCandidates, StageType.Input)

        saveChangelog(resolutionResults)

        val partners = resolutionResults.map { it.businessPartner }
        val orchestratorBusinessPartnersDto = resolutionResults.map { orchestratorMappings.toBusinessPartnerGenericDto(it.businessPartner) }

        val taskIds = createGoldenRecordTasks(orchestratorBusinessPartnersDto).createdTasks.map { it.taskId }

        val pendingRequests =  partners.zip(taskIds)
            .map { (partner, taskId) ->
                SharingStateService.PendingRequest(
                    SharingStateService.SharingStateIdentifierDto(
                        partner.externalId,
                        BusinessPartnerType.GENERIC
                    ), taskId
                )
            }
        sharingStateService.setPending(pendingRequests)

        return businessPartnerRepository.saveAll(partners)
    }

    private fun upsertBusinessPartnersOutputFromCandidates(entityCandidates: List<BusinessPartner>): List<BusinessPartner> {
        val externalIds = entityCandidates.map { it.externalId }
        assertInputStageExists(externalIds)

        val validatedEntities = filterUpdateCandidates(entityCandidates, StageType.Output)

        val resolutionResults = resolveCandidatesForStage(validatedEntities, StageType.Output)

        saveChangelog(resolutionResults)

        val partners = resolutionResults.map { it.businessPartner }

        val successRequests = partners.map {
            SharingStateService.SuccessRequest(
                SharingStateService.SharingStateIdentifierDto(it.externalId, BusinessPartnerType.GENERIC),
                it.bpnA!!
            )
        }
        sharingStateService.setSuccess(successRequests)

        return businessPartnerRepository.saveAll(partners)
    }

    private fun getBusinessPartners(pageRequest: PageRequest, externalIds: Collection<String>?, stage: StageType): Page<BusinessPartner> {
        return when {
            externalIds.isNullOrEmpty() -> businessPartnerRepository.findByStage(stage, pageRequest)
            else -> businessPartnerRepository.findByStageAndExternalIdIn(stage, externalIds, pageRequest)
        }
    }

    private fun saveChangelog(resolutionResults: Collection<ResolutionResult>) {
        resolutionResults.forEach { result ->
            if (result.wasResolved)
                saveChangelog(result.businessPartner, ChangelogType.UPDATE)
            else
                saveChangelog(result.businessPartner, ChangelogType.CREATE)
        }
    }

    private fun saveChangelog(partner: BusinessPartner, changelogType: ChangelogType) {
        changelogRepository.save(ChangelogEntry(partner.externalId, BusinessPartnerType.GENERIC, changelogType, partner.stage))
    }

    private fun assertInputStageExists(externalIds: Collection<String>) {
        val existingExternalIds = businessPartnerRepository.findByStageAndExternalIdIn(StageType.Input, externalIds)
            .map { it.externalId }
            .toSet()

        externalIds.minus(existingExternalIds)
            .takeIf { it.isNotEmpty() }
            ?.let { throw BpdmMissingStageException(it, StageType.Input) }
    }

    /**
     * Filters all [entities] by looking for existing business partner data in the given [stage]
     *
     * Filters incoming Business Partner for changes against the same persisted record in the Database
     * If the data is the same, the consequent Business Partner will not have a new changelog and the golden record
     * process will not start. If Business Partner has the same data, but linked sharing state has an error state,
     * Business Partner will start the process again.
     */
    private fun filterUpdateCandidates(entities: List<BusinessPartner>, stage: StageType): List<BusinessPartner> {
        val externalIds = entities.map { it.externalId }
        val persistedBusinessPartnerMap = businessPartnerRepository.findByStageAndExternalIdIn(stage, externalIds).associateBy { it.externalId }
        val sharingStatesMap =
            sharingStateRepository.findByExternalIdInAndBusinessPartnerType(externalIds, BusinessPartnerType.GENERIC).associateBy { it.externalId }

        return entities.filter { entity ->
            val matchingBusinessPartner = persistedBusinessPartnerMap[entity.externalId]
            val hasErrorSharingState = sharingStatesMap[entity.externalId]?.sharingStateType == SharingStateType.Error

            matchingBusinessPartner?.let { hasChanges(entity, it) } ?: true || hasErrorSharingState //If there are difference return true, else returns false
        }
    }

    private fun hasChanges(entity: BusinessPartner, persistedBP: BusinessPartner): Boolean {

        return entity.nameParts != persistedBP.nameParts ||
                entity.roles != persistedBP.roles ||
                entity.shortName != persistedBP.shortName ||
                entity.legalName != persistedBP.legalName ||
                entity.legalForm != persistedBP.legalForm ||
                entity.isOwnCompanyData != persistedBP.isOwnCompanyData ||
                entity.bpnL != persistedBP.bpnL ||
                entity.bpnS != persistedBP.bpnS ||
                entity.bpnA != persistedBP.bpnA ||
                entity.stage != persistedBP.stage ||
                entity.parentId != persistedBP.parentId ||
                entity.parentType != persistedBP.parentType ||
                entity.identifiers != persistedBP.identifiers ||
                entity.states != persistedBP.states ||
                entity.classifications != persistedBP.classifications ||
                postalAddressHasChanges(entity.postalAddress, persistedBP.postalAddress)
    }

    private fun postalAddressHasChanges(entityPostalAddress: PostalAddress, persistedPostalAddress: PostalAddress): Boolean {
        return (entityPostalAddress.addressType != persistedPostalAddress.addressType) ||
                (entityPostalAddress.alternativePostalAddress != persistedPostalAddress.alternativePostalAddress) ||
                (entityPostalAddress.physicalPostalAddress != persistedPostalAddress.physicalPostalAddress)
    }

    /**
     * Resolve all [entityCandidates] by looking for existing business partner data in the given [stage]
     *
     * Resolving a candidate means to exchange the candidate entity with the existing entity and copy from the candidate to that existing entity.
     *
     */
    private fun resolveCandidatesForStage(entityCandidates: List<BusinessPartner>, stage: StageType): List<ResolutionResult> {
        val existingPartnersByExternalId = businessPartnerRepository.findByStageAndExternalIdIn(stage, entityCandidates.map { it.externalId })
            .associateBy { it.externalId }

        return entityCandidates.map { candidate ->
            val existingEntity = existingPartnersByExternalId[candidate.externalId]
            if (existingEntity != null)
                ResolutionResult(copyValues(candidate, existingEntity), true)
            else
                ResolutionResult(candidate, false)
        }
    }

    data class ResolutionResult(
        val businessPartner: BusinessPartner,
        val wasResolved: Boolean
    )

    private fun copyValues(fromPartner: BusinessPartner, toPartner: BusinessPartner): BusinessPartner {
        return toPartner.apply {
            stage = fromPartner.stage
            shortName = fromPartner.shortName
            legalName = fromPartner.legalName
            legalForm = fromPartner.legalForm
            isOwnCompanyData = fromPartner.isOwnCompanyData
            bpnL = fromPartner.bpnL
            bpnS = fromPartner.bpnS
            bpnA = fromPartner.bpnA
            parentId = fromPartner.parentId
            parentType = fromPartner.parentType

            nameParts.replace(fromPartner.nameParts)
            roles.replace(fromPartner.roles)

            states.copyAndSync(fromPartner.states, ::copyValues)
            classifications.copyAndSync(fromPartner.classifications, ::copyValues)
            identifiers.copyAndSync(fromPartner.identifiers, ::copyValues)

            copyValues(fromPartner.postalAddress, postalAddress)
        }
    }

    private fun copyValues(fromState: State, toState: State) =
        toState.apply {
            validFrom = fromState.validFrom
            validTo = fromState.validTo
            description = fromState.description
            type = fromState.type
        }

    private fun copyValues(fromClassification: Classification, toClassification: Classification) =
        toClassification.apply {
            value = fromClassification.value
            type = fromClassification.type
            code = fromClassification.code
        }

    private fun copyValues(fromIdentifier: Identifier, toIdentifier: Identifier) =
        toIdentifier.apply {
            type = fromIdentifier.type
            value = fromIdentifier.value
            issuingBody = fromIdentifier.issuingBody
        }

    private fun copyValues(fromPostalAddress: PostalAddress, toPostalAddress: PostalAddress) =
        toPostalAddress.apply {
            addressType = fromPostalAddress.addressType
            physicalPostalAddress = fromPostalAddress.physicalPostalAddress
            alternativePostalAddress = fromPostalAddress.alternativePostalAddress
        }


    private fun createGoldenRecordTasks(orchestratorBusinessPartnersDto: List<BusinessPartnerGenericDto>): TaskCreateResponse {
        return orchestrationApiClient.goldenRecordTasks.createTasks(
            TaskCreateRequest(
                TaskMode.UpdateFromSharingMember, orchestratorBusinessPartnersDto
            )
        )
    }

    @Scheduled(cron = "\${bpdm.cleaningService.pollingCron:-}", zone = "UTC")
    @Transactional
    fun finishCleaningTask() {
        val sharingStates = sharingStateRepository.findBySharingStateTypeAndTaskIdNotNull(SharingStateType.Pending)
        val tasks = orchestrationApiClient.goldenRecordTasks.searchTaskStates(TaskStateRequest(sharingStates.map { it.taskId!! })).tasks

        val sharingStateMap = sharingStates.associateBy { it.taskId }

        val taskStatesByResult = tasks
            .map { Pair(it, sharingStateMap[it.taskId]!!) }
            .groupBy { (task, _) -> task.processingState.resultState }

        val businessPartnersToUpsert = taskStatesByResult[ResultState.Success]?.map { (task, sharingState) ->
            orchestratorMappings.toBusinessPartner(task.businessPartnerResult!!, sharingState.externalId)
        } ?: emptyList()
        upsertBusinessPartnersOutputFromCandidates(businessPartnersToUpsert)


        val errorRequests = taskStatesByResult[ResultState.Error]?.map { (task, sharingState) ->
            SharingStateService.ErrorRequest(
                SharingStateService.SharingStateIdentifierDto(sharingState.externalId, sharingState.businessPartnerType),
                BusinessPartnerSharingError.SharingProcessError,
                if (task.processingState.errors.isNotEmpty()) task.processingState.errors.joinToString(" // ") { it.description } else null
            )
        } ?: emptyList()
        sharingStateService.setError(errorRequests)

    }

}
