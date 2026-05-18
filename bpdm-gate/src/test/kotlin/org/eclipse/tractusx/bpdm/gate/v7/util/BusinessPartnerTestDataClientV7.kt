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
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.test.testdata.GoldenRecordMockFactory
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolMockDataFactory
import org.eclipse.tractusx.orchestrator.api.model.TaskClientStateDto
import org.eclipse.tractusx.orchestrator.api.model.TaskErrorType

class BusinessPartnerTestDataClientV7(
    private val gateClient: GateClient,
    private val testDataFactory: TestDataFactoryGateV7,
    private val orchestratorMockDataFactory: OrchestratorMockDataFactory,
    private val taskCreationBatchService: TaskCreationBatchService,
    private val taskResolutionBatchService: TaskResolutionBatchService,
    private val goldenRecordMockFactory: GoldenRecordMockFactory,
    private val tenantBpnL: String,
) {

    fun upsertInput(seed: String): BusinessPartnerInputDto {
        val request = testDataFactory.businessPartner.input.request.fromSeed(seed)
        return upsertInput(request)
    }

    fun upsertInput(request: BusinessPartnerInputRequest): BusinessPartnerInputDto {
        return gateClient.businessParters.upsertBusinessPartnersInput(listOf(request)).body!!.single()
    }

    fun setStateToReady(externalId: String) {
        gateClient.sharingState.postSharingStateReady(PostSharingStateReadyRequest(listOf(externalId)))
    }

    fun setStateToPending(externalId: String, seed: String = externalId): TaskClientStateDto {
        setStateToReady(externalId)
        val createdTask = orchestratorMockDataFactory.mockCreateTask(seed)
        taskCreationBatchService.createTasksForReadyBusinessPartners()
        return createdTask
    }

    fun setStateToSuccess(externalId: String, seed: String = externalId): TaskClientStateDto {
        val refinement = goldenRecordMockFactory.mockAdditionalAddressOfSiteRefinement(seed, null, emptyList())
        setStateToReady(externalId)
        taskCreationBatchService.createTasksForReadyBusinessPartners()
        taskResolutionBatchService.resolveTasks()
        return refinement.taskState
    }

    fun setStateToError(externalId: String, seed: String = externalId, errorType: TaskErrorType): TaskClientStateDto {
        val errorTask = orchestratorMockDataFactory.mockSharingError(seed, errorType)
        setStateToReady(externalId)
        taskCreationBatchService.createTasksForReadyBusinessPartners()
        taskResolutionBatchService.resolveTasks()
        return errorTask
    }

    fun createOutput(seed: String): BusinessPartnerOutputDto {
        val upsertedInput = upsertInput(seed)
        return refineToSuccess(upsertedInput)
    }

    fun updateOutput(output: BusinessPartnerOutputDto, newSeed: String): BusinessPartnerOutputDto {
        val upsertedInput = upsertInput(
            testDataFactory.businessPartner.input.request.fromSeed(newSeed).copy(externalId = output.externalId)
        )
        return refineToSuccess(upsertedInput)
    }

    fun refineToSuccess(input: BusinessPartnerInputDto, seed: String = input.externalId): BusinessPartnerOutputDto {
        prepareLegalEntityRefinement(input, seed)
        return shareBusinessPartnerAndResolve(input.externalId)
    }

    fun refineToLegalEntity(input: BusinessPartnerInputDto, seed: String = input.externalId): LegalEntityWithLegalAddressVerboseDto {
        val poolMockResult = prepareLegalEntityRefinement(input)
        shareBusinessPartnerAndResolve(input.externalId)
        return poolMockResult
    }

    fun refineToLegalEntityOnSite(input: BusinessPartnerInputDto, seed: String = input.externalId): PoolMockDataFactory.SiteWithLegalEntityParent {
        val owningCompany = if (input.isOwnCompanyData) tenantBpnL else null
        val poolMockResult = goldenRecordMockFactory.mockLegalEntityOnSiteRefinement(seed, owningCompany, input.nameParts)
        shareBusinessPartnerAndResolve(input.externalId)
        return poolMockResult
    }

    fun refineToSite(input: BusinessPartnerInputDto, seed: String = input.externalId): PoolMockDataFactory.SiteWithLegalEntityParent {
        val owningCompany = if (input.isOwnCompanyData) tenantBpnL else null
        val poolMockResult = goldenRecordMockFactory.mockSiteRefinement(seed, owningCompany, input.nameParts)
        shareBusinessPartnerAndResolve(input.externalId)
        return poolMockResult
    }

    fun refineToAdditionalAddressOfSite(input: BusinessPartnerInputDto, seed: String = input.externalId): PoolMockDataFactory.AdditionalAddressOfSiteResult {
        val owningCompany = if (input.isOwnCompanyData) tenantBpnL else null
        val refinement = goldenRecordMockFactory.mockAdditionalAddressOfSiteRefinement(seed, owningCompany, input.nameParts)
        shareBusinessPartnerAndResolve(input.externalId)
        return refinement.poolResult
    }

    private fun prepareLegalEntityRefinement(input: BusinessPartnerInputDto, seed: String = input.externalId): LegalEntityWithLegalAddressVerboseDto {
        val owningCompany = if (input.isOwnCompanyData) tenantBpnL else null
        return goldenRecordMockFactory.mockLegalEntityRefinement(seed, owningCompany, input.nameParts)
    }

    private fun shareBusinessPartnerAndResolve(externalId: String): BusinessPartnerOutputDto {
        gateClient.sharingState.postSharingStateReady(PostSharingStateReadyRequest(listOf(externalId)))
        taskCreationBatchService.createTasksForReadyBusinessPartners()
        taskResolutionBatchService.resolveTasks()
        return gateClient.businessParters.getBusinessPartnersOutput(listOf(externalId)).content.single()
    }
}
