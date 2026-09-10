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
import org.eclipse.tractusx.bpdm.common.util.BpdmClientCreateProperties
import org.eclipse.tractusx.bpdm.common.util.BpdmWebClientProvider
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
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
 * Every other client is left to the provider this one wraps, so one run can hold both routes at once.
 */
class EdcWebClientProvider(
    private val delegate: BpdmWebClientProvider,
    private val negotiatorsByRegistrationId: Map<String, EdcAccessNegotiator>
) : BpdmWebClientProvider {

    companion object {
        private val logger = KotlinLogging.logger { }

        // The assets set 'dataAddress.baseUrl' to the API's own '/v7' path and the data plane appends the
        // incoming path to it, so the version the generated clients emit would otherwise arrive twice.
        private const val API_VERSION_PATH = "/v7"

        private const val MAX_RESPONSE_SIZE = 10 * 1024 * 1024

        private val BODYLESS_METHODS = setOf(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.DELETE, HttpMethod.OPTIONS)

        private val REJECTED_TOKEN_STATUSES = setOf(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN)
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

    // The token is resolved off the event loop, because keeping it fresh can mean a call to the consumer
    // connector. It is set as the header value the connector issued, without a scheme in front of it.
    private fun dataPlaneFilter(negotiator: EdcAccessNegotiator, endpointPath: String) = ExchangeFilterFunction { request, next ->
        val dataPlaneRequest = withoutContentTypeWhenBodyless(withoutApiVersionPath(request, endpointPath))

        Mono.fromSupplier { negotiator.currentToken() }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { token ->
                next.exchange(authorized(dataPlaneRequest, token))
                    .flatMap { response -> retryOnceWhenRejected(response, dataPlaneRequest, token, negotiator, next) }
                    .flatMap { response -> reportingRefusal(response, dataPlaneRequest) }
            }
    }

    // A token can expire between being read and arriving, and a data plane refuses an expired token the same
    // way it refuses one the policy does not grant. Both are retried once; the second refusal reaches the caller.
    private fun retryOnceWhenRejected(
        response: ClientResponse,
        request: ClientRequest,
        rejectedToken: String,
        negotiator: EdcAccessNegotiator,
        next: ExchangeFunction
    ): Mono<ClientResponse> {
        if (response.statusCode() !in REJECTED_TOKEN_STATUSES) return Mono.just(response)

        // subscribeOn belongs to the supplier itself: the chain is resumed by the event loop that finished
        // releasing the body, and moving only the subscription of the outer chain would leave the fetch on it.
        return response.releaseBody()
            .then(Mono.fromSupplier { negotiator.tokenAfterRejectionOf(rejectedToken) }.subscribeOn(Schedulers.boundedElastic()))
            .flatMap { token -> next.exchange(authorized(request, token)) }
    }

    // A data plane states its reason in the body and nowhere else, while the exception the caller sees names
    // only the status and the data plane's address. The body is put back for the caller to read as well.
    private fun reportingRefusal(response: ClientResponse, request: ClientRequest): Mono<ClientResponse> {
        if (!response.statusCode().isError) return Mono.just(response)

        return response.bodyToMono(String::class.java).defaultIfEmpty("").map { body ->
            logger.warn {
                "The data plane answered ${response.statusCode()} to ${request.method()} ${request.url()}:" +
                        " ${body.ifBlank { "no body" }}"
            }

            ClientResponse.create(response.statusCode())
                .headers { it.contentType = response.headers().contentType().orElse(MediaType.APPLICATION_JSON) }
                .body(body)
                .build()
        }
    }

    private fun authorized(request: ClientRequest, token: String) =
        ClientRequest.from(request).headers { it.set(HttpHeaders.AUTHORIZATION, token) }.build()

    // The clients announce 'application/json' on every call, which an API answering a GET ignores. A data
    // plane with 'proxyBody' set reads it as the announcement of a body and refuses the request arriving without one.
    private fun withoutContentTypeWhenBodyless(request: ClientRequest): ClientRequest {
        if (request.method() !in BODYLESS_METHODS) return request

        return ClientRequest.from(request).headers { it.remove(HttpHeaders.CONTENT_TYPE) }.build()
    }

    // The version follows the data plane's own path rather than opening the request: what the filter sees is
    // the endpoint of the offer and the client's path joined together.
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
