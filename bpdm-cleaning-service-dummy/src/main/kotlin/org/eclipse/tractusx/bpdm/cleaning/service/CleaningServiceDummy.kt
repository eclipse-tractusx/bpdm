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

package org.eclipse.tractusx.bpdm.cleaning.service


import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.cleaning.util.toUUID
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class CleaningServiceDummy(
    private val orchestrationApiClient: OrchestrationApiClient,

    ) {

    private val logger = KotlinLogging.logger { }

    @Scheduled(cron = "\${bpdm.cleaningService.pollingCron:-}", zone = "UTC")
    fun pollForCleaningTasks() {
        processPollingTasks(TaskStep.CleanAndSync)
        processPollingTasks(TaskStep.Clean)
    }


    private fun processPollingTasks(step: TaskStep) {
        try {
            logger.info { "Starting polling for cleaning tasks from Orchestrator... TaskStep ${step.name}" }

            val cleaningRequest = orchestrationApiClient.goldenRecordTasks
                .reserveTasksForStep(TaskStepReservationRequest(amount = 10, step))

            val cleaningTasks = cleaningRequest.reservedTasks

            logger.info { "${cleaningTasks.size} tasks found for cleaning. Proceeding with cleaning..." }

            if (cleaningTasks.isNotEmpty()) {
                val cleaningResults = cleaningTasks.map { reservedTask ->
                    processCleaningTask(reservedTask)
                }

                orchestrationApiClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest(step, cleaningResults))
                logger.info { "Cleaning tasks processing completed for this iteration." }
            }

        } catch (e: Exception) {
            logger.error(e) { "Error while processing cleaning task" }
        }
    }

    fun processCleaningTask(reservedTask: TaskStepReservationEntry): TaskStepResultEntry {
        val genericBusinessPartner = reservedTask.businessPartner.generic

        val addressPartner = createAddressRepresentation(genericBusinessPartner)

        val addressType = genericBusinessPartner.address.addressType ?: AddressType.AdditionalAddress

        val legalEntityDto = createLegalEntityRepresentation(addressPartner, addressType, genericBusinessPartner)

        val siteDto = createSiteDtoIfNeeded(genericBusinessPartner, addressPartner)

        val addressDto = shouldCreateAddress(addressType, addressPartner)

        val updatedGenericBusinessPartner = genericBusinessPartner.update(addressType, legalEntityDto, siteDto, addressDto)

        return TaskStepResultEntry(reservedTask.taskId, BusinessPartnerFull(updatedGenericBusinessPartner, legalEntityDto, siteDto, addressDto))
    }

    private fun shouldCreateAddress(
        addressType: AddressType,
        addressPartner: LogisticAddress
    ): LogisticAddress? {
        val addressDto = if (addressType == AddressType.AdditionalAddress) {
            addressPartner
        } else {
            null
        }
        return addressDto
    }

    fun createSiteDtoIfNeeded(businessPartner: BusinessPartnerGeneric, addressPartner: LogisticAddress): Site? {
        if (!shouldCreateSite(businessPartner)) return null

        val siteMainAddress = addressPartner.copy(bpnAReference = generateBpnRequestIdentifier(businessPartner.createSiteMainAddressReferenceValue()))
        return createSiteRepresentation(businessPartner, siteMainAddress)
    }

    fun createLegalEntityRepresentation(
        addressPartner: LogisticAddress,
        addressType: AddressType,
        genericPartner: BusinessPartnerGeneric
    ): LegalEntity {
        val legalAddressBpnReference = generateBpnRequestIdentifier(genericPartner.createLegalAddressReferenceValue())
        val legalAddress = addressPartner.copy(bpnAReference = legalAddressBpnReference)

        val legalEntityBpnReference = generateBpnRequestIdentifier(genericPartner.createLegalEntityReferenceValue())
        return genericPartner.toLegalEntityDto(legalEntityBpnReference, legalAddress)

    }

    fun createAddressRepresentation(genericPartner: BusinessPartnerGeneric): LogisticAddress {
        val bpnReferenceDto = generateBpnRequestIdentifier(genericPartner.createAdditionalAddressReferenceValue())
        return genericPartner.toLogisticAddressDto(bpnReferenceDto)
    }

    fun createSiteRepresentation(genericPartner: BusinessPartnerGeneric, siteAddressReference: LogisticAddress): Site {
        val bpnReferenceDto = generateBpnRequestIdentifier(genericPartner.createSiteReferenceValue())
        return genericPartner.toSiteDto(bpnReferenceDto, siteAddressReference)
    }

    fun shouldCreateSite(genericPartner: BusinessPartnerGeneric): Boolean {
        return genericPartner.ownerBpnL != null && genericPartner.site.name != null
    }

    private fun BusinessPartnerGeneric.update(
        addressType: AddressType,
        legalEntityDto: LegalEntity,
        siteDto: Site?,
        logisticAddress: LogisticAddress?
    ): BusinessPartnerGeneric {
        val relevantAddress = when (addressType) {
            AddressType.LegalAndSiteMainAddress -> legalEntityDto.legalAddress!!
            AddressType.LegalAddress -> legalEntityDto.legalAddress!!
            AddressType.SiteMainAddress -> siteDto!!.mainAddress!!
            AddressType.AdditionalAddress -> logisticAddress!!
        }

        return copy(
            legalEntity = legalEntity.copy(legalName = legalEntityDto.legalName, confidenceCriteria = legalEntityDto.confidenceCriteria),
            site = site.copy(name = siteDto?.name, confidenceCriteria = siteDto?.confidenceCriteria),
            address = address.copy(name = logisticAddress?.name, addressType = addressType, confidenceCriteria = relevantAddress.confidenceCriteria)
        )
    }

    private fun BusinessPartnerGeneric.createLegalEntityReferenceValue() =
        "LEGAL_ENTITY" + (legalEntity.legalName ?: nameParts.joinToString(" "))

    private fun BusinessPartnerGeneric.createLegalAddressReferenceValue() =
        "LEGAL_ADDRESS" + createLegalEntityReferenceValue()

    private fun BusinessPartnerGeneric.createSiteReferenceValue() =
        "SITE" + createLegalEntityReferenceValue() + site.name

    private fun BusinessPartnerGeneric.createSiteMainAddressReferenceValue() =
        if (address.addressType == AddressType.LegalAndSiteMainAddress)
            createLegalAddressReferenceValue()
        else
            "SITE_MAIN_ADDRESS" + createSiteReferenceValue()

    private fun BusinessPartnerGeneric.createAdditionalAddressReferenceValue() =
        "ADDITIONAL_ADDRESS" + createSiteReferenceValue()

    private fun generateBpnRequestIdentifier(fromString: String) =
        BpnReference(fromString.toUUID().toString(), BpnReferenceType.BpnRequestIdentifier)


}
