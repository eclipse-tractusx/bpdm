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

package org.eclipse.tractusx.bpdm.test.containers

import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.*
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestMetadataV7
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

/**
 * Shared orchestrator mock bean configuration for modules that mock the Orchestrator (Gate, Pool).
 * Not auto-configured — consumers must explicitly @Import this class.
 * Requires a TestMetadataV7 bean to be provided by the importing module.
 */
@Configuration
class OrchestratorMockConfiguration {

    @Bean
    fun refinementTestDataFactory(): RefinementTestDataFactory = RefinementTestDataFactory()

    @Bean
    fun orchestratorExpectedResultFactoryV7(): OrchestratorExpectedResultFactoryV7 =
        OrchestratorExpectedResultFactoryV7(
            Duration.ofDays(1),
            Duration.ofDays(1),
            mapOf(
                TaskMode.UpdateFromPool to listOf(TaskStep.CleanAndSync, TaskStep.PoolSync),
                TaskMode.UpdateFromSharingMember to listOf(TaskStep.Clean)
            )
        )

    @Bean
    fun orchestratorRequestFactoryCommon(testMetadataV7: TestMetadataV7): OrchestratorRequestFactoryCommon {
        val metadata = TestMetadataReferences(
            legalForms = testMetadataV7.legalForms.map { it.technicalKey },
            legalEntityIdentifierTypes = testMetadataV7.legalEntityIdentifierTypes.map { it.technicalKey },
            addressIdentifierTypes = testMetadataV7.addressIdentifierTypes.map { it.technicalKey },
            adminAreas = testMetadataV7.adminAreas.map { it.code },
            scriptCodes = testMetadataV7.scriptCodes.map { it.technicalKey },
            reasonCodes = testMetadataV7.reasonCodes.map { it.technicalKey }
        )
        return OrchestratorRequestFactoryCommon(metadata)
    }

    @Bean
    fun businessPartnerTestDataFactory(orchestratorRequestFactoryCommon: OrchestratorRequestFactoryCommon): BusinessPartnerTestDataFactory =
        BusinessPartnerTestDataFactory(orchestratorRequestFactoryCommon)

    @Bean
    fun orchestratorRequestFactoryV7(
        businessPartnerTestDataFactory: BusinessPartnerTestDataFactory,
        orchestratorRequestFactoryCommon: OrchestratorRequestFactoryCommon
    ): OrchestratorRequestFactoryV7 =
        OrchestratorRequestFactoryV7(businessPartnerTestDataFactory, orchestratorRequestFactoryCommon)

    @Bean
    fun orchestratorMockDataFactory(
        refinementTestDataFactory: RefinementTestDataFactory,
        orchestratorRequestFactoryV7: OrchestratorRequestFactoryV7,
        orchestratorExpectedResultFactoryV7: OrchestratorExpectedResultFactoryV7,
        jsonMapper: JsonMapper
    ): OrchestratorMockDataFactory =
        OrchestratorMockDataFactory(refinementTestDataFactory, orchestratorRequestFactoryV7, orchestratorExpectedResultFactoryV7, jsonMapper)
}
