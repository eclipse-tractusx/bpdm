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

package org.eclipse.tractusx.bpdm.pool.config

import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.ScriptCodeDto
import org.eclipse.tractusx.bpdm.pool.service.TaskBatchResolutionService
import org.eclipse.tractusx.bpdm.pool.util.metadata.AdminAreaLevel1EntryImporter
import org.eclipse.tractusx.bpdm.pool.util.metadata.IdentifierTypeEntryImporter
import org.eclipse.tractusx.bpdm.pool.util.metadata.LegalFormEntryImporter
import org.eclipse.tractusx.bpdm.pool.v7.util.AdminAreaLevel1V7Importer
import org.eclipse.tractusx.bpdm.pool.v7.util.IdentifierTypeV7Importer
import org.eclipse.tractusx.bpdm.pool.v7.util.LegalFormV7Importer
import org.eclipse.tractusx.bpdm.pool.v7.util.TestDataClientV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.BusinessPartnerTestDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorRequestFactoryCommon
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorRequestFactoryV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.RefinementTestDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.TestMetadataReferences
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolDataHelper
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestMetadataV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.PoolRequestFactoryV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.PoolResponseFactoryV7
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper

@Configuration
class TestDataV7Configuration {

    /**
     * We create [PoolDataHelper] here with standard metadata to create test data environments from
     * For more specialized environments you could also not autowire it and  instead create the [PoolDataHelper] in the test class itself
     */
    @Bean
    fun poolDataHelper(poolClient: PoolApiClient): PoolDataHelper {
        return PoolDataHelper(poolClient)
    }

    @Bean
    fun testMetadataV7(
        legalFormEntryImporter: LegalFormEntryImporter,
        adminAreaLevel1EntryImporter: AdminAreaLevel1EntryImporter,
        identifierTypeEntryImporter: IdentifierTypeEntryImporter
    ): TestMetadataV7{
        val legalFormV7Importer = LegalFormV7Importer(legalFormEntryImporter)
        val adminAreaLevel1V7Importer = AdminAreaLevel1V7Importer(adminAreaLevel1EntryImporter)
        val identifierTypeV7Importer = IdentifierTypeV7Importer(identifierTypeEntryImporter)

        val legalForms = legalFormV7Importer.importFromResource()
        val adminAreas = adminAreaLevel1V7Importer.importFromResource()
        val identifierTypes = identifierTypeV7Importer.importFromResource()
        val scriptCodes = listOf(
            ScriptCodeDto("CHINESE_SIMPLIFIED", "Simplified Chinese characters"),
            ScriptCodeDto("CHINESE_TRADITIONAL", "Traditional Chinese characters"),
            ScriptCodeDto("KANJI", "Japanese Characters (Hiragana, Katakana and Kanji)")
        )

        return TestMetadataV7(
            legalForms = legalForms,
            legalEntityIdentifierTypes = identifierTypes.filter { it.businessPartnerType == IdentifierBusinessPartnerType.LEGAL_ENTITY },
            addressIdentifierTypes = identifierTypes.filter { it.businessPartnerType == IdentifierBusinessPartnerType.ADDRESS },
            adminAreas = adminAreas,
            scriptCodes = scriptCodes
        )
    }

    @Bean
    fun requestFactoryV7(testMetadata: TestMetadataV7): PoolRequestFactoryV7{
        return PoolRequestFactoryV7(testMetadata)
    }

    @Bean
    fun responseFactoryV7(testMetadata: TestMetadataV7): PoolResponseFactoryV7{
        return PoolResponseFactoryV7(testMetadata)
    }

    @Bean
    fun refinementTestDataFactory(): RefinementTestDataFactory{
        return RefinementTestDataFactory()
    }

    @Bean
    fun orchestratorMockDataFactory(refinementTestDataFactory: RefinementTestDataFactory, jsonMapper: JsonMapper): OrchestratorMockDataFactory{
        return OrchestratorMockDataFactory(refinementTestDataFactory, jsonMapper)
    }

    @Bean
    fun orchestratorRequestFactoryCommon(testMetadataV7: TestMetadataV7): OrchestratorRequestFactoryCommon{
        val orchestratorMetadata = TestMetadataReferences(
            legalForms = testMetadataV7.legalForms.map { it.technicalKey },
            legalEntityIdentifierTypes = testMetadataV7.legalEntityIdentifierTypes.map { it.technicalKey },
            addressIdentifierTypes = testMetadataV7.addressIdentifierTypes.map { it.technicalKey },
            adminAreas = testMetadataV7.adminAreas.map { it.code },
            scriptCodes = testMetadataV7.scriptCodes.map { it.technicalKey }
        )
        return OrchestratorRequestFactoryCommon(orchestratorMetadata)
    }

    @Bean
    fun businessPartnerTestDataFactory(orchestratorRequestFactoryCommon: OrchestratorRequestFactoryCommon): BusinessPartnerTestDataFactory {
        return BusinessPartnerTestDataFactory(orchestratorRequestFactoryCommon)
    }

    @Bean
    fun orchestratorRequestFactory(
        businessPartnerTestDataFactory: BusinessPartnerTestDataFactory,
        orchestratorRequestFactoryCommon: OrchestratorRequestFactoryCommon
    ): OrchestratorRequestFactoryV7 {
        return OrchestratorRequestFactoryV7(businessPartnerTestDataFactory, orchestratorRequestFactoryCommon)
    }

    @Bean
    fun testDataClientV7(
        poolApiClient: PoolApiClient,
        requestFactory: PoolRequestFactoryV7,
        orchestratorMockDataFactory: OrchestratorMockDataFactory,
        taskBatchResolutionService: TaskBatchResolutionService
    ): TestDataClientV7{
        return TestDataClientV7(poolApiClient, requestFactory, orchestratorMockDataFactory, taskBatchResolutionService)
    }

}