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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.util.copyAndSync
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntryDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.bpdm.gate.exception.BpdmMissingStageException
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateResponse
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BusinessPartnerService(
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val businessPartnerMappings: BusinessPartnerMappings,
    private val sharingStateService: SharingStateService,
    private val changelogRepository: ChangelogRepository,
    private val sharingStateRepository: SharingStateRepository,
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun upsertBusinessPartnersInput(dtos: List<BusinessPartnerInputRequest>, ownerBpnl: String?): List<BusinessPartnerInputDto> {
        logger.debug { "Executing upsertBusinessPartnersInput() with parameters $dtos" }
        return OwnerContext(ownerBpnl).upsertBusinessPartnersInput(dtos)
    }

    fun getBusinessPartnersInput(pageRequest: PageRequest, externalIds: Collection<String>?, ownerBpnl: String?): PageDto<BusinessPartnerInputDto> {
        logger.debug { "Executing getBusinessPartnersInput() with parameters $pageRequest and $externalIds" }
        return OwnerContext(ownerBpnl).getBusinessPartnersInput(pageRequest, externalIds)
    }

    fun getBusinessPartnersOutput(pageRequest: PageRequest, externalIds: Collection<String>?, ownerBpnl: String?): PageDto<BusinessPartnerOutputDto> {
        logger.debug { "Executing getBusinessPartnersOutput() with parameters $pageRequest and $externalIds" }
        return OwnerContext(ownerBpnl).getBusinessPartnersOutput(pageRequest, externalIds)
    }

    fun upsertBusinessPartnersOutputFromCandidates(entityCandidates: List<BusinessPartnerDb>, ownerBpnl: String?): List<BusinessPartnerDb> {
        logger.debug { "Upsert output from candidates $entityCandidates" }
        val candidateSet = entityCandidates.groupBy { it.externalId }.map { group -> group.value.last() }
        return OwnerContext(ownerBpnl).upsertBusinessPartnersOutputFromCandidates(candidateSet)
    }

    inner class OwnerContext(
        private val ownerBpnl: String?
    ){
        @Transactional
        fun upsertBusinessPartnersInput(dtos: List<BusinessPartnerInputRequest>): List<BusinessPartnerInputDto> {
            val entities = dtos.map { dto -> businessPartnerMappings.toBusinessPartnerInput(dto, ownerBpnl) }
            //Validation method
            val validatedEntities = filterUpdateCandidates(entities, StageType.Input)
            return upsertBusinessPartnersInputFromCandidates(validatedEntities).map(businessPartnerMappings::toBusinessPartnerInputDto)
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

        fun upsertBusinessPartnersOutputFromCandidates(entityCandidates: List<BusinessPartnerDb>): List<BusinessPartnerDb> {
            val externalIds = entityCandidates.map { it.externalId }
            assertInputStageExists(externalIds)

            val changedBusinessPartners = filterUpdateCandidates(entityCandidates, StageType.Output)

            val resolutionResults = resolveCandidatesForStage(changedBusinessPartners, StageType.Output)

            saveChangelog(resolutionResults)

            val changedPartners = resolutionResults.map { it.businessPartner }

            val successRequests = entityCandidates.map {
                SharingStateService.SuccessRequest(it.externalId)
            }
            sharingStateService.setSuccess(successRequests, ownerBpnl)

            return businessPartnerRepository.saveAll(changedPartners)
        }

        private fun upsertBusinessPartnersInputFromCandidates(entityCandidates: List<BusinessPartnerDb>): List<BusinessPartnerDb> {
            val resolutionResults = resolveCandidatesForStage(entityCandidates, StageType.Input)

            saveChangelog(resolutionResults)

            val partners = resolutionResults.map { it.businessPartner }
            sharingStateService.setInitial(partners.map { it.externalId }, ownerBpnl)

            partners.forEach { logger.info { "Business Partner ${it.externalId} was created or updated" } }

            return businessPartnerRepository.saveAll(partners)
        }

        private fun getBusinessPartners(pageRequest: PageRequest, externalIds: Collection<String>?, stage: StageType): Page<BusinessPartnerDb> {
            return when {
                externalIds.isNullOrEmpty() -> businessPartnerRepository.findByStageAndAssociatedOwnerBpnl(stage, associatedOwnerBpnl = ownerBpnl,pageRequest)
                else -> businessPartnerRepository.findByStageAndAssociatedOwnerBpnlAndExternalIdIn(stage, associatedOwnerBpnl = ownerBpnl,externalIds, pageRequest)
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

        private fun saveChangelog(partner: BusinessPartnerDb, changelogType: ChangelogType) {
            changelogRepository.save(ChangelogEntryDb(externalId = partner.externalId,changelogType = changelogType, stage =  partner.stage, associatedOwnerBpnl = ownerBpnl))
        }

        private fun assertInputStageExists(externalIds: Collection<String>) {
            val existingExternalIds = businessPartnerRepository.findByStageAndAssociatedOwnerBpnlAndExternalIdIn(StageType.Input, ownerBpnl, externalIds, Pageable.unpaged())
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
        private fun filterUpdateCandidates(entities: List<BusinessPartnerDb>, stage: StageType): List<BusinessPartnerDb> {
            val externalIds = entities.map { it.externalId }
            val persistedBusinessPartnerMap = businessPartnerRepository.findByStageAndExternalIdIn(stage, externalIds).associateBy { it.externalId }
            val sharingStatesMap =
                sharingStateRepository.findByExternalIdInAndAssociatedOwnerBpnl(externalIds, ownerBpnl).associateBy { it.externalId }

            return entities.filter { entity ->
                val matchingBusinessPartner = persistedBusinessPartnerMap[entity.externalId]
                val hasErrorSharingState = sharingStatesMap[entity.externalId]?.sharingStateType == SharingStateType.Error

                matchingBusinessPartner?.let { hasChanges(entity, it) } ?: true || hasErrorSharingState //If there are difference return true, else returns false
            }
        }

        private fun hasChanges(entity: BusinessPartnerDb, persistedBP: BusinessPartnerDb): Boolean {

            return entity.nameParts != persistedBP.nameParts ||
                    entity.roles != persistedBP.roles ||
                    entity.shortName != persistedBP.shortName ||
                    entity.legalName != persistedBP.legalName ||
                    entity.siteName != persistedBP.siteName ||
                    entity.addressName != persistedBP.addressName ||
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

        private fun postalAddressHasChanges(entityPostalAddress: PostalAddressDb, persistedPostalAddress: PostalAddressDb): Boolean {
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
        private fun resolveCandidatesForStage(entityCandidates: List<BusinessPartnerDb>, stage: StageType): List<ResolutionResult> {
            val existingPartnersByExternalId = businessPartnerRepository.findByStageAndAssociatedOwnerBpnlAndExternalIdIn(stage, ownerBpnl, entityCandidates.map { it.externalId }, Pageable.unpaged())
                .associateBy { it.externalId }

            return entityCandidates.map { candidate ->
                val existingEntity = existingPartnersByExternalId[candidate.externalId]
                if (existingEntity != null)
                    ResolutionResult(copyValues(candidate, existingEntity), true)
                else
                    ResolutionResult(candidate, false)
            }
        }

        private fun copyValues(fromPartner: BusinessPartnerDb, toPartner: BusinessPartnerDb): BusinessPartnerDb {
            return toPartner.apply {
                stage = fromPartner.stage
                shortName = fromPartner.shortName
                legalName = fromPartner.legalName
                siteName = fromPartner.siteName
                addressName = fromPartner.addressName
                legalForm = fromPartner.legalForm
                isOwnCompanyData = fromPartner.isOwnCompanyData
                bpnL = fromPartner.bpnL
                bpnS = fromPartner.bpnS
                bpnA = fromPartner.bpnA
                parentId = fromPartner.parentId
                parentType = fromPartner.parentType
                legalEntityConfidence = fromPartner.legalEntityConfidence
                siteConfidence = fromPartner.siteConfidence
                addressConfidence = fromPartner.addressConfidence

                nameParts.replace(fromPartner.nameParts)
                roles.replace(fromPartner.roles)

                states.copyAndSync(fromPartner.states, ::copyValues)
                classifications.copyAndSync(fromPartner.classifications, ::copyValues)
                identifiers.copyAndSync(fromPartner.identifiers, ::copyValues)

                copyValues(fromPartner.postalAddress, postalAddress)
            }
        }

        private fun copyValues(fromState: StateDb, toState: StateDb) =
            toState.apply {
                validFrom = fromState.validFrom
                validTo = fromState.validTo
                type = fromState.type
            }

        private fun copyValues(fromClassification: ClassificationDb, toClassification: ClassificationDb) =
            toClassification.apply {
                value = fromClassification.value
                type = fromClassification.type
                code = fromClassification.code
            }

        private fun copyValues(fromIdentifier: IdentifierDb, toIdentifier: IdentifierDb) =
            toIdentifier.apply {
                type = fromIdentifier.type
                value = fromIdentifier.value
                issuingBody = fromIdentifier.issuingBody
            }

        private fun copyValues(fromPostalAddress: PostalAddressDb, toPostalAddress: PostalAddressDb) =
            toPostalAddress.apply {
                addressType = fromPostalAddress.addressType
                physicalPostalAddress = fromPostalAddress.physicalPostalAddress
                alternativePostalAddress = fromPostalAddress.alternativePostalAddress
            }


    }

    data class ResolutionResult(
        val businessPartner: BusinessPartnerDb,
        val wasResolved: Boolean
    )
}
