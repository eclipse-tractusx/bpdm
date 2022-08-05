package org.eclipse.tractusx.bpdm.gate.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class CdqConfig(
    val cdqProperties: CdqConfigProperties
) {
    companion object {
        const val memorySize = 1 * 1024 * 1024 // 1mb
    }

    @Bean
    fun adapterClient(): WebClient {
        return WebClient.builder()
            .exchangeStrategies(ExchangeStrategies.builder()
                .codecs { codecs: ClientCodecConfigurer -> codecs.defaultCodecs().maxInMemorySize(memorySize) }
                .build())
            .baseUrl(cdqProperties.host)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("x-api-key", cdqProperties.apiKey)
            .build()
    }

    fun getDataExchangeApiUrl(): String {
        return "/data-exchange/rest/v4/storages/${cdqProperties.storage}"
    }

    fun getDataClinicApiUrl(): String {
        return "/data-clinic/rest/storages/${cdqProperties.storage}"
    }
}