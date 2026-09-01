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

import org.eclipse.tractusx.bpdm.common.util.BpdmClientCreateProperties
import org.eclipse.tractusx.bpdm.common.util.BpdmWebClientProvider
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.net.URI

/**
 * Sends the calls of a client that reaches its API over an EDC to the provider's data plane instead.
 *
 * Every other client is left to the provider this one wraps, so one run can hold both routes at once: the
 * Gate over a data offer and the Orchestrator, which is not offered in the dataspace, with credentials of its
 * own.
 */
class EdcWebClientProvider(
    private val delegate: BpdmWebClientProvider,
    private val negotiatorsByRegistrationId: Map<String, EdcAccessNegotiator>
) : BpdmWebClientProvider {

    companion object {
        // The assets set 'dataAddress.baseUrl' to the API's own '/v7' path, and the data plane appends the
        // path of the incoming request to it. The generated clients emit that same '/v7' - every client
        // interface under '.../api/client' is version seven - so it is dropped here rather than arriving
        // twice. Changing either side means changing the other.
        private const val API_VERSION_PATH = "/v7"

        private const val MAX_RESPONSE_SIZE = 10 * 1024 * 1024
    }

    override fun builder(properties: BpdmClientCreateProperties): WebClient.Builder {
        val negotiator = negotiatorsByRegistrationId[properties.registrationId]
            ?: return delegate.builder(properties)

        val endpoint = negotiator.dataPlaneEndpoint()

        return WebClient.builder()
            .baseUrl(endpoint)
            .codecs { codecs -> codecs.defaultCodecs().maxInMemorySize(MAX_RESPONSE_SIZE) }
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .filter(dataPlaneFilter(negotiator, URI.create(endpoint).rawPath.removeSuffix("/")))
    }

    /**
     * Addresses the data plane and authorizes the call with the transfer token of the offer.
     *
     * The token is resolved off the event loop the way the OAuth2 provider resolves its own, because keeping
     * it fresh can mean a call to the consumer connector. It is set as the header value the connector issued,
     * without a scheme of our own in front of it.
     */
    private fun dataPlaneFilter(negotiator: EdcAccessNegotiator, endpointPath: String) = ExchangeFilterFunction { request, next ->
        val dataPlaneRequest = withoutApiVersionPath(request, endpointPath)

        Mono.fromSupplier { negotiator.currentToken() }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { token ->
                next.exchange(authorized(dataPlaneRequest, token))
                    .flatMap { response -> retryOnceWhenRejected(response, dataPlaneRequest, token, negotiator, next) }
            }
    }

    /**
     * Sends the call once more with a token fetched after the data plane refused the one it carried.
     *
     * A token can expire between being read and arriving. A second refusal is not an expiry but a policy or
     * an asset that does not grant this call, and is left to the caller as it is.
     */
    private fun retryOnceWhenRejected(
        response: ClientResponse,
        request: ClientRequest,
        rejectedToken: String,
        negotiator: EdcAccessNegotiator,
        next: ExchangeFunction
    ): Mono<ClientResponse> {
        if (response.statusCode() != HttpStatus.UNAUTHORIZED) return Mono.just(response)

        // subscribeOn belongs to the supplier itself: the chain is resumed by the event loop that finished
        // releasing the body, and moving only the subscription of the outer chain would leave the fetch on it.
        return response.releaseBody()
            .then(Mono.fromSupplier { negotiator.tokenAfterRejectionOf(rejectedToken) }.subscribeOn(Schedulers.boundedElastic()))
            .flatMap { token -> next.exchange(authorized(request, token)) }
    }

    private fun authorized(request: ClientRequest, token: String) =
        ClientRequest.from(request).headers { it.set(HttpHeaders.AUTHORIZATION, token) }.build()

    /**
     * Drops the version the client emits from the path, leaving the one the asset already names.
     *
     * The version follows the data plane's own path rather than opening the request: what the filter sees is
     * the address the client was built with and the endpoint of the offer joined together.
     */
    private fun withoutApiVersionPath(request: ClientRequest, endpointPath: String): ClientRequest {
        val path = request.url().rawPath
        val versionedPrefix = endpointPath + API_VERSION_PATH
        if (!path.startsWith(versionedPrefix)) return request

        val strippedUri = UriComponentsBuilder.fromUri(request.url())
            .replacePath(endpointPath + path.substring(versionedPrefix.length))
            .build(true)
            .toUri()

        return ClientRequest.from(request).url(strippedUri).build()
    }
}
