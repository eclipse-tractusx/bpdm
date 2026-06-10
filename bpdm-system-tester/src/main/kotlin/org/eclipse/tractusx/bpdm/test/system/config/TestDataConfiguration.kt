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

package org.eclipse.tractusx.bpdm.test.system.config

import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.ReasonCodeDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.ReasonCodeUpsertRequest
import org.eclipse.tractusx.bpdm.test.system.utils.BusinessPartnerRelationTestDataGenerator
import org.eclipse.tractusx.bpdm.test.system.utils.BusinessPartnerShareActions
import org.eclipse.tractusx.bpdm.test.system.utils.ConfidenceAssertHelper
import org.eclipse.tractusx.bpdm.test.system.utils.GateOutputFactory
import org.eclipse.tractusx.bpdm.test.system.utils.GoldenRecordRelationAssertHelper
import org.eclipse.tractusx.bpdm.test.system.utils.ShareOwnCompanyDataTestDataGenerator
import org.eclipse.tractusx.bpdm.test.system.utils.SharingStateWatcher
import org.eclipse.tractusx.bpdm.test.system.utils.StepUtils
import org.eclipse.tractusx.bpdm.test.system.utils.TaskReservationWatcher
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.bpdm.test.testdata.gate.GateInputFactory
import org.eclipse.tractusx.bpdm.test.testdata.gate.TestMetadata
import org.eclipse.tractusx.bpdm.test.testdata.gate.TestRunData
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.BusinessPartnerInputDtoV7Factory
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.BusinessPartnerInputRequestV7Factory
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.BusinessPartnerOutputDtoV7Factory
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.GateAssertRepositoryV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.GateTestMetadataV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.PageChangeLogV7Factory
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.RelationInputRequestV7Factory
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.RelationOutputDtoV7Factory
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.RefinementTestDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolDataHelper
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestMetadataV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.PoolRequestFactoryV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.PoolResponseFactoryV7
import org.eclipse.tractusx.bpdm.test.util.InstantSecondsComparator
import org.eclipse.tractusx.bpdm.test.util.LocalDatetimeSecondsComparator
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

@Configuration
class TestDataConfiguration {

    @Bean
    fun testMetadata(): TestMetadata {
        val testMetadata = TestMetadata(
            identifierTypes = listOf("EU_VAT_ID_DE", "DUNS_ID"),
            legalForms = listOf("SCE1", "SGST"),
            adminAreas = listOf("DE-BW", "DE-BY"),
            reasonCodes = listOf("HEADQUARTER_RELOCATION")
        )


        return testMetadata
    }

    @Bean
    fun gateTestDataFactory(testMetadata: TestMetadata, testRunData: TestRunData): GateInputFactory {
        return GateInputFactory(testMetadata, testRunData)
    }

    @Bean
    fun gateOutputFactory(gateInputDataFactory: GateInputFactory): GateOutputFactory {
        return GateOutputFactory(gateInputDataFactory)
    }

    @Bean
    fun testRunData(): TestRunData {
        return TestRunData(Instant.now())
    }

    @Bean
    fun stepUtils(testRunData: TestRunData, gateClient: GateClient): StepUtils{
        return StepUtils(gateClient)
    }

    @Bean
    fun sharingStateWatcher(gateClient: GateClient): SharingStateWatcher {
        return SharingStateWatcher(gateClient)
    }

    @Bean
    fun taskReservationWatcher(orchestrationApiClient: OrchestrationApiClient): TaskReservationWatcher {
        return TaskReservationWatcher(orchestrationApiClient)
    }

    @Bean
    fun testMetadataV7(poolClient: PoolApiClient): TestMetadataV7 {
        val poolDataHelper = PoolDataHelper(poolClient, listOf(
            ReasonCodeDto("REASON_CODE_1", "REASON_CODE_1 description"),
            ReasonCodeDto("REASON_CODE_2", "REASON_CODE_2 description"),
        ))
        return poolDataHelper.createTestDataEnvironment().metadata
    }

    @Bean
    fun poolRequestFactoryV7(testMetadataV7: TestMetadataV7): PoolRequestFactoryV7 {
        return PoolRequestFactoryV7(testMetadataV7)
    }

    @Bean
    fun poolResponseFactoryV7(testMetadataV7: TestMetadataV7): PoolResponseFactoryV7 {
        return PoolResponseFactoryV7(testMetadataV7)
    }

    @Bean
    fun refinementTestDataFactory(): RefinementTestDataFactory {
        return RefinementTestDataFactory()
    }

