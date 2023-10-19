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
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntry
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.bpdm.gate.exception.BpdmMissingStageException
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BusinessPartnerService(
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val businessPartnerMappings: BusinessPartnerMappings,
    private val sharingStateService: SharingStateService,
    private val changelogRepository: ChangelogRepository,
    private val orchestrationApiClient: OrchestrationApiClient,
    private val orchestratorMappings: OrchestratorMappings
) {

    @Transactional
    fun upsertBusinessPartnersInput(dtos: Collection<BusinessPartnerInputRequest>): Collection<BusinessPartnerInputDto> {
        val entities = dtos.map { dto -> businessPartnerMappings.toBusinessPartnerInput(dto) }
        return upsertBusinessPartnersInput(entities).map(businessPartnerMappings::toBusinessPartnerInputDto)
    }

    @Transactional
    fun upsertBusinessPartnersOutput(dtos: Collection<BusinessPartnerOutputRequest>): Collection<BusinessPartnerOutputDto> {
        val entities = dtos.map { dto -> businessPartnerMappings.toBusinessPartnerOutput(dto) }
        return upsertBusinessPartnersOutput(entities).map(businessPartnerMappings::toBusinessPartnerOutputDto)
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

    private fun upsertBusinessPartnersInput(entityCandidates: List<BusinessPartner>): List<BusinessPartner> {
        val resolutionResults = resolveCandidatesForStage(entityCandidates, StageType.Input)

        saveChangelog(resolutionResults)

        val partners = resolutionResults.map { it.businessPartner }
        val orchestratorBusinessPartnersDto = resolutionResults.map { orchestratorMappings.toBusinessPartnerGenericDto(it.businessPartner) }
        partners.forEach { entity ->
            initSharingState(entity)
        }

        val taskCreateResponse = createGoldenRecordTasks(orchestratorBusinessPartnersDto)

        for (i in partners.indices) {
            updateSharingState(partners[i], taskCreateResponse.createdTasks[i])
        }

        return businessPartnerRepository.saveAll(partners)
    }

    private fun upsertBusinessPartnersOutput(entityCandidates: List<BusinessPartner>): List<BusinessPartner> {
        val externalIds = entityCandidates.map { it.externalId }
        assertInputStageExists(externalIds)

        val resolutionResults = resolveCandidatesForStage(entityCandidates, StageType.Output)

        saveChangelog(resolutionResults)

        return businessPartnerRepository.saveAll(resolutionResults.map { it.businessPartner })
    }

    private fun getBusinessPartners(pageRequest: PageRequest, externalIds: Collection<String>?, stage: StageType): Page<BusinessPartner> {
        return when {
            externalIds.isNullOrEmpty() -> businessPartnerRepository.findByStage(stage, pageRequest)
            else -> businessPartnerRepository.findByStageAndExternalIdIn(stage, externalIds, pageRequest)
        }
    }

    private fun initSharingState(entity: BusinessPartner) {
        // TODO make businessPartnerType optional
        sharingStateService.upsertSharingState(SharingStateDto(BusinessPartnerType.GENERIC, entity.externalId))
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

    private fun updateSharingState(entity: BusinessPartner, stateDto: TaskClientStateDto) {
        sharingStateService.upsertSharingState(
            SharingStateDto(
                BusinessPartnerType.ADDRESS,
                entity.externalId,
                sharingStateType = orchestratorMappings.toSharingStateType(stateDto.processingState.resultState),
                taskId = stateDto.taskId
            )
        )

    }

}
