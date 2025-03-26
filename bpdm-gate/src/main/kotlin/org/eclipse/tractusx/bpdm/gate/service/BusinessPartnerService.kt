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
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntryDb
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.exception.BpdmInvalidPartnerException
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.OutputUpsertData
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.gate.util.BusinessPartnerComparisonUtil
import org.eclipse.tractusx.bpdm.gate.util.BusinessPartnerCopyUtil
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BusinessPartnerService(
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val businessPartnerMappings: BusinessPartnerMappings,
    private val sharingStateService: SharingStateService,
    private val changelogRepository: ChangelogRepository,
    private val copyUtil: BusinessPartnerCopyUtil,
    private val compareUtil: BusinessPartnerComparisonUtil,
    private val outputUpsertMappings: OutputUpsertMappings
) {
    private val logger = KotlinLogging.logger { }


    fun getBusinessPartnersInput(pageRequest: PageRequest, externalIds: Collection<String>?, tenantBpnl: String?): PageDto<BusinessPartnerInputDto> {
        logger.debug { "Executing getBusinessPartnersInput() with parameters $pageRequest and $externalIds" }

        return getBusinessPartners(pageRequest, externalIds, StageType.Input, tenantBpnl).toPageDto(businessPartnerMappings::toBusinessPartnerInputDto)
    }

    fun getBusinessPartnersOutput(pageRequest: PageRequest, externalIds: Collection<String>?, tenantBpnl: String?): PageDto<BusinessPartnerOutputDto> {
        logger.debug { "Executing getBusinessPartnersOutput() with parameters $pageRequest and $externalIds" }

        return getBusinessPartners(pageRequest, externalIds, StageType.Output, tenantBpnl).toPageDto(businessPartnerMappings::toBusinessPartnerOutputDto)
    }

    @Transactional
    fun upsertBusinessPartnersInput(requests: List<BusinessPartnerInputRequest>, tenantBpnl: String?): List<BusinessPartnerInputDto> {
        logger.debug { "Executing upsertBusinessPartnersInput() with parameters $requests" }

        val sharingStates = sharingStateService.getOrCreateStates(requests.map { it.externalId }, tenantBpnl)
        val sharingStatesByExternalId = sharingStates.associateBy { it.externalId }
        val existingInputs = businessPartnerRepository.findBySharingStateInAndStage(sharingStates, StageType.Input)
        val existingInputsByExternalId = existingInputs.associateBy { it.sharingState.externalId }

        val updatedEntities = requests.mapNotNull { request ->
            val sharingState = sharingStatesByExternalId[request.externalId]!!
            val updatedData = businessPartnerMappings.toBusinessPartnerInput(request, sharingState)
            val existingInput = existingInputsByExternalId[request.externalId]


            upsertFromEntity(existingInput, updatedData)
                .takeIf {  it.shouldUpdate && (it.hadChanges || sharingState.sharingStateType == SharingStateType.Error) }
                ?.also { sharingStateService.setInitial(sharingState) }
                ?.businessPartner
        }

        return updatedEntities.map(businessPartnerMappings::toBusinessPartnerInputDto)
    }

    @Transactional
    fun upsertBusinessPartnersOutput(requests: List<OutputUpsertRequest>): List<UpsertResult> {
        logger.debug { "Executing upsertBusinessPartnersOutput() with parameters $requests" }

        val existingOutputs = businessPartnerRepository.findBySharingStateInAndStage(requests.map { it.sharingState }, StageType.Output)
        val existingOutputsBySharingStateId = existingOutputs.associateBy { it.sharingState.id }

        val updatedEntities = requests.map { request ->
            val existingOutput = existingOutputsBySharingStateId[request.sharingState.id]
            val updatedData = outputUpsertMappings.toEntity(request.upsertData, request.sharingState)

            upsertFromEntity(existingOutput, updatedData)
        }

        return updatedEntities
    }

    @Transactional
    fun updateBusinessPartnerOutput(businessPartner: BusinessPartnerDb, upsertData: OutputUpsertData): UpsertResult {
        logger.debug { "Executing updateBusinessPartnerOutput() with parameters $businessPartner and $upsertData" }

        if (businessPartner.stage != StageType.Output)
            throw BpdmInvalidPartnerException(businessPartner.id.toString(), "Needs to be in Output stage")

        val updatedData = outputUpsertMappings.toEntity(upsertData, businessPartner.sharingState)
        return upsertFromEntity(businessPartner, updatedData)
    }


    private fun upsertFromEntity(existingPartner: BusinessPartnerDb?, upsertData: BusinessPartnerDb): UpsertResult {
        val sharingState = upsertData.sharingState
        val stage = upsertData.stage
        val changeType = if (existingPartner == null) ChangelogType.CREATE else ChangelogType.UPDATE
        val partnerToUpsert = existingPartner ?: BusinessPartnerDb.createEmpty(upsertData.sharingState, upsertData.stage)

        val hasChanges = changeType == ChangelogType.CREATE || compareUtil.hasChanges(upsertData, partnerToUpsert)
        val shouldUpdate = when {
            upsertData.externalSequenceTimestamp == null -> true
            existingPartner?.externalSequenceTimestamp == null -> true
            else -> upsertData.externalSequenceTimestamp!!.isAfter(existingPartner.externalSequenceTimestamp)
        }

        if (hasChanges && shouldUpdate) {
                changelogRepository.save(ChangelogEntryDb(sharingState.externalId, sharingState.tenantBpnl, changeType, stage))

                copyUtil.copyValues(upsertData, partnerToUpsert)
                businessPartnerRepository.save(partnerToUpsert)
        }

        return UpsertResult(hasChanges, shouldUpdate, changeType, partnerToUpsert)
    }

    private fun getBusinessPartners(
        pageRequest: PageRequest,
        externalIds: Collection<String>?,
        stage: StageType,
        tenantBpnl: String?
    ): Page<BusinessPartnerDb> {
        val spec = Specification.allOf(
            BusinessPartnerRepository.Specs.byExternalIdsIn(externalIds),
            BusinessPartnerRepository.Specs.byTenantBpnl(tenantBpnl),
            BusinessPartnerRepository.Specs.byStage(stage)
        )

        return businessPartnerRepository.findAll(spec, pageRequest)
    }

    data class UpsertResult(
        val hadChanges: Boolean,
        val shouldUpdate: Boolean,
        val type: ChangelogType,
        val businessPartner: BusinessPartnerDb
    )

    data class OutputUpsertRequest(
        val sharingState: SharingStateDb,
        val upsertData: OutputUpsertData
    )
}
