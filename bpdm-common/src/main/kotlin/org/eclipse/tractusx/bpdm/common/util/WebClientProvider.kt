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

package org.eclipse.tractusx.bpdm.common.util

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient


interface BpdmWebClientProvider{
    fun builder(properties: BpdmClientCreateProperties): WebClient.Builder

    fun builder(properties: BpdmClientProperties): WebClient.Builder{
        return builder(BpdmClientCreateProperties(
            registrationId = properties.getId(),
            baseUrl = properties.baseUrl,
            securityEnabled = properties.securityEnabled
        ))
    }
}

class BpdmOAuth2WebClientProvider(
    private val bpdmWebClientProvider: BpdmWebClientProvider,
    private val authorizedClientManager: OAuth2AuthorizedClientManager
): BpdmWebClientProvider{
    override fun builder(properties: BpdmClientCreateProperties): WebClient.Builder {
        return if(properties.securityEnabled) {
            val oAuth2ExchangeFilter = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
            oAuth2ExchangeFilter.setDefaultClientRegistrationId(properties.registrationId)
            oAuth2ExchangeFilter.setDefaultOAuth2AuthorizedClient(true)

            bpdmWebClientProvider.builder(properties).apply(oAuth2ExchangeFilter.oauth2Configuration())
        }else{
            bpdmWebClientProvider.builder(properties)
        }
    }
}

class BpdmUnauthorizedWebClientProvider: BpdmWebClientProvider{
    override fun builder(properties: BpdmClientCreateProperties): WebClient.Builder {
        return WebClient.builder()
            .baseUrl(properties.baseUrl)
            .codecs { codecs -> codecs.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    }
}

data class BpdmClientCreateProperties(
    val registrationId: String,
    val baseUrl: String,
    val securityEnabled: Boolean
)