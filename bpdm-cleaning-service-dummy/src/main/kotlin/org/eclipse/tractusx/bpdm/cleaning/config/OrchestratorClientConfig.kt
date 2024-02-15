/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.cleaning.config

import org.eclipse.tractusx.bpdm.common.util.BpdmWebClientProvider
import org.eclipse.tractusx.bpdm.common.util.ClientConfigurationProperties
import org.eclipse.tractusx.bpdm.common.util.ConditionalOnBoundProperty
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClientImpl
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository

@ConfigurationProperties(prefix = OrchestratorConfigProperties.PREFIX)
data class OrchestratorConfigProperties(
    override val baseUrl: String = "http://localhost:8085",
    override val securityEnabled: Boolean = false,
    override val oauth2ClientRegistration: String = "orchestrator-client"
) : ClientConfigurationProperties {
    companion object {
        const val PREFIX = "${ClientConfigurationProperties.PREFIX}.orchestrator"
    }
}

@Configuration
class OrchestratorClientConfiguration(
    clientProperties: OrchestratorConfigProperties,
) : BpdmWebClientProvider(
    clientProperties
) {
    @Bean
    @ConditionalOnBoundProperty(OrchestratorConfigProperties.PREFIX, OrchestratorConfigProperties::class, true)
    fun authorizedOrchestratorClient(
        clientRegistrationRepository: ClientRegistrationRepository,
        oAuth2AuthorizedClientService: OAuth2AuthorizedClientService
    ): OrchestrationApiClient =
        OrchestrationApiClientImpl { provideAuthorizedClient(clientRegistrationRepository, oAuth2AuthorizedClientService) }

    @Bean
    @ConditionalOnBoundProperty(OrchestratorConfigProperties.PREFIX, OrchestratorConfigProperties::class, false)
    fun unauthorizedOrchestratorClient(): OrchestrationApiClient =
        OrchestrationApiClientImpl { provideUnauthorizedClient() }
}
