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

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.pool.api.model.*
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
                IdentifierTypeDto("idType1", IdentifierBusinessPartnerType.LEGAL_ENTITY, "idType1", null, null, null, null, sortedSetOf(), emptyList()),
                IdentifierTypeDto("idType2", IdentifierBusinessPartnerType.LEGAL_ENTITY, "idType2", null, null, null, null, sortedSetOf(), emptyList()),
                IdentifierTypeDto("idType3", IdentifierBusinessPartnerType.LEGAL_ENTITY, "idType3", null, null, null, null, sortedSetOf(), emptyList())
            ),
            addressIdentifierTypes = listOf(
                IdentifierTypeDto("addressIdType1", IdentifierBusinessPartnerType.ADDRESS, "addressIdType1", null, null, null, null, sortedSetOf(), emptyList()),
                IdentifierTypeDto("addressIdType2", IdentifierBusinessPartnerType.ADDRESS, "addressIdType2", null, null, null, null, sortedSetOf(), emptyList()),
                IdentifierTypeDto("addressIdType3", IdentifierBusinessPartnerType.ADDRESS, "addressIdType3", null, null, null, null, sortedSetOf(), emptyList())
            ),
            adminAreas = listOf(
                CountrySubdivisionDto(CountryCode.DE, "adminArea1", "adminArea1"),
                CountrySubdivisionDto(CountryCode.US, "adminArea2", "adminArea2"),
                CountrySubdivisionDto(CountryCode.CN, "adminArea3", "adminArea3"),
            ),
            scriptCodes = listOf(
                ScriptCodeDto("Test", "Test Description")
            ),
            reasonCodes = listOf(
                ReasonCodeDto("HEADQUARTER_RELOCATION", "Test Reason"),
                ReasonCodeDto("OTHER", "Another Test Reason"),
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