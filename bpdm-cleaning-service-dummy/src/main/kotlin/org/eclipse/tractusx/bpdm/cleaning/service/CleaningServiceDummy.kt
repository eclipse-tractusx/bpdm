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
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class CleaningServiceDummy(
    private val orchestrationApiClient: OrchestrationApiClient,

    ) {

    private val logger = KotlinLogging.logger { }

    val dummyConfidenceCriteria = ConfidenceCriteria(
        sharedByOwner = false,
        numberOfSharingMembers = 1,
        checkedByExternalDataSource = false,
        lastConfidenceCheckAt = Instant.now(),
        nextConfidenceCheckAt = Instant.now().plus (5, ChronoUnit.DAYS),
        confidenceLevel = 0
    )

    @Scheduled(cron = "\${bpdm.cleaningService.pollingCron:-}", zone = "UTC")
    fun pollForCleanAndSyncTasks() {
        processPollingTasks(TaskStep.CleanAndSync)
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

    fun processCleaningTask(reservedTask: TaskStepReservationEntryDto): TaskStepResultEntryDto {
        val businessPartner = reservedTask.businessPartner

        val cleanedBusinessPartner = BusinessPartner(
            nameParts = businessPartner.nameParts,
            uncategorized = businessPartner.uncategorized,
            owningCompany = businessPartner.owningCompany,
            legalEntity = cleanLegalEntity(businessPartner),
            site = cleanSite(businessPartner),
            additionalAddress =  cleanAdditionalAddress(businessPartner)
        )

        return TaskStepResultEntryDto(reservedTask.taskId, cleanedBusinessPartner)
    }

    private fun cleanLegalEntity(businessPartner: BusinessPartner): LegalEntity {
        val addressToClean = businessPartner.legalEntity.legalAddress.takeIf { it != PostalAddress.empty }
            ?: businessPartner.uncategorized.address
            ?: businessPartner.site?.siteMainAddress
            ?: businessPartner.additionalAddress
            ?: PostalAddress.empty

        return with(businessPartner.legalEntity){
                copy(
                    bpnReference = bpnReference.toRequestIfNotBpn(businessPartner.createLegalEntityReferenceValue()),
                    legalName = legalName ?: businessPartner.uncategorized.nameParts.joinToString(""),
                    identifiers = identifiers.takeIf { it.isNotEmpty() } ?: businessPartner.uncategorized.identifiers,
                    states = states.takeIf { it.isNotEmpty() } ?: businessPartner.uncategorized.states,
                    confidenceCriteria = dummyConfidenceCriteria,
                    hasChanged = businessPartner.type == GoldenRecordType.LegalEntity,
                    legalAddress = cleanAddress(addressToClean, businessPartner.createLegalAddressReferenceValue(), true),
                )
            }
    }

    private fun cleanSite(businessPartner: BusinessPartner): Site?{
        return businessPartner.site?.let { site ->

            val addressToClean = if(site.siteMainIsLegalAddress){
                null
            }else {
                site.siteMainAddress?.takeIf { it != PostalAddress.empty }
                    ?: businessPartner.uncategorized.address
                    ?: businessPartner.additionalAddress
                    ?: PostalAddress.empty
            }

            with(site){
                copy(
                    bpnReference = bpnReference.toRequestIfNotBpn(businessPartner.createSiteReferenceValue()),
                    confidenceCriteria = dummyConfidenceCriteria,
                    hasChanged = businessPartner.type == GoldenRecordType.Site,
                    siteMainAddress = addressToClean?.let { cleanAddress(addressToClean, businessPartner.createSiteMainAddressReferenceValue(), true) }
                )
            }
        }
    }

    private fun cleanAdditionalAddress(businessPartner: BusinessPartner): PostalAddress? {
        return businessPartner.additionalAddress?.let {
            cleanAddress(
                it,
                businessPartner.createAdditionalAddressReferenceValue(),
                businessPartner.type == GoldenRecordType.Address)
        }
    }


    private fun cleanAddress(addressToClean: PostalAddress, requestId: String, hasChanged: Boolean): PostalAddress {
        return addressToClean.copy(
                bpnReference =  addressToClean.bpnReference.toRequestIfNotBpn(requestId),
                hasChanged = hasChanged,
                confidenceCriteria = dummyConfidenceCriteria
            )
    }

    private fun BusinessPartner.createLegalEntityReferenceValue() =
        "LEGAL_ENTITY" + (legalEntity.legalName ?: nameParts.joinToString(" "))

    private fun BusinessPartner.createSiteReferenceValue() =
        "SITE" + createLegalEntityReferenceValue() + (site?.siteName ?: "")

    private fun BusinessPartner.createSiteMainAddressReferenceValue() =
        "SITE_MAIN_ADDRESS" + createSiteReferenceValue()

    private fun BusinessPartner.createLegalAddressReferenceValue() =
        "LEGAL_ADDRESS" + createLegalEntityReferenceValue()


    private fun BusinessPartner.createAdditionalAddressReferenceValue() =
        "ADDITIONAL_ADDRESS" + (additionalAddress?.addressName ?: "") +  createSiteReferenceValue()

    private fun generateBpnRequestIdentifier(fromString: String) =
        BpnReference(fromString.toUUID().toString(), null, BpnReferenceType.BpnRequestIdentifier)


    private fun BpnReference.toRequestIfNotBpn(requestId: String) =
        if(referenceType == BpnReferenceType.Bpn)
            this
        else
            generateBpnRequestIdentifier(requestId)

}
