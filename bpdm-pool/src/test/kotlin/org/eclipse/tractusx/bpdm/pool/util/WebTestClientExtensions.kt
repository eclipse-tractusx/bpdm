package org.eclipse.tractusx.bpdm.pool.util

import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

fun WebTestClient.invokePostEndpointWithoutResponse(path: String) {
    post().uri(path)
        .exchange()
        .expectStatus().is2xxSuccessful
}

fun WebTestClient.invokeDeleteEndpointWithoutResponse(path: String) {
    delete().uri(path)
        .exchange()
        .expectStatus().is2xxSuccessful
}


inline fun <reified T : Any> WebTestClient.invokeGetEndpoint(path: String): T {
    return get().uri(path)
        .exchange()
        .expectStatus().is2xxSuccessful
        .returnResult<T>()
        .responseBody
        .blockFirst()!!
}


