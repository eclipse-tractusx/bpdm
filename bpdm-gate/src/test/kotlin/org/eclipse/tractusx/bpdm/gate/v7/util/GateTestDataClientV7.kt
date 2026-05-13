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
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolMockDataFactory

class GateTestDataClientV7(
    private val gateClient: GateClient,
    private val businessPartnerInputRequestV7Factory: BusinessPartnerInputRequestV7Factory,
    private val orchestratorMockDataFactory: OrchestratorMockDataFactory,
    private val taskCreationBatchService: TaskCreationBatchService,
    private val taskResolutionBatchService: TaskResolutionBatchService,
    private val poolMockDataFactory: PoolMockDataFactory,
    private val tenantBpnL: String,
) {

    fun upsertBusinessPartnerInput(seed: String): BusinessPartnerInputDto {
        val request = businessPartnerInputRequestV7Factory.fromSeed(seed)
        return upsertBusinessPartnerInput(request)
    }

    fun upsertBusinessPartnerInput(request: BusinessPartnerInputRequest): BusinessPartnerInputDto {
        return gateClient.businessParters.upsertBusinessPartnersInput(listOf(request)).body!!.single()
    }

    fun setStateToReady(externalId: String) {
        gateClient.sharingState.postSharingStateReady(PostSharingStateReadyRequest(listOf(externalId)))
    }

    fun createBusinessPartnerOutput(seed: String): BusinessPartnerOutputDto {
        val upsertedInput = upsertBusinessPartnerInput(seed)
        return refineToSuccess(upsertedInput)
    }

    fun updateBusinessPartnerOutput(output: BusinessPartnerOutputDto, newSeed: String): BusinessPartnerOutputDto {
        val upsertedInput = upsertBusinessPartnerInput(
            businessPartnerInputRequestV7Factory.fromSeed(newSeed).copy(externalId = output.externalId)
        )
        return refineToSuccess(upsertedInput)
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