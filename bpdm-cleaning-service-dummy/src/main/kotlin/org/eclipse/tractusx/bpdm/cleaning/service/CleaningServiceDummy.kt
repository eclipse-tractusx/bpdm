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
import org.eclipse.tractusx.bpdm.cleaning.config.CleaningServiceConfigProperties
import org.eclipse.tractusx.bpdm.cleaning.util.toUUID
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class CleaningServiceDummy(
    private val orchestrationApiClient: OrchestrationApiClient,
    private val cleaningServiceConfigProperties: CleaningServiceConfigProperties
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
        processPollingTasks(cleaningServiceConfigProperties.step)
    }


    private fun processPollingTasks(step: TaskStep) {
        try {
            logger.info { "Starting polling for cleaning tasks from Orchestrator... TaskStep ${step.name}" }

            do{
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
            }while (cleaningRequest.reservedTasks.isNotEmpty())
        } catch (e: Exception) {
            logger.error(e) { "Error while processing cleaning task" }
        }
    }

    fun processCleaningTask(reservedTask: TaskStepReservationEntryDto): TaskStepResultEntryDto {
        val businessPartner = reservedTask.businessPartner
        val sharedByOwner = businessPartner.owningCompany?.isNotBlank() ?: false

        val cleanedBusinessPartner = BusinessPartner(
            nameParts = businessPartner.nameParts,
            // Identifiers are taken as legal identifiers and should be removed from uncategorized collection
            uncategorized = businessPartner.uncategorized.copy(identifiers = emptyList()),
            owningCompany = businessPartner.owningCompany,
            legalEntity = cleanLegalEntity(businessPartner, sharedByOwner),
            site = cleanSite(businessPartner, sharedByOwner),
            additionalAddress =  cleanAdditionalAddress(businessPartner, sharedByOwner)
        )

        return TaskStepResultEntryDto(reservedTask.taskId, cleanedBusinessPartner)
    }

    private fun cleanLegalEntity(businessPartner: BusinessPartner, sharedByOwner: Boolean): LegalEntity {
        val addressToClean = businessPartner.legalEntity.legalAddress.takeIf { it != PostalAddress.empty }
            ?: businessPartner.uncategorized.address
            ?: businessPartner.site?.siteMainAddress
            ?: businessPartner.additionalAddress
            ?: PostalAddress.empty

        val legalAddressBpnReference =  addressToClean.bpnReference
            .toRequestIfNotBpn(businessPartner.legalEntityReference(), businessPartner.siteReference(), businessPartner.legalAddressReference())

        return with(businessPartner.legalEntity){
                copy(
                    bpnReference = bpnReference.toRequestIfNotBpn(businessPartner.legalEntityReference()),
                    legalName = legalName ?: businessPartner.uncategorized.nameParts.takeIf { it.isNotEmpty() }?.joinToString(" "),
                    identifiers = identifiers.takeIf { it.isNotEmpty() } ?: businessPartner.uncategorized.identifiers,
                    states = states,
                    confidenceCriteria = dummyConfidenceCriteria.copy(sharedByOwner = sharedByOwner),
                    hasChanged = businessPartner.type == GoldenRecordType.LegalEntity,
                    isCatenaXMemberData = sharedByOwner,
                    legalAddress = cleanAddress(addressToClean, legalAddressBpnReference, true, sharedByOwner),
                )
            }
    }

    private fun cleanSite(businessPartner: BusinessPartner, sharedByOwner: Boolean): Site?{
        return businessPartner.site?.let { site ->

            val addressToClean = if(site.siteMainIsLegalAddress){
                null
            }else {
                site.siteMainAddress?.takeIf { it != PostalAddress.empty }
                    ?: businessPartner.uncategorized.address
                    ?: businessPartner.additionalAddress
                    ?: PostalAddress.empty
            }

            val siteMainBpnReference = addressToClean?.bpnReference
                ?.toRequestIfNotBpn(businessPartner.legalAddressReference(), businessPartner.siteReference(), businessPartner.siteMainAddressReference())

            with(site){
                copy(
                    bpnReference = bpnReference.toRequestIfNotBpn(businessPartner.legalEntityReference(), businessPartner.siteReference()),
                    confidenceCriteria = dummyConfidenceCriteria.copy(sharedByOwner = sharedByOwner),
                    hasChanged = businessPartner.type == GoldenRecordType.Site,
                    siteMainAddress = addressToClean?.let { cleanAddress(addressToClean, siteMainBpnReference!!, true, sharedByOwner) }
                )
            }
        }
    }

    private fun cleanAdditionalAddress(businessPartner: BusinessPartner, sharedByOwner: Boolean): PostalAddress? {
        return businessPartner.additionalAddress?.let {
            cleanAddress(
                it,
                it.bpnReference.toRequestIfNotBpn(businessPartner.legalEntityReference(), businessPartner.siteReference(), businessPartner.addressReference()),
                businessPartner.type == GoldenRecordType.Address,
                sharedByOwner
            )
        }
    }


    private fun cleanAddress(addressToClean: PostalAddress, bpnReference: BpnReference, hasChanged: Boolean, sharedByOwner: Boolean): PostalAddress {
        return addressToClean.copy(
                bpnReference =  bpnReference,
                hasChanged = hasChanged,
                confidenceCriteria = dummyConfidenceCriteria.copy(sharedByOwner = sharedByOwner)
            )
    }

    private fun BusinessPartner.legalEntityReference() =
        "LEGAL_ENTITY${legalEntity.legalName ?: namePartsName()}".toUUID()

    private fun BusinessPartner.siteReference() = "S_${site?.siteName ?: namePartsName()}".toUUID()

    private fun BusinessPartner.addressReference() = "A_${additionalAddress?.addressName ?: namePartsName()}".toUUID()

    private fun BusinessPartner.namePartsName() = uncategorized.nameParts.joinToString(" ")

    private fun BusinessPartner.legalAddressReference() = "LEGAL_ADDRESS".toUUID()

    private fun BusinessPartner.siteMainAddressReference() = "SITE_MAIN_ADDRESS".toUUID()


    private fun generateBpnRequestIdentifier(legalEntityReference: UUID, siteReference: UUID? = null, addressReference: UUID? = null): BpnReference{
        val siteReferenceString = siteReference?.let { "${siteReference}_" } ?: ""
        val addressReferenceString = addressReference?.let { "${addressReference}_" } ?: ""
        return BpnReference( addressReferenceString + siteReferenceString + legalEntityReference.toString(), null, BpnReferenceType.BpnRequestIdentifier)
    }



    private fun BpnReference.toRequestIfNotBpn(legalEntityReference: UUID, siteReference: UUID? = null, additionalAddressReference: UUID? = null) =
        if(referenceType == BpnReferenceType.Bpn)
            this
        else
            generateBpnRequestIdentifier(legalEntityReference, siteReference, additionalAddressReference)

}
