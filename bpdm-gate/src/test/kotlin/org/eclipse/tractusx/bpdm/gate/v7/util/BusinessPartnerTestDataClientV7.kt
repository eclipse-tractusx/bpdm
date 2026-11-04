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

package org.eclipse.tractusx.bpdm.gate.v7.util

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.api.v7.client.GateV7Client
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.BusinessPartnerInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.BusinessPartnerInputV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.output.BusinessPartnerOutputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.sharingstate.PostSharingStateReadyV7Request
import org.eclipse.tractusx.bpdm.gate.service.application.GoldenRecordConsistencyCheckApplicationService
import org.eclipse.tractusx.bpdm.gate.service.application.GoldenRecordCountedSyncApplicationService
import org.eclipse.tractusx.bpdm.gate.service.application.GoldenRecordTaskCreationApplicationService
import org.eclipse.tractusx.bpdm.gate.service.application.GoldenRecordTaskHealthCheckApplicationService
import org.eclipse.tractusx.bpdm.gate.service.application.GoldenRecordTaskResolutionApplicationService
import org.eclipse.tractusx.bpdm.gate.service.application.GoldenRecordUpdateApplicationService
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.changelog.ChangelogEntryVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.changelog.ChangelogTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.test.testdata.GoldenRecordMockFactory
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolMockDataFactory
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner
import org.eclipse.tractusx.orchestrator.api.model.SharingMemberRecordUpdateRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskClientStateDto
import org.eclipse.tractusx.orchestrator.api.model.TaskErrorType
import java.time.Instant

