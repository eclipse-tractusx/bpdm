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

    @Scheduled(fixedRate = 60000) // Adjust the rate as needed
    fun pollForCleaningTasks() {
        try {
            logger.info { "Starting polling for cleaning tasks from Orchestrator..." }

            // Step 1: Fetch and reserve the next cleaning request
            val cleaningRequest = orchestrationApiClient.goldenRecordTasks
                .reserveTasksForStep(TaskStepReservationRequest(amount = 10, TaskStep.Clean))

            val cleaningTasks = cleaningRequest.reservedTasks

            if (cleaningTasks.isNotEmpty()) {
                logger.info { "${cleaningTasks.size} tasks found for cleaning. Proceeding with cleaning..." }

                val cleaningResults = cleaningTasks.mapNotNull { reservedTask ->
                    // Step 2: Generate dummy cleaning results
                    processCleaningTask(reservedTask)
                }

                // Step 3: Send the cleaning result back to the Orchestrator
                orchestrationApiClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest(cleaningResults))
                logger.info { "Cleaning tasks processing completed for this iteration." }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error while processing cleaning task" }
        }
    }

    fun processCleaningTask(reservedTask: TaskStepReservationEntryDto): TaskStepResultEntryDto {
        val businessPartner = reservedTask.businessPartner
        val addressPartner = createAddressRepresentation(businessPartner.generic)

        val legalAddressBpnReference = when (businessPartner.generic.postalAddress.addressType) {
            AddressType.LegalAddress, AddressType.LegalAndSiteMainAddress -> addressPartner.bpnAReference
            else -> generateNewBpnReference()
        }

        val legalAddress = addressPartner.copy(bpnAReference = legalAddressBpnReference)
        val legalEntityDto = createLegalEntityRepresentation(businessPartner.generic, legalAddress)

        val siteDto = when {
            shouldCreateSite(businessPartner.generic) -> {
                val siteAddressReference = when (businessPartner.generic.postalAddress.addressType) {
                    AddressType.SiteMainAddress, AddressType.LegalAndSiteMainAddress -> addressPartner.bpnAReference
                    else -> generateNewBpnReference()
                }
                val siteMainAddress = addressPartner.copy(bpnAReference = siteAddressReference)
                createSiteRepresentation(businessPartner.generic, siteMainAddress)
            }

            else -> null
        }

        return TaskStepResultEntryDto(reservedTask.taskId, BusinessPartnerFullDto(businessPartner.generic, legalEntityDto, siteDto, addressPartner))
    }

    fun createLegalEntityRepresentation(genericPartner: BusinessPartnerGenericDto, legalAddress: LogisticAddressDto): LegalEntityDto {
        val legalName = genericPartner.nameParts.joinToString(" ")
        val bpnReferenceDto = validateBpn(genericPartner.bpnL)
        return genericPartner.toLegalEntityDto(bpnReferenceDto, legalName, legalAddress)

    }

    fun createAddressRepresentation(genericPartner: BusinessPartnerGenericDto): LogisticAddressDto {
        val legalName = genericPartner.nameParts.joinToString(" ")
        val bpnReferenceDto = validateBpn(genericPartner.bpnA)
        return genericPartner.postalAddress.toLogisticAddressDto(bpnReferenceDto, legalName)
    }

    fun createSiteRepresentation(genericPartner: BusinessPartnerGenericDto, siteAddressReference: LogisticAddressDto): SiteDto {
        val legalName = genericPartner.nameParts.joinToString(" ")
        val bpnReferenceDto = validateBpn(genericPartner.bpnS)
        return genericPartner.toSiteDto(bpnReferenceDto, legalName, siteAddressReference)
    }

    fun validateBpn(bpn: String?): BpnReferenceDto {
        return if (bpn != null) {
            BpnReferenceDto(bpn, BpnReferenceType.Bpn)
        } else {
            // Generate a new UUID and create a BpnReferenceDto object if bpnL/bpnS/bpnA is null
            generateNewBpnReference()
        }
    }

    private fun generateNewBpnReference() = BpnReferenceDto(UUID.randomUUID().toString(), BpnReferenceType.BpnRequestIdentifier)

    fun shouldCreateSite(genericPartner: BusinessPartnerGenericDto): Boolean {
        return genericPartner.postalAddress.addressType == AddressType.SiteMainAddress ||
                genericPartner.postalAddress.addressType == AddressType.LegalAndSiteMainAddress ||
                genericPartner.bpnS != null
    }


}
