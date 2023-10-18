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

package org.eclipse.tractusx.bpdm.cleaning.service


import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*

@Service
class CleaningServiceDummy(
    private val orchestrationApiClient: OrchestrationApiClient,

    ) {

    private val logger = KotlinLogging.logger { }


    @Scheduled(cron = "\${cleaningService.pollingCron:-}", zone = "UTC")
    fun pollForCleaningTasks() {
        try {
            logger.info { "Starting polling for cleaning tasks from Orchestrator..." }
            val step = TaskStep.CleanAndSync

            // Step 1: Fetch and reserve the next cleaning request
            val cleaningRequest = orchestrationApiClient.goldenRecordTasks
                .reserveTasksForStep(TaskStepReservationRequest(amount = 10, step))

            val cleaningTasks = cleaningRequest.reservedTasks

            logger.info { "${cleaningTasks.size} tasks found for cleaning. Proceeding with cleaning..." }

            if (cleaningTasks.isNotEmpty()) {
                val cleaningResults = cleaningTasks.map { reservedTask ->
                    // Step 2: Generate dummy cleaning results
                    processCleaningTask(reservedTask)
                }

                // Step 3: Send the cleaning result back to the Orchestrator
                orchestrationApiClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest(step, cleaningResults))
                logger.info { "Cleaning tasks processing completed for this iteration." }
            }

        } catch (e: Exception) {
            logger.error(e) { "Error while processing cleaning task" }
        }
    }

    fun processCleaningTask(reservedTask: TaskStepReservationEntryDto): TaskStepResultEntryDto {
        val genericBusinessPartner = reservedTask.businessPartner.generic

        val addressPartner = createAddressRepresentation(genericBusinessPartner)

        val addressType = genericBusinessPartner.postalAddress.addressType ?: AddressType.AdditionalAddress

        val legalEntityDto = createLegalEntityRepresentation(addressPartner, addressType, genericBusinessPartner)

        val siteDto = createSiteDtoIfNeeded(genericBusinessPartner, addressPartner)

        val addressDto = shouldCreateAddress(addressType, addressPartner)

        return TaskStepResultEntryDto(reservedTask.taskId, BusinessPartnerFullDto(genericBusinessPartner, legalEntityDto, siteDto, addressDto))
    }

    private fun shouldCreateAddress(
        addressType: AddressType,
        addressPartner: LogisticAddressDto
    ): LogisticAddressDto? {
        val addressDto = if (addressType == AddressType.AdditionalAddress) {
            addressPartner
        } else {
            null
        }
        return addressDto
    }

    fun createSiteDtoIfNeeded(businessPartner: BusinessPartnerGenericDto, addressPartner: LogisticAddressDto): SiteDto? {
        if (!shouldCreateSite(businessPartner)) return null

        val siteAddressReference = when (businessPartner.postalAddress.addressType) {
            AddressType.SiteMainAddress, AddressType.LegalAndSiteMainAddress -> addressPartner.bpnAReference
            else -> generateNewBpnRequestIdentifier()
        }

        val siteMainAddress = addressPartner.copy(bpnAReference = siteAddressReference)
        return createSiteRepresentation(businessPartner, siteMainAddress)
    }

    fun createLegalEntityRepresentation(
        addressPartner: LogisticAddressDto,
        addressType: AddressType,
        genericPartner: BusinessPartnerGenericDto
    ): LegalEntityDto {
        val legalAddressBpnReference = if (addressType == AddressType.LegalAddress || addressType == AddressType.LegalAndSiteMainAddress) {
            addressPartner.bpnAReference
        } else {
            generateNewBpnRequestIdentifier()
        }

        val legalAddress = addressPartner.copy(bpnAReference = legalAddressBpnReference)

        val bpnReferenceDto = createBpnReference(genericPartner.bpnL)

        return genericPartner.toLegalEntityDto(bpnReferenceDto, legalAddress)

    }

    fun createAddressRepresentation(genericPartner: BusinessPartnerGenericDto): LogisticAddressDto {
        val bpnReferenceDto = createBpnReference(genericPartner.bpnA)
        return genericPartner.toLogisticAddressDto(bpnReferenceDto)
    }

    fun createSiteRepresentation(genericPartner: BusinessPartnerGenericDto, siteAddressReference: LogisticAddressDto): SiteDto {
        val legalName = genericPartner.nameParts.joinToString(" ")
        val bpnReferenceDto = createBpnReference(genericPartner.bpnS)
        return genericPartner.toSiteDto(bpnReferenceDto, legalName, siteAddressReference)
    }

    fun createBpnReference(bpn: String?): BpnReferenceDto {
        return if (bpn != null) {
            BpnReferenceDto(bpn, BpnReferenceType.Bpn)
        } else {
            // Generate a new UUID and create a BpnReferenceDto object if bpnL/bpnS/bpnA is null
            generateNewBpnRequestIdentifier()
        }
    }

    private fun generateNewBpnRequestIdentifier() = BpnReferenceDto(UUID.randomUUID().toString(), BpnReferenceType.BpnRequestIdentifier)

    fun shouldCreateSite(genericPartner: BusinessPartnerGenericDto): Boolean {
        return genericPartner.postalAddress.addressType == AddressType.SiteMainAddress ||
                genericPartner.postalAddress.addressType == AddressType.LegalAndSiteMainAddress ||
                genericPartner.bpnS != null
    }


}
