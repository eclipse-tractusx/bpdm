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

package org.eclipse.tractusx.bpdm.orchestrator.config

import org.eclipse.tractusx.bpdm.common.util.BpdmWebClientProvider
import org.eclipse.tractusx.bpdm.test.config.SelfClientConfigProperties
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClientImpl
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OrchestratorClientConfig{
    @Bean
    fun orchestratorClient(
        webServerAppCtxt: ServletWebServerApplicationContext,
        selfClientConfigProperties: SelfClientConfigProperties,
        webClientProvider: BpdmWebClientProvider
    ): OrchestrationApiClient {
        return OrchestrationApiClientImpl {
            val properties = selfClientConfigProperties.copy(baseUrl = "http://localhost:${webServerAppCtxt.webServer.port}")
            webClientProvider.builder(properties).build()
        }
    }

}