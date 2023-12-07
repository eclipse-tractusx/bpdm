/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClientImpl
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import java.util.function.Consumer


@Configuration
class ClientsConfig {

    // Orchestrator-Client without authentication
    @Bean
    @ConditionalOnProperty(
        value = ["bpdm.client.orchestrator.security-enabled"],
        havingValue = "false",
        matchIfMissing = true
    )
    fun orchestratorClientNoAuth(orchestratorConfigProperties: OrchestratorConfigProperties): OrchestrationApiClient {
        val url = orchestratorConfigProperties.baseUrl
        return OrchestrationApiClientImpl { webClientBuilder(url).build() }
    }

    @Bean
    @ConditionalOnProperty(
        value = ["bpdm.gate-security.pool-security-enabled"],
        havingValue = "false",
        matchIfMissing = true
    )
    fun poolClientNoAuth(poolConfigProperties: PoolConfigProperties): PoolApiClient {
        val url = poolConfigProperties.baseUrl
        return PoolClientImpl {
            WebClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build()
        }
    }



    @Bean
    @ConditionalOnProperty(
        value = ["bpdm.client.orchestrator.security-enabled"],
        havingValue = "true"
    )
    fun orchestratorClientWithAuth(
        orchestratorConfigProperties: OrchestratorConfigProperties,
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientService: OAuth2AuthorizedClientService
    ): OrchestrationApiClient {
        val url = orchestratorConfigProperties.baseUrl
        val clientRegistrationId = orchestratorConfigProperties.oauth2ClientRegistration
            ?: throw IllegalArgumentException("bpdm.orchestrator.oauth2-client-registration is required if bpdm.client.orchestrator.security-enabled is set")
        return OrchestrationApiClientImpl {
            webClientBuilder(url)
                .apply(oauth2Configuration(clientRegistrationRepository, authorizedClientService, clientRegistrationId))
                .build()
        }
    }

    @Bean
    @ConditionalOnProperty(
        value = ["bpdm.pool.security-enabled"],
        havingValue = "true"
    )
    fun poolClientWithAuth(
        poolConfigProperties: PoolConfigProperties,
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientService: OAuth2AuthorizedClientService
    ): PoolApiClient {
        val url = poolConfigProperties.baseUrl
        val clientRegistrationId = poolConfigProperties.oauth2ClientRegistration
            ?: throw IllegalArgumentException("bpdm.pool.oauth2-client-registration is required if bpdm.pool.security-enabled is set")
        return PoolClientImpl {
            webClientBuilder(url)
                .apply(oauth2Configuration(clientRegistrationRepository, authorizedClientService, clientRegistrationId))
                .build()
        }
    }

    private fun webClientBuilder(url: String) =
        WebClient.builder()
            .baseUrl(url)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)

    private fun oauth2Configuration(
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientService: OAuth2AuthorizedClientService,
        clientRegistrationId: String
    ): Consumer<WebClient.Builder> {
        val authorizedClientManager = authorizedClientManager(clientRegistrationRepository, authorizedClientService)
        val oauth = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
        oauth.setDefaultClientRegistrationId(clientRegistrationId)
        return oauth.oauth2Configuration()
    }

    private fun authorizedClientManager(
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientService: OAuth2AuthorizedClientService
    ): OAuth2AuthorizedClientManager {
        val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
        val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService)
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
        return authorizedClientManager
    }

}