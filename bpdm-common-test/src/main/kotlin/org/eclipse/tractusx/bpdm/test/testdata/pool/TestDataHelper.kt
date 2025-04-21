/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.test.util.Timeframe
import java.time.Instant
import kotlin.random.Random

fun List<LegalEntityHierarchy>.getAllLegalEntities() = map { it.legalEntity }
fun List<LegalEntityHierarchy>.getAllSites() = flatMap { it.getAllSites() }
fun List<LegalEntityHierarchy>.getAllAddresses() = flatMap { it.getAllAddresses() }

/**
 * This class provides functionality to create a [TestDataEnvironment] to support creating and comparing business partner data
 * Also it has functionality to easily create hierarchical business partner data
 */
class PoolDataHelper(
    private val metadataToCreate: TestMetadataKeys,
    private val poolClient: PoolApiClient,
) {

    fun createTestDataEnvironment(): TestDataEnvironment {
        val legalEntityIdentifierTypes = metadataToCreate.legalEntityIdentifierTypeKeys.map { poolClient.metadata.createIdentifierType(createIdentifierType(it, IdentifierBusinessPartnerType.LEGAL_ENTITY)) }
        val addressIdentifierTypes = metadataToCreate.addressIdentifierTypeKeys.map { poolClient.metadata.createIdentifierType(createIdentifierType(it, IdentifierBusinessPartnerType.ADDRESS)) }
        val legalForms = poolClient.metadata.getLegalForms(PaginationRequest()).content.toList()
        val adminAreas = poolClient.metadata.getAdminAreasLevel1(PaginationRequest()).content.toList()

        val testMetadata = TestMetadata(legalForms, legalEntityIdentifierTypes, addressIdentifierTypes, adminAreas)

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

    private fun createIdentifierType(seed: String, type: IdentifierBusinessPartnerType): IdentifierTypeDto {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)
        return IdentifierTypeDto(
            technicalKey = seed,
            businessPartnerType = type,
            name = "Identifier Name $seed",
            abbreviation = "Identifier Abbreviation $seed",
            transliteratedName = "Identifier Transliterated Name $seed",
            transliteratedAbbreviation = "Identifier Transliterated Abbreviation $seed",
            format = "Identifier Formart $seed",
            categories = (1 .. 3).map { IdentifierTypeCategory.entries.random(random) }.toSortedSet(),
            listOf(
                IdentifierTypeDetailDto(country = CountryCode.entries.random(random), false),
                IdentifierTypeDetailDto(country = CountryCode.entries.random(random), false)
            )
        )
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
    val adminAreas: List<CountrySubdivisionDto>
)

data class TestMetadataKeys(
    val legalEntityIdentifierTypeKeys: List<String>,
    val addressIdentifierTypeKeys: List<String>
)