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

import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.PostSharingStateReadyRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.service.TaskCreationBatchService
import org.eclipse.tractusx.bpdm.gate.service.TaskResolutionBatchService
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.BusinessPartnerInputRequestV7Factory
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorRequestFactoryV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.copyWithBpnReferenceType
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.copyWithBpnReferences
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.copyWithBpnRequests
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolMockDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.PoolRequestFactoryV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.PoolResponseFactoryV7
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType

class GateTestDataClientV7(
    private val gateClient: GateClient,
    private val businessPartnerInputRequestV7Factory: BusinessPartnerInputRequestV7Factory,
    private val orchestratorRequestFactoryV7: OrchestratorRequestFactoryV7,
    private val orchestratorMockDataFactory: OrchestratorMockDataFactory,
    private val taskCreationBatchService: TaskCreationBatchService,
    private val taskResolutionBatchService: TaskResolutionBatchService,
    private val poolMockDataFactory: PoolMockDataFactory,
    private val tenantBpnL: String,
) {

    fun createBusinessPartnerInput(seed: String): BusinessPartnerInputDto {
        val request = businessPartnerInputRequestV7Factory.fromSeed(seed)
        return createBusinessPartnerInput(request)
    }

    fun createBusinessPartnerInput(request: BusinessPartnerInputRequest): BusinessPartnerInputDto {
        return gateClient.businessParters.upsertBusinessPartnersInput(listOf(request)).body!!.single()
    }

    fun setStateToReady(externalId: String) {
        gateClient.sharingState.postSharingStateReady(PostSharingStateReadyRequest(listOf(externalId)))
    }

    fun createBusinessPartnerOutput(externalId: String): BusinessPartnerOutputDto {
        val createdInput = createBusinessPartnerInput(externalId)
        return refineToSuccess(createdInput)
    }

    fun refineToSuccess(input: BusinessPartnerInputDto, seed: String = input.externalId): BusinessPartnerOutputDto{
        prepareLegalEntityRefinement(input, seed)
        return shareBusinessPartnerAndResolve(input.externalId)
    }

    fun refineToLegalEntity(input: BusinessPartnerInputDto, seed: String = input.externalId): LegalEntityWithLegalAddressVerboseDto{
        val poolMockResult = prepareLegalEntityRefinement(input)
        shareBusinessPartnerAndResolve(input.externalId)

        return poolMockResult
    }

    fun refineToLegalEntityOnSite(input: BusinessPartnerInputDto, seed: String = input.externalId): PoolMockDataFactory.SiteWithLegalEntityParent{
        val poolMockResult = poolMockDataFactory.mockLegalAndSiteMainAddressSearchResult(seed)

        val owningCompany = if(input.isOwnCompanyData) tenantBpnL else null
        orchestratorMockDataFactory.mockRefineToLegalEntityOnSite(
            seed,
            poolMockResult.legalEntityParent,
            poolMockResult.site.site,
            owningCompany,
            input.nameParts
        )

        shareBusinessPartnerAndResolve(input.externalId)

        return poolMockResult
    }

    fun refineToSite(input: BusinessPartnerInputDto, seed: String = input.externalId): PoolMockDataFactory.SiteWithLegalEntityParent{
        val poolMockResult = poolMockDataFactory.mockSiteAndMainAddressSearchResult(seed)

        val owningCompany = if(input.isOwnCompanyData) tenantBpnL else null
        orchestratorMockDataFactory.mockRefineToSite(
            seed,
            poolMockResult.legalEntityParent,
            poolMockResult.site,
            owningCompany,
            input.nameParts
        )

        shareBusinessPartnerAndResolve(input.externalId)

        return poolMockResult
    }

    fun refineToAdditionalAddressOfSite(input: BusinessPartnerInputDto, seed: String = input.externalId): PoolMockDataFactory.AdditionalAddressOfSiteResult{
        val poolMockResult = poolMockDataFactory.mockAdditionalAddressOfSiteSearchResult(seed)

        val owningCompany = if(input.isOwnCompanyData) tenantBpnL else null
        orchestratorMockDataFactory.mockRefineToAdditionalAddressOfSite(
            seed,
            poolMockResult.legalEntityParent,
            poolMockResult.siteParent,
            poolMockResult.additionalAddress,
            owningCompany,
            input.nameParts
        )

        shareBusinessPartnerAndResolve(input.externalId)

        return poolMockResult
    }

    fun refineToAdditionalAddressOfSite(
        input: BusinessPartnerInputDto,
        legalEntityRequest: LegalEntityPartnerCreateRequest,
        siteRequest: SitePartnerCreateRequest,
        additionalAddressRequest: AddressPartnerCreateRequest,
        seed: String = input.externalId
    ): PoolMockDataFactory.AdditionalAddressOfSiteResult{
        val poolMockResult = poolMockDataFactory.mockAdditionalAddressOfSiteSearchResult(legalEntityRequest, siteRequest, additionalAddressRequest, seed)

        val owningCompany = if(input.isOwnCompanyData) tenantBpnL else null
        orchestratorMockDataFactory.mockRefineToAdditionalAddressOfSite(
            seed,
            poolMockResult.legalEntityParent,
            poolMockResult.siteParent,
            poolMockResult.additionalAddress,
            owningCompany,
            input.nameParts
        )

        shareBusinessPartnerAndResolve(input.externalId)

        return poolMockResult
    }

    private fun prepareLegalEntityRefinement(input: BusinessPartnerInputDto, seed: String = input.externalId): LegalEntityWithLegalAddressVerboseDto{
        val poolMockResult = poolMockDataFactory.mockLegalEntityAndLegalAddressSearchResult(seed)

        val owningCompany = if(input.isOwnCompanyData) tenantBpnL else null
        orchestratorMockDataFactory.mockRefineToLegalEntity(seed, poolMockResult, owningCompany, input.nameParts)

        return poolMockResult
    }

    private fun shareBusinessPartnerAndResolve(externalId: String): BusinessPartnerOutputDto{
        gateClient.sharingState.postSharingStateReady(PostSharingStateReadyRequest(listOf(externalId)))
        taskCreationBatchService.createTasksForReadyBusinessPartners()
        taskResolutionBatchService.resolveTasks()

        return gateClient.businessParters.getBusinessPartnersOutput(listOf(externalId)).content.single()
    }

}