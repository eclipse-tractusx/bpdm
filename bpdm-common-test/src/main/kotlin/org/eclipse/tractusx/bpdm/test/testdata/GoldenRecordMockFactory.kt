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

package org.eclipse.tractusx.bpdm.test.testdata

import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolMockDataFactory
import org.eclipse.tractusx.orchestrator.api.model.TaskClientStateDto

class GoldenRecordMockFactory(
    private val poolMockDataFactory: PoolMockDataFactory,
    private val orchestratorMockDataFactory: OrchestratorMockDataFactory
) {
    data class AdditionalAddressOfSiteRefinement(
        val poolResult: PoolMockDataFactory.AdditionalAddressOfSiteResult,
        val taskState: TaskClientStateDto
    )

    fun mockLegalEntityRefinement(seed: String, owningCompany: String?, nameParts: List<String>): LegalEntityWithLegalAddressVerboseDto {
        val poolMockResult = poolMockDataFactory.mockLegalEntityAndLegalAddressSearchResult(seed)
        orchestratorMockDataFactory.mockRefineToLegalEntity(seed, poolMockResult, owningCompany, nameParts)
        return poolMockResult
    }

    fun mockLegalEntityOnSiteRefinement(seed: String, owningCompany: String?, nameParts: List<String>): PoolMockDataFactory.SiteWithLegalEntityParent {
        val poolMockResult = poolMockDataFactory.mockLegalAndSiteMainAddressSearchResult(seed)
        orchestratorMockDataFactory.mockRefineToLegalEntityOnSite(seed, poolMockResult.legalEntityParent, poolMockResult.site.site, owningCompany, nameParts)
        return poolMockResult
    }

    fun mockSiteRefinement(seed: String, owningCompany: String?, nameParts: List<String>): PoolMockDataFactory.SiteWithLegalEntityParent {
        val poolMockResult = poolMockDataFactory.mockSiteAndMainAddressSearchResult(seed)
        orchestratorMockDataFactory.mockRefineToSite(seed, poolMockResult.legalEntityParent, poolMockResult.site, owningCompany, nameParts)
        return poolMockResult
    }

    fun mockAdditionalAddressOfSiteRefinement(seed: String, owningCompany: String?, nameParts: List<String>): AdditionalAddressOfSiteRefinement {
        val poolMockResult = poolMockDataFactory.mockAdditionalAddressOfSiteSearchResult(seed)
        val taskState = orchestratorMockDataFactory.mockRefineToAdditionalAddressOfSite(
            seed,
            poolMockResult.legalEntityParent,
            poolMockResult.siteParent,
            poolMockResult.additionalAddress,
            owningCompany,
            nameParts
        )
        return AdditionalAddressOfSiteRefinement(poolMockResult, taskState)
    }
}
