package com.catenax.gpdm.config

import org.apache.http.ssl.SSLContextBuilder
import org.apache.http.ssl.SSLContexts
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.RestClients
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories
import org.springframework.http.HttpHeaders
import java.security.KeyStore
import java.security.Security
import javax.net.ssl.SSLContext


@Configuration
@EnableElasticsearchRepositories(basePackages = ["com.catenax.gpdm.repository.elastic"])
@ConditionalOnProperty(
    value = ["bpdm.elastic.enabled"],
    havingValue = "true",
    matchIfMissing = false)
class ElasticSearchConfig(
    val esProperties: ElasticSearchConfigProperties
) {

    @Bean
    fun client(): RestHighLevelClient {
        val clientConfiguration = ClientConfiguration.builder()
            .connectedTo("${esProperties.host}:${esProperties.port}")
            .build()
        return RestClients.create(clientConfiguration).rest()
    }

    @Bean
    fun elasticsearchTemplate(): ElasticsearchOperations {
        return ElasticsearchRestTemplate(client())
    }


}