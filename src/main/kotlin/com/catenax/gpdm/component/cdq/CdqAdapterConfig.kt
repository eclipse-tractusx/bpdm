package com.catenax.gpdm.component.cdq

import com.catenax.gpdm.component.cdq.config.CdqAdapterConfigProperties
import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import javax.annotation.PostConstruct


@EnableAsync
@Configuration
@ComponentScan
@ConfigurationPropertiesScan
class CdqAdapterConfig(
    val adapterProperties: CdqAdapterConfigProperties
) {

    companion object {
        const val memorySize = 1 * 1024 * 1024 // 1mb
    }

    private val logger = KotlinLogging.logger { }

    @PostConstruct
    fun logCreation() {
        logger.info { "Enable and configure CDQ adapter" }
    }

    @Bean
    fun adapterClient(): WebClient {
        return WebClient.builder()
            .exchangeStrategies(ExchangeStrategies.builder()
                .codecs { codecs: ClientCodecConfigurer -> codecs.defaultCodecs().maxInMemorySize(memorySize) }
                .build())
            .baseUrl("${adapterProperties.host}/${adapterProperties.api}/storages/${adapterProperties.storage}")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("x-api-key", adapterProperties.apiKey)
            .build()
    }
}