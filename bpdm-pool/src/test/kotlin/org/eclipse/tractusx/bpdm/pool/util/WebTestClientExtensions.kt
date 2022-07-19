package org.eclipse.tractusx.bpdm.pool.util

import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.web.reactive.function.BodyInserters


inline fun <reified T : Any> WebTestClient.invokePostEndpoint(path: String, body: Any): T {
    return post().uri(path)
        .body(BodyInserters.fromValue(body))
        .exchange()
        .expectStatus().is2xxSuccessful
        .returnResult<T>()
        .responseBody
        .blockFirst()!!
}

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


inline fun <reified T : Any> WebTestClient.invokeGetEndpoint(path: String, vararg params: Pair<String, String>): T {
    return get().uri { builder ->
        var b = builder.path(path)
        params.forEach { p -> b = b.queryParam(p.first, p.second) }
        b.build()
    }
        .exchange()
        .expectStatus().is2xxSuccessful
        .returnResult<T>()
        .responseBody
        .blockFirst()!!
}



