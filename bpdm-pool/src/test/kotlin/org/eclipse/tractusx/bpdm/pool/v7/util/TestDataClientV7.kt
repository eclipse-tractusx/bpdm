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

package org.eclipse.tractusx.bpdm.pool.v7.util

import com.github.tomakehurst.wiremock.client.WireMock
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.service.TaskBatchResolutionService
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.PoolRequestFactoryV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withParticipantData
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner

class TestDataClientV7(
    private val poolClient: PoolApiClient,
    private val requestFactory: PoolRequestFactoryV7,
    private val orchestratorMockDataFactory: OrchestratorMockDataFactory,
    private val taskBatchResolutionService: TaskBatchResolutionService
) {


    fun createLegalEntity(seed: String): LegalEntityWithLegalAddressVerboseDto{
        return createLegalEntity(requestFactory.buildLegalEntity(seed))
    }

    fun createParticipantLegalEntity(seed: String): LegalEntityWithLegalAddressVerboseDto{
        return createLegalEntity(requestFactory.buildLegalEntity(seed).withParticipantData(true))
    }

    fun createLegalEntity(request: LegalEntityDto): LegalEntityWithLegalAddressVerboseDto{
        return poolClient.legalEntities.createBusinessPartners(listOf(LegalEntityPartnerCreateRequest(request, null))).entities.first().legalEntity
    }

    fun updateLegalEntity(bpnL: String, seed: String): LegalEntityWithLegalAddressVerboseDto{
        return updateLegalEntity(bpnL, requestFactory.buildLegalEntity(seed))
    }

    fun updateLegalEntity(bpnL: String, request: LegalEntityDto): LegalEntityWithLegalAddressVerboseDto{
        return poolClient.legalEntities.updateBusinessPartners(listOf(LegalEntityPartnerUpdateRequest(bpnL, request))).entities.first().legalEntity
    }

    fun createSite(legalEntity: LegalEntityWithLegalAddressVerboseDto, seed: String): SitePartnerCreateVerboseDto {
        val request = requestFactory.buildSiteCreateRequest(seed, legalEntity)
        return poolClient.sites.createSite(listOf(request)).entities.first()
    }

    fun createLegalAddressSite(legalEntity: LegalEntityWithLegalAddressVerboseDto, seed: String): SitePartnerCreateVerboseDto {
        val request = requestFactory.buildLegalAddressSiteCreateRequest(seed, legalEntity)
        return poolClient.sites.createSiteWithLegalReference(listOf(request)).entities.first()
    }

    fun updateSite(existingSite: SitePartnerCreateVerboseDto, seed: String): SitePartnerCreateVerboseDto {
        val request = requestFactory.createSiteUpdateRequest(seed, existingSite)
        return poolClient.sites.updateSite(listOf(request)).entities.first()
    }

    fun createAdditionalAddress(legalEntity: LegalEntityWithLegalAddressVerboseDto, seed: String): AddressPartnerCreateVerboseDto {
        val request = requestFactory.buildAdditionalAddressCreateRequest(seed, legalEntity)
        return poolClient.addresses.createAddresses(listOf(request)).entities.first()
    }

    fun createAdditionalAddress(site: SitePartnerCreateVerboseDto, seed: String): AddressPartnerCreateVerboseDto {
        val request = requestFactory.buildAdditionalAddressCreateRequest(seed, site)
        return poolClient.addresses.createAddresses(listOf(request)).entities.first()
    }

    fun processTask(seed: String, businessPartner: BusinessPartner): BusinessPartner{
        WireMock.reset()

        orchestratorMockDataFactory.mockReservedBusinessPartner(seed, businessPartner)

        taskBatchResolutionService.processTasks()
        val businessPartnerResult = orchestratorMockDataFactory.getBusinessPartnerResolution()
        return businessPartnerResult
    }
}