    @Bean
    fun gateTestMetadataV7(): GateTestMetadataV7 {
        return GateTestMetadataV7(
            identifierTypes = listOf("EU_VAT_ID_DE", "DUNS_ID"),
            legalForms = listOf("SCE1", "SGST"),
            adminAreas = listOf("DE-BW", "DE-BY"),
            scriptVariants = listOf("CHINESE_SIMPLIFIED", "CHINESE_TRADITIONAL", "KANJI", "HANGUL"),
            reasonCodes = listOf("REASON_CODE_1", "REASON_CODE_2"),
        )
    }

    @Bean
    fun businessPartnerInputRequestV7Factory(gateTestMetadataV7: GateTestMetadataV7): BusinessPartnerInputRequestV7Factory {
        return BusinessPartnerInputRequestV7Factory(gateTestMetadataV7)
    }

    @Bean
    fun businessPartnerInputDtoV7Factory(): BusinessPartnerInputDtoV7Factory {
        return BusinessPartnerInputDtoV7Factory()
    }

    @Bean
    fun businessPartnerOutputDtoV7Factory(): BusinessPartnerOutputDtoV7Factory {
        return BusinessPartnerOutputDtoV7Factory()
    }

    @Bean
    fun relationInputRequestV7Factory(gateTestMetadataV7: GateTestMetadataV7): RelationInputRequestV7Factory {
        return RelationInputRequestV7Factory(gateTestMetadataV7)
    }

    @Bean
    fun relationOutputDtoV7Factory(): RelationOutputDtoV7Factory {
        return RelationOutputDtoV7Factory()
    }

    @Bean
    fun pageChangeLogV7Factory(): PageChangeLogV7Factory {
        return PageChangeLogV7Factory()
    }

    @Bean
    fun testDataFactoryGateV7(
        businessPartnerInputRequestV7Factory: BusinessPartnerInputRequestV7Factory,
        businessPartnerInputDtoV7Factory: BusinessPartnerInputDtoV7Factory,
        businessPartnerOutputDtoV7Factory: BusinessPartnerOutputDtoV7Factory,
        relationInputRequestV7Factory: RelationInputRequestV7Factory,
        relationOutputDtoV7Factory: RelationOutputDtoV7Factory,
        pageChangeLogV7Factory: PageChangeLogV7Factory
    ): TestDataFactoryGateV7 {
        return TestDataFactoryGateV7(
            businessPartnerInputRequestV7Factory,
            businessPartnerInputDtoV7Factory,
            businessPartnerOutputDtoV7Factory,
            relationInputRequestV7Factory,
            relationOutputDtoV7Factory,
            pageChangeLogV7Factory
        )
    }

    @Bean
    fun shareOwnCompanyDataTestDataGenerator(
        poolRequestFactoryV7: PoolRequestFactoryV7,
        poolResponseFactoryV7: PoolResponseFactoryV7,
        refinementTestDataFactory: RefinementTestDataFactory,
        testDataFactoryGateV7: TestDataFactoryGateV7
    ): ShareOwnCompanyDataTestDataGenerator {
        return ShareOwnCompanyDataTestDataGenerator(
            poolRequestFactoryV7,
            poolResponseFactoryV7,
            refinementTestDataFactory,
            testDataFactoryGateV7
        )
    }

    @Bean
    fun businessPartnerRelationTestDataGenerator(
        testDataFactoryGateV7: TestDataFactoryGateV7
    ): BusinessPartnerRelationTestDataGenerator {
        return BusinessPartnerRelationTestDataGenerator(testDataFactoryGateV7)
    }

    @Bean
    fun gateAssertRepositoryV7(): GateAssertRepositoryV7{
        val instantSecondsComparator = InstantSecondsComparator()
        val localDatetimeSecondsComparator = LocalDatetimeSecondsComparator(instantSecondsComparator)
        return GateAssertRepositoryV7(instantSecondsComparator, localDatetimeSecondsComparator)
    }

    @Bean
    fun confidenceAssertHelper(
        gateClient: GateClient,
        jsonMapper: JsonMapper
    ): ConfidenceAssertHelper {
        return ConfidenceAssertHelper(gateClient, jsonMapper)
    }

    @Bean
    fun goldenRecordRelationAssertHelper(
        gateClient: GateClient,
        jsonMapper: JsonMapper
    ): GoldenRecordRelationAssertHelper {
        return GoldenRecordRelationAssertHelper(gateClient, jsonMapper)
    }

    @Bean
    fun businessPartnerShareActions(
        gateClient: GateClient,
        orchestratorClient: OrchestrationApiClient,
        testDataGenerator: ShareOwnCompanyDataTestDataGenerator,
        sharingStateWatcher: SharingStateWatcher,
        taskReservationWatcher: TaskReservationWatcher,
        jsonMapper: JsonMapper
    ): BusinessPartnerShareActions{
        return BusinessPartnerShareActions(
            gateClient,
            orchestratorClient,
            testDataGenerator,
            sharingStateWatcher,
            taskReservationWatcher,
            jsonMapper
        )
    }
}