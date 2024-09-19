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

package org.eclipse.tractusx.bpdm.test.system.config

import org.eclipse.tractusx.bpdm.common.util.BpdmClientProperties
import org.eclipse.tractusx.bpdm.common.util.BpdmWebClientProvider
import org.eclipse.tractusx.bpdm.common.util.ClientConfigurationProperties
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@ConfigurationProperties(prefix = PoolClientConfigurationProperties.PREFIX)
data class PoolClientConfigurationProperties(
    override val baseUrl: String = "http://localhost:8080",
    val searchChangelogPageSize: Int = 100,
    override val securityEnabled: Boolean = false,
    override val registration: OAuth2ClientProperties.Registration,
    override val provider: OAuth2ClientProperties.Provider
) : BpdmClientProperties {
    companion object {
        const val PREFIX = "${ClientConfigurationProperties.PREFIX}.pool"
    }

    override fun getId() = PREFIX
}

@Configuration
class PoolClientConfiguration{

    @Bean
    fun poolClient(webClientProvider: BpdmWebClientProvider, properties: PoolClientConfigurationProperties): PoolApiClient{
     return PoolClientImpl { webClientProvider.builder(properties).build() }
    }
}

