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

import org.eclipse.tractusx.bpdm.gate.api.v6.client.GateClientV6
import org.eclipse.tractusx.bpdm.gate.service.TaskCreationBatchService
import org.eclipse.tractusx.bpdm.gate.service.TaskResolutionBatchService
import org.eclipse.tractusx.bpdm.gate.v6.util.GateTestDataClientV6
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.testdata.gate.GateInputFactory
import org.eclipse.tractusx.bpdm.test.testdata.gate.TestMetadata
import org.eclipse.tractusx.bpdm.test.testdata.gate.v6.ExpectedGateResultV6Factory
import org.eclipse.tractusx.bpdm.test.testdata.gate.v6.GateTestDataFactoryV6
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolMockDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestMetadataV7
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GateTestDataV6Config {

    @Bean
    fun testDataFactoryV6(testMetadataV7: TestMetadataV7): GateTestDataFactoryV6 {
        return GateTestDataFactoryV6(
            GateInputFactory(
                testMetadata = TestMetadata(
                    identifierTypes = testMetadataV7.legalEntityIdentifierTypes.map { it.technicalKey },
                    legalForms = testMetadataV7.legalForms.map { it.technicalKey },
                    adminAreas = testMetadataV7.adminAreas.map { it.code },
                    reasonCodes = testMetadataV7.reasonCodes.map { it.technicalKey }
                ),
                testRunData = null
            ),
            ExpectedGateResultV6Factory()
        )
    }

    @Bean
    fun gateTestDataClientV6(
        testDataFactory: GateTestDataFactoryV6,
        operatorClient: GateClientV6,
        orchestratorMockDataFactory: OrchestratorMockDataFactory,
        taskCreationBatchService: TaskCreationBatchService,
        taskResolutionBatchService: TaskResolutionBatchService,
        poolMockDataFactory: PoolMockDataFactory
    ): GateTestDataClientV6 {
        return GateTestDataClientV6(
            testDataFactory,
            operatorClient,
            orchestratorMockDataFactory,
            taskCreationBatchService,
            taskResolutionBatchService,
            KeyCloakInitializer.TENANT_BPNL,
            poolMockDataFactory
        )
    }
}