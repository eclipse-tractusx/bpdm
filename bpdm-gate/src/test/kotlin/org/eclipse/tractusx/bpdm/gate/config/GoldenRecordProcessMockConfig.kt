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

package org.eclipse.tractusx.bpdm.gate.config

import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.CountrySubdivisionV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.CountryV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierBusinessPartnerTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierTypeV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.ReasonCodeV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.ScriptCodeV7Dto
import org.eclipse.tractusx.bpdm.test.testdata.GoldenRecordMockFactory
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper

@Configuration
class GoldenRecordProcessMockConfig {

    @Bean
    fun testMetadataV7(): TestMetadataV7{
        return TestMetadataV7(
            legalForms = listOf(
                BusinessPartnerVerboseValues.legalForm1,
                BusinessPartnerVerboseValues.legalForm2
            ),
            legalEntityIdentifierTypes = listOf(
                IdentifierTypeV7Dto("idType1", IdentifierBusinessPartnerTypeV7.LEGAL_ENTITY, "idType1", null, null, null, null, sortedSetOf(), emptyList()),
                IdentifierTypeV7Dto("idType2", IdentifierBusinessPartnerTypeV7.LEGAL_ENTITY, "idType2", null, null, null, null, sortedSetOf(), emptyList()),
                IdentifierTypeV7Dto("idType3", IdentifierBusinessPartnerTypeV7.LEGAL_ENTITY, "idType3", null, null, null, null, sortedSetOf(), emptyList())
            ),
            addressIdentifierTypes = listOf(
                IdentifierTypeV7Dto("addressIdType1", IdentifierBusinessPartnerTypeV7.ADDRESS, "addressIdType1", null, null, null, null, sortedSetOf(), emptyList()),
                IdentifierTypeV7Dto("addressIdType2", IdentifierBusinessPartnerTypeV7.ADDRESS, "addressIdType2", null, null, null, null, sortedSetOf(), emptyList()),
                IdentifierTypeV7Dto("addressIdType3", IdentifierBusinessPartnerTypeV7.ADDRESS, "addressIdType3", null, null, null, null, sortedSetOf(), emptyList())
            ),
            adminAreas = listOf(
                CountrySubdivisionV7Dto("DE", "adminArea1", "adminArea1"),
                CountrySubdivisionV7Dto("US", "adminArea2", "adminArea2"),
                CountrySubdivisionV7Dto("CN", "adminArea3", "adminArea3"),
            ),
            countries = listOf(
                CountryV7Dto("DE", "Germany", null),
                CountryV7Dto("US", "United States", null),
                CountryV7Dto("CN", "China", null),
            ),
            scriptCodes = listOf(
                ScriptCodeV7Dto("Test", "Test Description")
            ),
            reasonCodes = listOf(
                ReasonCodeV7Dto("HEADQUARTER_RELOCATION", "Test Reason"),
                ReasonCodeV7Dto("OTHER", "Another Test Reason"),
            )
        )
    }

    @Bean
    fun poolRequestFactory(testMetadataV7: TestMetadataV7): BusinessPartnerRequestFactory {
        return BusinessPartnerRequestFactory(testMetadataV7)
    }

    @Bean
    fun poolMockDataFactory(
        jsonMapper: JsonMapper,
        testMetadataV7: TestMetadataV7
    ): PoolMockDataFactory {
        return PoolMockDataFactory(
            BusinessPartnerRequestFactory(testMetadataV7),
            ExpectedBusinessPartnerResultFactory(testMetadataV7),
            jsonMapper
        )
    }

    @Bean
    fun goldenRecordMockFactory(
        orchestratorMockDataFactory: OrchestratorMockDataFactory,
        poolMockDataFactory: PoolMockDataFactory
    ): GoldenRecordMockFactory {
        return GoldenRecordMockFactory(poolMockDataFactory, orchestratorMockDataFactory)
    }

}