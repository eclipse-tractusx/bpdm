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

package com.catenax.bpdm.bridge.dummy.config

import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.client.GateClientImpl
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
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

    // Pool-Client without authentication
    @Bean
    @ConditionalOnProperty(
        value = ["bpdm.pool.security-enabled"],
        havingValue = "false",
        matchIfMissing = true
    )
    fun poolClientNoAuth(poolConfigProperties: PoolConfigProperties): PoolApiClient {
        val url = poolConfigProperties.baseUrl
        return PoolClientImpl { webClientBuilder(url).build() }
    }

    // Gate-Client without authentication
    @Bean
    @ConditionalOnProperty(
        value = ["bpdm.gate.security-enabled"],
        havingValue = "false",
        matchIfMissing = true
    )
    fun gateClientNoAuth(gateConfigProperties: GateConfigProperties): GateClient {
        val url = gateConfigProperties.baseUrl
        return GateClientImpl { webClientBuilder(url).build() }
    }

    // Pool-Client with OAuth2 authentication
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

    // Gate-Client with OAuth2 authentication
    @Bean
    @ConditionalOnProperty(
        value = ["bpdm.gate.security-enabled"],
        havingValue = "true"
    )
    fun gateClientWithAuth(
        gateConfigProperties: GateConfigProperties,
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientService: OAuth2AuthorizedClientService
    ): GateClient {
        val url = gateConfigProperties.baseUrl
        val clientRegistrationId = gateConfigProperties.oauth2ClientRegistration
            ?: throw IllegalArgumentException("bpdm.gate.oauth2-client-registration is required if bpdm.gate.security-enabled is set")
        return GateClientImpl {
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