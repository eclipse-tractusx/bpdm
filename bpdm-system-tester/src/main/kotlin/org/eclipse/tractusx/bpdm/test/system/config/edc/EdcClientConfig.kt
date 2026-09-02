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

package org.eclipse.tractusx.bpdm.test.system.config.edc

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.util.BpdmWebClientProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

/** The offers this run reaches an API over, one per client that is configured for the EDC. */
class EdcAccessNegotiators(
    private val negotiatorsByRegistrationId: Map<String, EdcAccessNegotiator>
) {

    /** Reports whether any client of this run reaches its API over an EDC. */
    val isAnyConfigured get() = negotiatorsByRegistrationId.isNotEmpty()

    /** Returns the offer the client reaches its API over, or null where it does not go over an EDC. */
    fun of(registrationId: String) = negotiatorsByRegistrationId[registrationId]

    /** Returns every offer this run negotiated for, keyed by the client that reaches its API over it. */
    fun all(): Map<String, EdcAccessNegotiator> = negotiatorsByRegistrationId
}

@Configuration
class EdcClientConfig {

    companion object {
        private val logger = KotlinLogging.logger { }

        private const val API_KEY_HEADER = "X-Api-Key"
    }

    /**
     * Negotiates access for every client configured for the EDC, before any scenario runs.
     *
     * Clients naming the same consumer and the same asset share one negotiator, and with it one agreement.
     */
    @Bean
    fun edcAccessNegotiators(clientProperties: List<EdcCapableClientProperties>): EdcAccessNegotiators {
        val edcClients = clientProperties.filter { it.edc.enabled }
        edcClients.forEach { it.edc.validate(it.getId()) }

        if (edcClients.isEmpty()) return EdcAccessNegotiators(emptyMap())

        logger.info {
            "Reaching over an EDC: " + edcClients.joinToString(", ") { "'${it.getId()}' as ${it.edc.consumer.consumerBpnl}" }
        }

        val managementClients = mutableMapOf<EdcConsumerProperties, EdcManagementClient>()
        val negotiators = mutableMapOf<EdcClientProperties, EdcAccessNegotiator>()

        return EdcAccessNegotiators(edcClients.associate { client ->
            val negotiator = negotiators.getOrPut(client.edc) {
                val management = managementClients.getOrPut(client.edc.consumer) { managementClientOf(client.edc.consumer) }
                EdcAccessNegotiator(client.edc.asset.subject, management, client.edc.asset)
            }
            client.getId() to negotiator
        })
    }

    /**
     * Sends the calls of every EDC-configured client to the provider's data plane, and the rest to the
     * provider that would have handled them.
     */
    @Bean
    @Primary
    fun edcWebClientProvider(
        @Qualifier("bpdmWebClientProvider") delegate: BpdmWebClientProvider,
        negotiators: EdcAccessNegotiators
    ): BpdmWebClientProvider = EdcWebClientProvider(delegate, negotiators.all())

    // A client of its own, so that keeping a token fresh never waits behind the API calls it authorizes.
    private fun managementClientOf(consumer: EdcConsumerProperties) =
        EdcManagementClient(
            WebClient.builder()
                .baseUrl(consumer.managementApiUrl)
                .defaultHeader(API_KEY_HEADER, consumer.apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build(),
            consumer
        )
}
