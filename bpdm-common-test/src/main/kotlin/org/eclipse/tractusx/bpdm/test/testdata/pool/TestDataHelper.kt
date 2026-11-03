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
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v7.client.PoolV7Client
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.CountrySubdivisionV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.CountryV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierBusinessPartnerTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierTypeV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.LegalFormV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.ReasonCodeV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.ScriptCodeV7Dto

/**
 * This class provides functionality to create a [TestDataEnvironment] to support creating and comparing business partner data
 * Also it has functionality to easily create hierarchical business partner data
 */
class PoolDataHelper(
    private val poolClient: PoolV7Client,
    private val reasonCodes: List<ReasonCodeV7Dto>
) {

    fun createTestDataEnvironment(): TestDataEnvironment {
        val legalEntityIdentifierTypes = poolClient.metadata.getIdentifierTypes(PaginationRequest(), IdentifierBusinessPartnerTypeV7.LEGAL_ENTITY, null).content.toList()
        val addressIdentifierTypes = poolClient.metadata.getIdentifierTypes(PaginationRequest(), IdentifierBusinessPartnerTypeV7.ADDRESS, null).content.toList()
        val legalForms = poolClient.metadata.getLegalForms(PaginationRequest()).content.toList()
        val adminAreas = poolClient.metadata.getAdminAreasLevel1(PaginationRequest()).content.toList()
        val scriptCodes = poolClient.metadata.getScriptCodes(PaginationRequest()).content.toList()
        val countries = fetchAllCountries()

        val testMetadata = TestMetadataV7(legalForms, legalEntityIdentifierTypes, addressIdentifierTypes, adminAreas, scriptCodes, reasonCodes, countries)

        val requestFactory = BusinessPartnerRequestFactory(testMetadata)
        val expectedResultFactory = ExpectedBusinessPartnerResultFactory(testMetadata)

        return TestDataEnvironment(testMetadata, requestFactory, expectedResultFactory)
    }

    // Catalogue can exceed one page (max size 100), so every page is read
    private fun fetchAllCountries(): List<CountryV7Dto> {
        val firstPage = poolClient.metadata.getCountries(PaginationRequest(0, 100))
        return firstPage.content.toList() + (1 until firstPage.totalPages).flatMap { poolClient.metadata.getCountries(PaginationRequest(it, 100)).content }
    }
}
data class TestDataEnvironment(
    val metadata: TestMetadataV7,
    val requestFactory: BusinessPartnerRequestFactory,
    val expectFactory: ExpectedBusinessPartnerResultFactory
)

data class TestMetadataV7(
    val legalForms: List<LegalFormV7Dto>,
    val legalEntityIdentifierTypes: List<IdentifierTypeV7Dto>,
    val addressIdentifierTypes: List<IdentifierTypeV7Dto>,
    val adminAreas: List<CountrySubdivisionV7Dto>,
    val scriptCodes: List<ScriptCodeV7Dto>,
    val reasonCodes: List<ReasonCodeV7Dto>,
    val countries: List<CountryV7Dto> = emptyList()
)

fun List<CountryV7Dto>.toCountryVerbose(countryCode: String): TypeKeyNameVerboseDto<String> =
    TypeKeyNameVerboseDto(countryCode, find { it.countryCode == countryCode }?.name ?: countryCode)