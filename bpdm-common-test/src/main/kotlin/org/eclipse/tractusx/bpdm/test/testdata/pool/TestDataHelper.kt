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

package org.eclipse.tractusx.bpdm.test.testdata.pool

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.request.ReasonCodeUpsertRequest
import org.eclipse.tractusx.bpdm.test.util.Timeframe
import java.time.Instant

fun List<LegalEntityHierarchy>.getAllLegalEntities() = map { it.legalEntity }
fun List<LegalEntityHierarchy>.getAllSites() = flatMap { it.getAllSites() }
fun List<LegalEntityHierarchy>.getAllAddresses() = flatMap { it.getAllAddresses() }

/**
 * This class provides functionality to create a [TestDataEnvironment] to support creating and comparing business partner data
 * Also it has functionality to easily create hierarchical business partner data
 */
class PoolDataHelper(
    private val poolClient: PoolApiClient,
    private val reasonCodes: List<ReasonCodeDto>
) {

    fun createTestDataEnvironment(): TestDataEnvironment {
        val legalEntityIdentifierTypes = poolClient.metadata.getIdentifierTypes(PaginationRequest(), IdentifierBusinessPartnerType.LEGAL_ENTITY, null).content.toList()
        val addressIdentifierTypes = poolClient.metadata.getIdentifierTypes(PaginationRequest(), IdentifierBusinessPartnerType.ADDRESS, null).content.toList()
        val legalForms = poolClient.metadata.getLegalForms(PaginationRequest()).content.toList()
        val adminAreas = poolClient.metadata.getAdminAreasLevel1(PaginationRequest()).content.toList()

        reasonCodes.forEach { poolClient.metadata.upsertReasonCode(ReasonCodeUpsertRequest(it)) }

        val testMetadata = TestMetadata(legalForms, legalEntityIdentifierTypes, addressIdentifierTypes, adminAreas, reasonCodes)

        val requestFactory = BusinessPartnerRequestFactory(testMetadata)
        val expectedResultFactory = ExpectedBusinessPartnerResultFactory(testMetadata)

        return TestDataEnvironment(testMetadata, requestFactory, expectedResultFactory)
    }

    fun createBusinessPartnerHierarchies(hierarchies: List<LegalEntityHierarchy>): HierarchyCreationResponse {
        val startCreationTime = Instant.now()

        val bpnlsByIndex =
            poolClient.legalEntities.createBusinessPartners(hierarchies.getAllLegalEntities()).entities.associate { Pair(it.index, it.legalEntity.bpnl) }
                .toMap()

        val hierarchiesWithBpnL = hierarchies.map { hierarchy ->
            val bpnl = bpnlsByIndex[hierarchy.legalEntity.index]!!
            hierarchy.setParentBpnl(bpnl)
        }

        val bpnsByIndex = poolClient.sites.createSite(hierarchiesWithBpnL.getAllSites()).entities.associate { Pair(it.index, it.site.bpns) }.toMap()

        val hierarchiesWithBpnS = hierarchiesWithBpnL.map { hierarchy ->
            hierarchy.copy(siteHierarchy = hierarchy.siteHierarchy.map { it.setParentBpns(bpnsByIndex[it.site.index]!!) })
        }

        poolClient.addresses.createAddresses(hierarchiesWithBpnS.getAllAddresses())

        val endCreationTime = Instant.now()

        return HierarchyCreationResponse(hierarchiesWithBpnS, Timeframe(startCreationTime, endCreationTime))
    }
}

data class HierarchyCreationResponse(
    val hierarchiesWithBpns: List<LegalEntityHierarchy>,
    val creationTimeframe: Timeframe
)

data class TestDataEnvironment(
    val metadata: TestMetadata,
    val requestFactory: BusinessPartnerRequestFactory,
    val expectFactory: ExpectedBusinessPartnerResultFactory
)

data class TestMetadata(
    val legalForms: List<LegalFormDto>,
    val legalEntityIdentifierTypes: List<IdentifierTypeDto>,
    val addressIdentifierTypes: List<IdentifierTypeDto>,
    val adminAreas: List<CountrySubdivisionDto>,
    val reasonCodes: List<ReasonCodeDto>
)