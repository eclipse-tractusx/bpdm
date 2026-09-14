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


package org.eclipse.tractusx.bpdm.pool.service.application.task

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound.GoldenRecordTaskUpsertRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound.GoldenRecordTaskParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound.GoldenRecordTaskResultMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.AddressResponseMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.SiteResponseMapper
import org.eclipse.tractusx.bpdm.pool.model.BpnReferenceAllocation
import org.eclipse.tractusx.bpdm.pool.model.GoldenRecordUpsertResult
import org.eclipse.tractusx.bpdm.pool.model.error.GoldenRecordUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.GoldenRecordUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpsertPlan
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpsertPlan
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerFetchService
import org.eclipse.tractusx.bpdm.pool.service.operation.task.GoldenRecordUpsertService
import org.eclipse.tractusx.bpdm.pool.service.parser.task.BpnReferenceParser
import org.eclipse.tractusx.bpdm.pool.service.parser.task.GoldenRecordUpsertParser
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Answers a batch of reserved golden record tasks with the state of the records they asked for.
 *
 * Entries are handled one after another rather than as one batch: two entries may name the same BPN request
 * identifier and must then reach the same record, which is only settled once the earlier entry has been written.
 */
@Service
class GoldenRecordTaskApplicationService(
    private val upsertRequestMapper: GoldenRecordTaskUpsertRequestMapper,
    private val bpnReferenceParser: BpnReferenceParser,
    private val upsertParser: GoldenRecordUpsertParser,
    private val upsertService: GoldenRecordUpsertService,
    private val parseErrorMapper: GoldenRecordTaskParseErrorMapper,
    private val taskResultMapper: GoldenRecordTaskResultMapper,
    private val businessPartnerFetchService: BusinessPartnerFetchService,
    private val siteResponseMapper: SiteResponseMapper,
    private val addressResponseMapper: AddressResponseMapper
) {

    /**
     * Writes what each task asks for and reports the resulting records, or that task's reasons for not being written.
     */
    @Transactional
    fun upsert(taskEntries: List<TaskStepReservationEntryDto>): List<TaskStepResultEntryDto> {
        val requests = taskEntries.map { upsertRequestMapper.toRequest(it) }
        val bpnReferences = BpnReferenceAllocation(bpnReferenceParser.parse(requests))

        return taskEntries.zip(requests) { taskEntry, request ->
            when (val result = upsertParser.parse(request, bpnReferences)) {
                is ParseResult.Failure -> toErrorReply(taskEntry, result.errors)
                is ParseResult.Success -> toSuccessReply(taskEntry, result.parsed, upsertService.upsert(result.parsed, bpnReferences))
            }
        }
    }

    private fun toErrorReply(taskEntry: TaskStepReservationEntryDto, errors: List<GoldenRecordUpsertParseError>): TaskStepResultEntryDto =
        TaskStepResultEntryDto(
            taskId = taskEntry.taskId,
            businessPartner = taskEntry.businessPartner,
            errors = errors.map { TaskErrorDto(TaskErrorType.Unspecified, parseErrorMapper.toUpsertDescription(it)) }
        )

    private fun toSuccessReply(
        taskEntry: TaskStepReservationEntryDto,
        plan: GoldenRecordUpsertParsed,
        written: GoldenRecordUpsertResult
    ): TaskStepResultEntryDto {
        val confidenceUpdates = written.confidenceUpdates

        val legalEntityResult = readLegalEntity(written, plan.legalEntity !is LegalEntityUpsertPlan.Unchanged)
            .withUpdatedNumberOfSharingMembers(confidenceUpdates.updatedLegalEntities, confidenceUpdates.updatedAddresses)
        val siteResult = written.site
            ?.let { readSite(written, sitePlan(plan) !is SiteUpsertPlan.Unchanged) }
            ?.let { it.copy(siteMainAddress = it.siteMainAddress?.withUpdatedNumberOfSharingMembers(confidenceUpdates.updatedAddresses)) }
        val addressResult = written.additionalAddress
            ?.let { readAddress(it) }
            ?.copyAsPostalAddress { it.withUpdatedNumberOfSharingMembers(confidenceUpdates.updatedAddresses) }

        return TaskStepResultEntryDto(
            taskId = taskEntry.taskId,
            businessPartner = toBusinessPartnerResult(
                taskEntry.businessPartner,
                legalEntityResult,
                siteResult,
                addressResult,
                readMembershipSites(written)
            ),
            errors = emptyList()
        )
    }

    private fun toBusinessPartnerResult(
        stated: BusinessPartner,
        legalEntityResult: LegalEntity,
        siteResult: Site?,
        addressResult: PostalAddressWithScriptVariants?,
        membershipSites: List<AdditionalSite>
    ): BusinessPartner {
        // A site whose main address is the legal address has one address written twice over, and the site result holds
        // its later state, so that is the one both partners are reported with.
        val isLegalAndSiteMainAddress = siteResult?.siteMainAddress?.bpnReference == legalEntityResult.legalAddress.bpnReference

        return stated.copy(
            legalEntity = if (isLegalAndSiteMainAddress) legalEntityResult.copy(legalAddress = siteResult.siteMainAddress!!) else legalEntityResult,
            site = if (isLegalAndSiteMainAddress) siteResult.copy(siteMainAddress = null) else siteResult,
            additionalAddress = addressResult,
            additionalSites = membershipSites
        )
    }

    private fun sitePlan(plan: GoldenRecordUpsertParsed): SiteUpsertPlan? =
        when (plan) {
            is GoldenRecordUpsertParsed.SiteRecord -> plan.site
            is GoldenRecordUpsertParsed.SiteAddressRecord -> plan.site
            is GoldenRecordUpsertParsed.LegalEntityRecord, is GoldenRecordUpsertParsed.LegalEntityAddressRecord -> null
        }

    private fun readLegalEntity(written: GoldenRecordUpsertResult, hasChanged: Boolean): LegalEntity =
        businessPartnerFetchService.fetchDtosByBpns(listOf(written.legalEntity.bpn)).firstOrNull()
            ?.let { taskResultMapper.toTaskResult(it, hasChanged) }
            ?: error("Legal entity ${written.legalEntity.bpn} was written by this task but cannot be read back")

    private fun readSite(written: GoldenRecordUpsertResult, hasChanged: Boolean): Site =
        siteResponseMapper.toSiteWithMainAddress(written.site!!)
            .let { taskResultMapper.toTaskResult(it.site, it.mainAddress, hasChanged) }

    private fun readAddress(address: LogisticAddressDb): PostalAddressWithScriptVariants =
        addressResponseMapper.toAddress(address)
            .let { taskResultMapper.toTaskResult(it.address, it.scriptVariants, hasChanged = true) }

    private fun readMembershipSites(written: GoldenRecordUpsertResult): List<AdditionalSite> {
        val recordSite = written.site ?: return emptyList()
        return written.recordAddress.sites
            .filterNot { it.bpn == recordSite.bpn }
            .sortedBy { it.createdAt }
            .map { AdditionalSite(BpnReference(it.bpn, null, BpnReferenceType.Bpn), it.name) }
    }

    private fun PostalAddress.withUpdatedNumberOfSharingMembers(candidates: Collection<LogisticAddressDb>): PostalAddress =
        copy(
            confidenceCriteria = confidenceCriteria.copy(
                numberOfSharingMembers = candidates.find { it.bpn == bpnReference.referenceValue }
                    ?.confidenceCriteria?.numberOfSharingMembers
                    ?: confidenceCriteria.numberOfSharingMembers
            )
        )

    private fun LegalEntity.withUpdatedNumberOfSharingMembers(
        legalEntityCandidates: Collection<org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb>,
        legalAddressCandidates: Collection<LogisticAddressDb>
    ): LegalEntity =
        copy(
            confidenceCriteria = confidenceCriteria.copy(
                numberOfSharingMembers = legalEntityCandidates.find { it.bpn == bpnReference.referenceValue }
                    ?.confidenceCriteria?.numberOfSharingMembers
                    ?: confidenceCriteria.numberOfSharingMembers
            ),
            legalAddress = legalAddress.withUpdatedNumberOfSharingMembers(legalAddressCandidates)
        )
}
