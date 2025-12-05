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

import org.eclipse.tractusx.bpdm.gate.api.model.request.PostSharingStateReadyRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.service.TaskCreationBatchService
import org.eclipse.tractusx.bpdm.gate.service.TaskResolutionBatchService
import org.eclipse.tractusx.bpdm.test.testdata.gate.v6.GateTestDataFactoryV6
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolMockDataFactory
import java.time.Instant

/**
 * Client offering functionality to quickly set up a given test environment for test cases (For the GIVEN section)
 */
class GateTestDataClientV6 (
    val testDataFactory: GateTestDataFactoryV6,
    val operatorClient: GateOperatorClientV6,
    private val orchestratorMockDataFactory: OrchestratorMockDataFactory,
    private val taskCreationBatchService: TaskCreationBatchService,
    private val taskResolutionBatchService: TaskResolutionBatchService,
    private val tenantBpnL: String,
    private val poolMockDataFactory: PoolMockDataFactory
){
    fun createBusinessPartnerInput(seed: String, externalId: String = seed, externalSequenceTimestamp: Instant? = null): BusinessPartnerInputDto{
        val createRequest = testDataFactory.request.createFullValid(seed, externalId).copy(externalSequenceTimestamp = externalSequenceTimestamp)
        return operatorClient.businessPartners.upsertBusinessPartnersInput(listOf(createRequest)).body!!.single()
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

        operatorClient.sharingStates.postSharingStateReady(PostSharingStateReadyRequest(listOf(input.externalId)))
        taskCreationBatchService.createTasksForReadyBusinessPartners()
        taskResolutionBatchService.resolveTasks()

        return poolMockResult
    }

}