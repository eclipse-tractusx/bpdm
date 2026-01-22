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

package org.eclipse.tractusx.bpdm.common.config

import org.eclipse.tractusx.bpdm.common.util.*
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientPropertiesMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository

@Conditional(ActiveClientsConfiguredCondition::class)
@EnableConfigurationProperties(OAuth2ClientProperties::class)
@Configuration
class BpdmOAuth2ClientRegistrationConfig {

    @Bean
    fun clientRegistrationRepository(clientsConfigProperties: List<BpdmClientProperties>, oauth2Properties: OAuth2ClientProperties): InMemoryClientRegistrationRepository {
        clientsConfigProperties .forEach { if(it.registration.provider == null) it.registration.provider = it.getId()  }

        clientsConfigProperties
            .filter { it.securityEnabled }
            .forEach { oauth2Properties.registration[it.getId()] = it.registration }

        clientsConfigProperties
            .filter { it.securityEnabled }
            .forEach{ oauth2Properties.provider[it.registration.provider!!] = it.provider }

        val registrations: List<ClientRegistration?> = OAuth2ClientPropertiesMapper(oauth2Properties).asClientRegistrations().values.toList()
        return InMemoryClientRegistrationRepository(registrations)
    }
}

@Configuration
class BpdmWebClientProviderConfig(){

    @Bean
    fun bpdmWebClientProvider(repository: ClientRegistrationRepository?, clientService: OAuth2AuthorizedClientService?): BpdmWebClientProvider{
        return if(repository != null && clientService != null)
            BpdmOAuth2WebClientProvider(BpdmUnauthorizedWebClientProvider(), AuthorizedClientServiceOAuth2AuthorizedClientManager(repository, clientService))
        else
             BpdmUnauthorizedWebClientProvider()
    }

}

