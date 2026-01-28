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

package org.eclipse.tractusx.bpdm.orchestrator.v6.config

import org.eclipse.tractusx.bpdm.orchestrator.config.StateMachineConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.v6.util.OrchestratorAssertRepositoryV6
import org.eclipse.tractusx.bpdm.orchestrator.v6.util.OrchestratorTestDataClientV6
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorExpectedResultFactoryV6
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorRequestFactoryCommon
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorRequestFactoryV6
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.TestMetadataReferences
import org.eclipse.tractusx.orchestrator.api.v6.client.OrchestratorApiClientV6
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["test.v6.enabled"], havingValue = "true", matchIfMissing = false)
class OrchestratorTestDataV6Config {

    @Bean
    fun orchestratorRequestFactoryV6(): OrchestratorRequestFactoryV6 {
        val metadata = TestMetadataReferences(
            legalForms = listOf("LF1", "LF2", "LF3"),
            legalEntityIdentifierTypes = listOf("LEID1", "LEID2", "LEID3"),
            addressIdentifierTypes = listOf("ADDID1", "ADDID2", "ADDID3"),
            adminAreas = listOf("ADMINAREA1", "ADMINAREA2", "ADMINAREA3")
        )
        val commonRequestFactory = OrchestratorRequestFactoryCommon(metadata)
        return OrchestratorRequestFactoryV6(commonRequestFactory)
    }

    @Bean
    fun orchestratorExpectedResultFactoryV6(
        taskConfigProperties: TaskConfigProperties,
        stateMachineConfigProperties: StateMachineConfigProperties
    ): OrchestratorExpectedResultFactoryV6 {
        return OrchestratorExpectedResultFactoryV6(
            taskConfigProperties.taskPendingTimeout,
            taskConfigProperties.taskRetentionTimeout,
            stateMachineConfigProperties.modeSteps
        )
    }

    @Bean
    fun orchestratorAssertRepository(): OrchestratorAssertRepositoryV6{
        return OrchestratorAssertRepositoryV6()
    }

    @Bean
    fun orchestratorTestDataClientV6(
        orchestratorClient: OrchestratorApiClientV6,
        requestFactory: OrchestratorRequestFactoryV6
    ): OrchestratorTestDataClientV6{
        return OrchestratorTestDataClientV6(
            orchestratorClient,
            requestFactory
        )
    }



}