class BusinessPartnerTestDataClientV7(
    private val gateClient: GateV7Client,
    private val testDataFactory: TestDataFactoryGateV7,
    private val orchestratorMockDataFactory: OrchestratorMockDataFactory,
    private val taskCreationService: GoldenRecordTaskCreationApplicationService,
    private val taskResolutionService: GoldenRecordTaskResolutionApplicationService,
    private val taskHealthCheckService: GoldenRecordTaskHealthCheckApplicationService,
    private val consistencyCheckService: GoldenRecordConsistencyCheckApplicationService,
    private val goldenRecordCountedSyncService: GoldenRecordCountedSyncApplicationService,
    private val goldenRecordMockFactory: GoldenRecordMockFactory,
    private val poolMockDataFactory: PoolMockDataFactory,
    private val goldenRecordUpdateService: GoldenRecordUpdateApplicationService,
    private val tenantBpnL: String,
) {

    fun upsertInput(seed: String): BusinessPartnerInputV7Dto {
        val request = testDataFactory.businessPartner.input.request.fromSeed(seed)
        return upsertInput(request)
    }

    fun upsertInput(request: BusinessPartnerInputV7Request): BusinessPartnerInputV7Dto {
        return gateClient.businessParters.upsertBusinessPartnersInput(listOf(request)).body!!.single()
    }

    fun setStateToReady(externalId: String) {
        gateClient.sharingState.postSharingStateReady(PostSharingStateReadyV7Request(listOf(externalId)))
    }

    fun setStateToPending(externalId: String, seed: String = externalId): TaskClientStateDto {
        setStateToReady(externalId)
        val createdTask = orchestratorMockDataFactory.mockCreateTask(seed)
        taskCreationService.createTasksForReadyBusinessPartners()
        return createdTask
    }

    fun sharedBusinessPartnerOf(externalId: String, seed: String = externalId): BusinessPartner {
        // Other tests share this mock server, so only what is recorded from here on is this record's sharing.
        orchestratorMockDataFactory.resetRecordedRequests()
        setStateToPending(externalId, seed)
        return orchestratorMockDataFactory.getCreatedTaskBusinessPartners().single()
    }

    fun shareBusinessPartnerAndResolve(externalId: String): BusinessPartnerOutputV7Dto {
        gateClient.sharingState.postSharingStateReady(PostSharingStateReadyV7Request(listOf(externalId)))
        taskCreationService.createTasksForReadyBusinessPartners()
        taskResolutionService.resolveFinishedTasks()
        return gateClient.businessParters.getBusinessPartnersOutput(listOf(externalId)).content.single()
    }

    fun upsertAndShareBusinessPartner(seed: String): BusinessPartnerOutputV7Dto {
        val input = upsertInput(seed)
        return shareBusinessPartnerAndResolve(input.externalId)
    }

    fun upsertAndShareBusinessPartner(request: BusinessPartnerInputV7Request): BusinessPartnerOutputV7Dto {
        val input = upsertInput(request)
        return shareBusinessPartnerAndResolve(input.externalId)
    }

    fun createErrorSharing(externalId: String, seed: String = externalId, errorType: TaskErrorType, errorMessage: String) {
        setStateToPending(externalId, seed)
        orchestratorMockDataFactory.mockResolveTask(seed, errorType, errorMessage)
        taskResolutionService.resolveFinishedTasks()
    }

    fun createErrorSharingAndResolve(externalId: String, seed: String = externalId, errorType: TaskErrorType, errorMessage: String) {
        val createdTask = setStateToPending(externalId, seed)
        orchestratorMockDataFactory.mockResolveTask(createdTask.taskId, errorType, errorMessage)
        taskResolutionService.resolveFinishedTasks()
    }

    fun refineToPendingLegalEntity(input: BusinessPartnerInputV7Dto, seed: String = input.externalId): Pair<SharingMemberRecordUpdateRequest, LegalEntityWithLegalAddressVerboseV7Dto> {
        val owningCompany = if (input.isOwnCompanyData) tenantBpnL else null
        val refinement = goldenRecordMockFactory.mockLegalEntityRefinement(seed, owningCompany, input.nameParts)
        return Pair(refinement.sharingMemberRecord, refinement.poolResult)
    }

    fun refineToLegalEntity(input: BusinessPartnerInputV7Dto, seed: String = input.externalId): LegalEntityWithLegalAddressVerboseV7Dto {
        val owningCompany = if (input.isOwnCompanyData) tenantBpnL else null
        val poolMockResult = goldenRecordMockFactory.mockLegalEntityRefinement(seed, owningCompany, input.nameParts)
        shareBusinessPartnerAndResolve(input.externalId)
        return poolMockResult
    }

    fun refineToLegalEntityChangedInPool(input: BusinessPartnerInputV7Dto, seed: String = input.externalId): LegalEntityWithLegalAddressVerboseV7Dto {
        val owningCompany = if (input.isOwnCompanyData) tenantBpnL else null
        val poolMockResult = goldenRecordMockFactory.mockLegalEntityChangedInPool(seed, owningCompany, input.nameParts)
        shareBusinessPartnerAndResolve(input.externalId)
        return poolMockResult
    }

    fun refineToLegalEntityOnSite(input: BusinessPartnerInputV7Dto, seed: String = input.externalId): PoolMockDataFactory.SiteWithLegalEntityParent {
        val owningCompany = if (input.isOwnCompanyData) tenantBpnL else null
        val poolMockResult = goldenRecordMockFactory.mockSiteRefinement(seed, owningCompany, input.nameParts)
        shareBusinessPartnerAndResolve(input.externalId)
        return poolMockResult
    }

    fun refineToSite(input: BusinessPartnerInputV7Dto, seed: String = input.externalId): PoolMockDataFactory.SiteWithLegalEntityParent {
        val owningCompany = if (input.isOwnCompanyData) tenantBpnL else null
        val poolMockResult = goldenRecordMockFactory.mockAdditionalSiteRefinement(seed, owningCompany, input.nameParts)
        shareBusinessPartnerAndResolve(input.externalId)
        return poolMockResult
    }

    fun refineToAdditionalAddressOfSite(input: BusinessPartnerInputV7Dto, seed: String = input.externalId): PoolMockDataFactory.AdditionalAddressOfSiteResult {
        val owningCompany = if (input.isOwnCompanyData) tenantBpnL else null
        val refinement = goldenRecordMockFactory.mockAdditionalAddressOfSiteRefinement(seed, owningCompany, input.nameParts)
        shareBusinessPartnerAndResolve(input.externalId)
        return refinement.poolResult
    }

    fun refineToAdditionalAddressWithAdditionalSites(input: BusinessPartnerInputV7Dto, seed: String = input.externalId): PoolMockDataFactory.AdditionalAddressWithAdditionalSitesResult {
        val owningCompany = if (input.isOwnCompanyData) tenantBpnL else null
        val refinement = goldenRecordMockFactory.mockAdditionalAddressWithAdditionalSitesRefinement(seed, owningCompany, input.nameParts)
        shareBusinessPartnerAndResolve(input.externalId)
        return refinement.poolResult
    }

    /**
     * Lets the Pool report [changedAddress] as updated and synchronizes that change into the Gate output,
     * with the Pool returning [memberSites] as the sites the address belongs to.
     */
    fun syncAddressChangeFromPool(changedAddress: LogisticAddressVerboseV7Dto, memberSites: List<SiteWithMainAddressVerboseV7Dto>) {
        poolMockDataFactory.mockSiteSearchResult(memberSites)
        poolMockDataFactory.mockAddressSearchResult(changedAddress)
        poolMockDataFactory.mockChangelogSearchResult(
            listOf(ChangelogEntryVerboseV7Dto(changedAddress.address.bpna, BusinessPartnerType.ADDRESS, Instant.now(), ChangelogTypeV7.UPDATE))
        )
        goldenRecordUpdateService.updateFromPoolChanges()
    }

    /**
     * Lets the Pool report [changedAddress] as updated and synchronizes that change into the Gate output,
     * with the Pool returning [memberSites] as the sites the address belongs to.
     */
    fun syncAddressChangeFromPool(changedAddress: LogisticAddressVerboseV7Dto, memberSites: List<SiteWithMainAddressVerboseV7Dto>) {
        poolMockDataFactory.mockSiteSearchResult(memberSites)
        poolMockDataFactory.mockAddressSearchResult(changedAddress)
        poolMockDataFactory.mockChangelogSearchResult(
            listOf(ChangelogEntryVerboseV7Dto(changedAddress.address.bpna, BusinessPartnerType.ADDRESS, Instant.now(), ChangelogTypeV7.UPDATE))
        )
        goldenRecordUpdateService.updateFromPoolChanges()
    }
}
