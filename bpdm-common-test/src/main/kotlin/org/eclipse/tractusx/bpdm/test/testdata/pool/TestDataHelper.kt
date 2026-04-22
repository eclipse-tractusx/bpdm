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

/**
 * This class provides functionality to create a [TestDataEnvironment] to support creating and comparing business partner data
 * Also it has functionality to easily create hierarchical business partner data
 */
class PoolDataHelper(
    private val poolClient: PoolApiClient,
) {

    fun createTestDataEnvironment(): TestDataEnvironment {
        val legalEntityIdentifierTypes = poolClient.metadata.getIdentifierTypes(PaginationRequest(), IdentifierBusinessPartnerType.LEGAL_ENTITY, null).content.toList()
        val addressIdentifierTypes = poolClient.metadata.getIdentifierTypes(PaginationRequest(), IdentifierBusinessPartnerType.ADDRESS, null).content.toList()
        val legalForms = poolClient.metadata.getLegalForms(PaginationRequest()).content.toList()
        val adminAreas = poolClient.metadata.getAdminAreasLevel1(PaginationRequest()).content.toList()
        val scriptCodes = poolClient.metadata.getScriptCodes(PaginationRequest()).content.toList()

        val testMetadata = TestMetadataV7(legalForms, legalEntityIdentifierTypes, addressIdentifierTypes, adminAreas, scriptCodes)

        val requestFactory = BusinessPartnerRequestFactory(testMetadata)
        val expectedResultFactory = ExpectedBusinessPartnerResultFactory(testMetadata)

        return TestDataEnvironment(testMetadata, requestFactory, expectedResultFactory)
    }
}
data class TestDataEnvironment(
    val metadata: TestMetadataV7,
    val requestFactory: BusinessPartnerRequestFactory,
    val expectFactory: ExpectedBusinessPartnerResultFactory
)

data class TestMetadataV7(
    val legalForms: List<LegalFormDto>,
    val legalEntityIdentifierTypes: List<IdentifierTypeDto>,
    val addressIdentifierTypes: List<IdentifierTypeDto>,
    val adminAreas: List<CountrySubdivisionDto>,
    val scriptCodes: List<ScriptCodeDto>
)