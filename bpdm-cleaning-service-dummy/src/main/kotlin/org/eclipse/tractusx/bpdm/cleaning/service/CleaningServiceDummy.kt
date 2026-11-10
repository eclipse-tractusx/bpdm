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

package org.eclipse.tractusx.bpdm.cleaning.service


import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.cleaning.config.CleaningServiceConfigProperties
import org.eclipse.tractusx.bpdm.cleaning.util.toUUID
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.client.OrchestratorV7Client
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AdditionalSiteV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartnerV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.GoldenRecordTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.LegalEntityV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressWithScriptVariantsV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.SiteV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepResultEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepResultV7Request
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class CleaningServiceDummy(
    private val orchestrationApiClient: OrchestratorV7Client,
    private val cleaningServiceConfigProperties: CleaningServiceConfigProperties
    ) {

    private val logger = KotlinLogging.logger { }

    val dummyConfidenceCriteria = ConfidenceCriteriaV7Dto(
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


    private fun processPollingTasks(step: TaskStepV7) {
        try {
            logger.debug { "Starting polling for cleaning tasks from Orchestrator... TaskStep ${step.name}" }

            do{
                val cleaningRequest = orchestrationApiClient.goldenRecordTasks
                    .reserveTasksForStep(TaskStepReservationV7Request(amount = 10, step))

                val cleaningTasks = cleaningRequest.reservedTasks

                logger.debug { "${cleaningTasks.size} tasks found for cleaning. Proceeding with cleaning..." }

                if (cleaningTasks.isNotEmpty()) {
                    val cleaningResults = cleaningTasks.map { reservedTask ->
                        processCleaningTask(reservedTask)
                    }

                    orchestrationApiClient.goldenRecordTasks.resolveStepResults(TaskStepResultV7Request(step, cleaningResults))
                    logger.debug { "Cleaning tasks processing completed for this iteration." }
                }
            }while (cleaningRequest.reservedTasks.isNotEmpty())
        } catch (e: Exception) {
            logger.error(e) { "Error while processing cleaning task" }
        }
    }

    fun processCleaningTask(reservedTask: TaskStepReservationEntryV7Dto): TaskStepResultEntryV7Dto {
        val businessPartner = reservedTask.businessPartner
        val sharedByOwner = businessPartner.owningCompany?.isNotBlank() ?: false

        val cleanedBusinessPartner = BusinessPartnerV7Dto(
            nameParts = businessPartner.nameParts,
            // Identifiers are taken as legal identifiers and should be removed from uncategorized collection
            uncategorized = businessPartner.uncategorized.copy(identifiers = emptyList()),
            owningCompany = businessPartner.owningCompany,
            legalEntity = cleanLegalEntity(businessPartner, sharedByOwner),
            site = cleanSite(businessPartner, sharedByOwner),
            additionalAddress =  cleanAdditionalAddress(businessPartner, sharedByOwner),
            additionalSites = cleanAdditionalSites(businessPartner)
        )

        return TaskStepResultEntryV7Dto(reservedTask.taskId, cleanedBusinessPartner)
    }

    private fun cleanLegalEntity(businessPartner: BusinessPartnerV7Dto, sharedByOwner: Boolean): LegalEntityV7Dto {
        val addressToClean = businessPartner.legalEntity.legalAddress.takeIf { it != PostalAddressV7Dto.empty }
            ?: businessPartner.uncategorized.address?.postalProperties
            ?: businessPartner.site?.siteMainAddress
            ?: businessPartner.additionalAddress?.postalProperties
            ?: PostalAddressV7Dto.empty

        val legalAddressBpnReference =  addressToClean.bpnReference
            .toRequestIfNotBpn(businessPartner.legalEntityReference(), businessPartner.siteReference(), businessPartner.legalAddressReference())

        return with(businessPartner.legalEntity){
                copy(
                    bpnReference = bpnReference.toRequestIfNotBpn(businessPartner.legalEntityReference()),
                    legalName = legalName ?: businessPartner.uncategorized.nameParts.takeIf { it.isNotEmpty() }?.joinToString(" "),
                    identifiers = identifiers.takeIf { it.isNotEmpty() } ?: businessPartner.uncategorized.identifiers,
                    states = states,
                    confidenceCriteria = dummyConfidenceCriteria.copy(sharedByOwner = sharedByOwner),
                    hasChanged = (businessPartner.type == GoldenRecordTypeV7.LegalEntity) || (businessPartner.type == GoldenRecordTypeV7.Site && businessPartner.site!!.siteMainIsLegalAddress),
                    isParticipantData = sharedByOwner,
                    legalAddress = cleanAddress(addressToClean, legalAddressBpnReference, true, sharedByOwner),
                )
            }
    }

    private fun cleanSite(businessPartner: BusinessPartnerV7Dto, sharedByOwner: Boolean): SiteV7Dto?{
        return businessPartner.site?.let { site ->

            val addressToClean = if(site.siteMainIsLegalAddress){
                null
            }else {
                site.siteMainAddress?.takeIf { it != PostalAddressV7Dto.empty }
                    ?: businessPartner.uncategorized.address?.postalProperties
                    ?: businessPartner.additionalAddress?.postalProperties
                    ?: PostalAddressV7Dto.empty
            }

            val siteMainBpnReference = addressToClean?.bpnReference
                ?.toRequestIfNotBpn(businessPartner.legalAddressReference(), businessPartner.siteReference(), businessPartner.siteMainAddressReference())

            with(site){
                copy(
                    bpnReference = bpnReference.toRequestIfNotBpn(businessPartner.legalEntityReference(), businessPartner.siteReference()),
                    confidenceCriteria = dummyConfidenceCriteria.copy(sharedByOwner = sharedByOwner),
                    hasChanged = businessPartner.type == GoldenRecordTypeV7.Site,
                    siteMainAddress = addressToClean?.let { cleanAddress(addressToClean, siteMainBpnReference!!, true, sharedByOwner) }
                )
            }
        }
    }

    private fun cleanAdditionalAddress(businessPartner: BusinessPartnerV7Dto, sharedByOwner: Boolean): PostalAddressWithScriptVariantsV7Dto? {
        return businessPartner.additionalAddress?.let {
            PostalAddressWithScriptVariantsV7Dto(
                postalProperties = cleanAddress(
                    it.postalProperties,
                    it.bpnReference.toRequestIfNotBpn(businessPartner.legalEntityReference(), null, businessPartner.addressReference()),
                    businessPartner.type == GoldenRecordTypeV7.Address,
                    sharedByOwner
                ),
                scriptVariants = it.scriptVariants
            )

        }
    }


    // The Pool takes this list together with the record's own site as the address's complete site membership and
    // unlinks whatever it leaves out. Consolidating the records that share an address into that complete list needs the
    // whole stream over time, which this service does not see - it is handed one record per task and keeps no ledger.
    // So it passes the sharing member's own statement through: an address several records name ends up with the sites
    // of whichever record was refined last. A refinement service meant for production has to keep that ledger.
    private fun cleanAdditionalSites(businessPartner: BusinessPartnerV7Dto): List<AdditionalSiteV7Dto> {
        return businessPartner.additionalSites.map { additionalSite ->
            // An entry without a name offers nothing to derive a distinct reference from, and the record's name parts -
            // the fallback the primary site reference uses - would alias the entry onto that primary site.
            val siteReference = additionalSite.siteName?.let { siteReferenceOf(it) }
                ?: return@map additionalSite

            additionalSite.copy(
                bpnReference = additionalSite.bpnReference.toRequestIfNotBpn(businessPartner.legalEntityReference(), siteReference)
            )
        }
    }

    private fun cleanAddress(addressToClean: PostalAddressV7Dto, bpnReference: BpnReferenceV7Dto, hasChanged: Boolean, sharedByOwner: Boolean): PostalAddressV7Dto {
        return addressToClean.copy(
                bpnReference =  bpnReference,
                hasChanged = hasChanged,
                confidenceCriteria = dummyConfidenceCriteria.copy(sharedByOwner = sharedByOwner)
            )
    }

    private fun BusinessPartnerV7Dto.legalEntityReference() =
        "LEGAL_ENTITY${legalEntity.legalName ?: namePartsName()}".toUUID()

    private fun BusinessPartnerV7Dto.siteReference() = siteReferenceOf(site?.siteName ?: namePartsName())

    private fun siteReferenceOf(siteName: String) = "S_$siteName".toUUID()

    private fun BusinessPartnerV7Dto.addressReference() = "A_${additionalAddress?.addressName ?: namePartsName()}".toUUID()

    private fun BusinessPartnerV7Dto.namePartsName() = uncategorized.nameParts.joinToString(" ")

    private fun BusinessPartnerV7Dto.legalAddressReference() = "LEGAL_ADDRESS".toUUID()

    private fun BusinessPartnerV7Dto.siteMainAddressReference() = "SITE_MAIN_ADDRESS".toUUID()


    private fun generateBpnRequestIdentifier(legalEntityReference: UUID, siteReference: UUID? = null, addressReference: UUID? = null): BpnReferenceV7Dto{
        val siteReferenceString = siteReference?.let { "${siteReference}_" } ?: ""
        val addressReferenceString = addressReference?.let { "${addressReference}_" } ?: ""
        return BpnReferenceV7Dto( addressReferenceString + siteReferenceString + legalEntityReference.toString(), null, BpnReferenceTypeV7.BpnRequestIdentifier)
    }



    private fun BpnReferenceV7Dto.toRequestIfNotBpn(legalEntityReference: UUID, siteReference: UUID? = null, additionalAddressReference: UUID? = null) =
        if(referenceType == BpnReferenceTypeV7.Bpn)
            this
        else
            generateBpnRequestIdentifier(legalEntityReference, siteReference, additionalAddressReference)

}
