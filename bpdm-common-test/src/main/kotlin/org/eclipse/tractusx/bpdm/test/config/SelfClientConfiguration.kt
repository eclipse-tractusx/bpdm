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

package org.eclipse.tractusx.bpdm.test.config

import org.eclipse.tractusx.bpdm.common.util.ClientConfigurationProperties
import org.eclipse.tractusx.bpdm.common.util.ConditionalOnBoundProperty
import org.eclipse.tractusx.bpdm.common.util.HasEnablingProperty
import org.eclipse.tractusx.bpdm.test.util.BpdmOAuth2ClientFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository


@ConfigurationProperties(prefix = SelfClientConfigProperties.PREFIX)
data class SelfClientConfigProperties(
    val securityEnabled: Boolean = false,
    val oauth2ClientRegistration: String = "self-client"
) : HasEnablingProperty {
    companion object {
        const val PREFIX = "${ClientConfigurationProperties.PREFIX}.self"
    }

    override val enabled: Boolean
        get() = securityEnabled
}

@Configuration
class SelfClientConfiguration{

    @Bean
    @ConditionalOnBoundProperty(SelfClientConfigProperties.PREFIX, SelfClientConfigProperties::class, true)
    fun createAuth2ClientFactory(clientRegistrationRepository: ClientRegistrationRepository,
                                 oAuth2AuthorizedClientService: OAuth2AuthorizedClientService
    ): BpdmOAuth2ClientFactory{
        return BpdmOAuth2ClientFactory(clientRegistrationRepository, oAuth2AuthorizedClientService)
    }

}