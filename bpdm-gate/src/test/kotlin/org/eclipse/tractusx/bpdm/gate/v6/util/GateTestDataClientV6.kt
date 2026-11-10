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

package org.eclipse.tractusx.bpdm.gate.v6.util

import org.eclipse.tractusx.bpdm.gate.api.v6.client.GateV6Client
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.input.BusinessPartnerInputV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.input.BusinessPartnerInputV6Request
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.sharingstate.PostSharingStateReadyV6Request
import org.eclipse.tractusx.bpdm.gate.api.v6.model.relation.RelationTypeV6
import org.eclipse.tractusx.bpdm.gate.api.v6.model.relation.input.RelationPostV6Request
import org.eclipse.tractusx.bpdm.gate.api.v6.model.relation.input.RelationV6Dto
import org.eclipse.tractusx.bpdm.gate.service.application.GoldenRecordTaskCreationApplicationService
import org.eclipse.tractusx.bpdm.gate.service.application.GoldenRecordTaskResolutionApplicationService
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateV7Request
import org.eclipse.tractusx.bpdm.test.testdata.gate.v6.GateTestDataFactoryV6
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolMockDataFactory
import org.eclipse.tractusx.bpdm.orchestrator.api.model.TaskClientStateDto
import org.eclipse.tractusx.bpdm.orchestrator.api.model.TaskErrorType
import java.time.Instant
import kotlin.random.Random

/**
 * Client offering functionality to quickly set up a given test environment for test cases (For the GIVEN section)
 */
class GateTestDataClientV6 (
    val testDataFactory: GateTestDataFactoryV6,
    val operatorClient: GateV6Client,
    private val orchestratorMockDataFactory: OrchestratorMockDataFactory,
    private val taskCreationService: GoldenRecordTaskCreationApplicationService,
    private val taskResolutionService: GoldenRecordTaskResolutionApplicationService,
    private val tenantBpnL: String,
    private val poolMockDataFactory: PoolMockDataFactory
){
    fun createBusinessPartnerInput(seed: String, externalId: String = seed, externalSequenceTimestamp: Instant? = null): BusinessPartnerInputV6Dto{
        val createRequest = testDataFactory.request.createFullValid(seed, externalId).copy(externalSequenceTimestamp = externalSequenceTimestamp)
        return operatorClient.businessPartners.upsertBusinessPartnersInput(listOf(createRequest)).body!!.single()
    }

    fun createBusinessPartnerInput(request: BusinessPartnerInputV6Request): BusinessPartnerInputV6Dto {
        return operatorClient.businessPartners.upsertBusinessPartnersInput(listOf(request)).body!!.single()
    }

    fun createRelation(externalId: String, source: BusinessPartnerInputV6Dto, target: BusinessPartnerInputV6Dto, relationType: RelationTypeV6): RelationV6Dto{
        return operatorClient.relations.post(RelationPostV6Request(externalId, relationType, source.externalId, target.externalId))
    }

    fun createRelationWithBusinessPartners(externalId: String, relationType: RelationTypeV6, seed: String = externalId): RelationV6Dto{
        val sourceInput = createBusinessPartnerInput("$seed Source")
        val targetInput = createBusinessPartnerInput("$seed Target")

        return operatorClient.relations.post(RelationPostV6Request(externalId, relationType, sourceInput.externalId, targetInput.externalId))
    }

    fun refineToLegalEntity(input: BusinessPartnerInputV6Dto, seed: String = input.externalId): LegalEntityWithLegalAddressVerboseV7Dto{
        val poolMockResult = poolMockDataFactory.mockLegalEntityAndLegalAddressSearchResult(seed)

        val owningCompany = if(input.isOwnCompanyData) tenantBpnL else null
        orchestratorMockDataFactory.mockRefineToLegalEntity(seed, poolMockResult, owningCompany, input.nameParts)

        shareBusinessPartnerAndResolve(input.externalId)

        return poolMockResult
    }

    fun refineToLegalEntityOnSite(input: BusinessPartnerInputV6Dto, seed: String = input.externalId): PoolMockDataFactory.SiteWithLegalEntityParent{
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

    fun refineToSite(input: BusinessPartnerInputV6Dto, seed: String = input.externalId): PoolMockDataFactory.SiteWithLegalEntityParent{
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

    fun refineToAdditionalAddressOfSite(input: BusinessPartnerInputV6Dto, seed: String = input.externalId): PoolMockDataFactory.AdditionalAddressOfSiteResult{
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
        input: BusinessPartnerInputV6Dto,
        legalEntityRequest: LegalEntityPartnerCreateV7Request,
        siteRequest: SitePartnerCreateV7Request,
        additionalAddressRequest: AddressPartnerCreateV7Request,
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



    fun setStateToReady(externalId: String){
        operatorClient.sharingStates.postSharingStateReady(PostSharingStateReadyV6Request(listOf(externalId)))
    }

    fun setStateToPending(externalId: String, seed: String = externalId): TaskClientStateDto{
        setStateToReady(externalId)
        val createdTask = orchestratorMockDataFactory.mockCreateTask(seed)
        taskCreationService.createTasksForReadyBusinessPartners()

        return createdTask
    }

    fun setStateToSuccess(externalId: String, seed: String = externalId): TaskClientStateDto{
        val poolMockResult = poolMockDataFactory.mockAdditionalAddressOfSiteSearchResult(seed)
        val mockedRefinedTask = orchestratorMockDataFactory.mockRefineToAdditionalAddressOfSite(
            seed,
            poolMockResult.legalEntityParent,
            poolMockResult.siteParent,
            poolMockResult.additionalAddress,
            null,
            emptyList()
        )

        setStateToReady(externalId)
        taskCreationService.createTasksForReadyBusinessPartners()
        taskResolutionService.resolveFinishedTasks()

        return mockedRefinedTask
    }

    fun setStateToError(externalId: String, seed: String = externalId){
        val random = Random(seed.hashCode())
        setStateToError(externalId, seed, TaskErrorType.entries.random(random))
    }

    fun setStateToError(externalId: String, seed: String = externalId, errorType: TaskErrorType): TaskClientStateDto{
        val errorTask = orchestratorMockDataFactory.mockSharingError(seed, errorType)

        setStateToReady(externalId)
        taskCreationService.createTasksForReadyBusinessPartners()
        taskResolutionService.resolveFinishedTasks()

        return errorTask
    }

    private fun shareBusinessPartnerAndResolve(externalId: String){
        operatorClient.sharingStates.postSharingStateReady(PostSharingStateReadyV6Request(listOf(externalId)))
        taskCreationService.createTasksForReadyBusinessPartners()
        taskResolutionService.resolveFinishedTasks()
    }
}