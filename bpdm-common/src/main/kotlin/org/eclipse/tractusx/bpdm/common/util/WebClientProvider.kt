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
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers


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
            bpdmWebClientProvider.builder(properties)
                .filter(clientCredentialsBearerTokenFilter(properties.registrationId))
        }else{
            bpdmWebClientProvider.builder(properties)
        }
    }

    /**
     * Attaches a client_credentials access token to every request.
     *
     * We deliberately do not use Spring's ServletOAuth2AuthorizedClientExchangeFilterFunction here: since Spring Security 7.1
     * it short-circuits (no token, no error) whenever there is no bound HttpServletRequest/HttpServletResponse. Our clients are
     * also driven from non-servlet threads (scheduled sync/cleaning jobs), where that context is absent, so it would silently
     * emit unauthenticated requests. The AuthorizedClientServiceOAuth2AuthorizedClientManager only needs a principal to perform
     * a client_credentials grant, so we resolve the token ourselves with a fixed anonymous principal.
     */
    private fun clientCredentialsBearerTokenFilter(registrationId: String): ExchangeFilterFunction {
        val principal = AnonymousAuthenticationToken(
            "bpdm-client", "bpdm-client",
            AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        )
        return ExchangeFilterFunction { request, next ->
            Mono.fromSupplier {
                authorizedClientManager.authorize(
                    OAuth2AuthorizeRequest.withClientRegistrationId(registrationId).principal(principal).build()
                ) ?: throw IllegalStateException("client_credentials authorization failed for registration '$registrationId'")
            }
                .subscribeOn(Schedulers.boundedElastic())
                .map { authorizedClient ->
                    ClientRequest.from(request)
                        .headers { it.setBearerAuth(authorizedClient.accessToken.tokenValue) }
                        .build()
                }
                .flatMap(next::exchange)
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