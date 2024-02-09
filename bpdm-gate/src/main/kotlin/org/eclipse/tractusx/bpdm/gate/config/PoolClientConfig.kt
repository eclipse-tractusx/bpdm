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

package org.eclipse.tractusx.bpdm.gate.config

import org.eclipse.tractusx.bpdm.common.util.BpdmWebClientProvider
import org.eclipse.tractusx.bpdm.common.util.ClientConfigurationProperties
import org.eclipse.tractusx.bpdm.common.util.ConditionalOnBoundProperty
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository


@ConfigurationProperties(prefix = PoolClientConfigurationProperties.PREFIX)
data class PoolClientConfigurationProperties(
    override val baseUrl: String = "http://localhost:8080",
    val searchChangelogPageSize: Int = 100,
    override val securityEnabled: Boolean = false,
    override val oauth2ClientRegistration: String = "pool-client"
) : ClientConfigurationProperties {
    companion object {
        const val PREFIX = "${ClientConfigurationProperties.PREFIX}.pool"
    }

    override val enabled = securityEnabled
}

@Configuration
class PoolClientConfiguration(
    poolConfigProperties: PoolClientConfigurationProperties,
) : BpdmWebClientProvider(
    poolConfigProperties
) {
    @Bean
    @ConditionalOnBoundProperty(PoolClientConfigurationProperties.PREFIX, PoolClientConfigurationProperties::class, true)
    fun authorizedPoolClient(
        clientRegistrationRepository: ClientRegistrationRepository,
        oAuth2AuthorizedClientService: OAuth2AuthorizedClientService
    ): PoolApiClient = PoolClientImpl { provideAuthorizedClient(clientRegistrationRepository, oAuth2AuthorizedClientService) }

    @Bean
    @ConditionalOnBoundProperty(PoolClientConfigurationProperties.PREFIX, PoolClientConfigurationProperties::class, false)
    fun unauthorizedPoolClient(): PoolApiClient =
        PoolClientImpl { provideUnauthorizedClient() }
}

