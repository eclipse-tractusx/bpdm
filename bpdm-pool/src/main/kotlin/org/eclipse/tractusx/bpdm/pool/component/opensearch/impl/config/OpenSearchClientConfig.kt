package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.config

import org.apache.http.HttpHost
import org.eclipse.tractusx.bpdm.pool.config.OpenSearchConfigProperties
import org.opensearch.client.RestClient
import org.opensearch.client.RestClientBuilder
import org.opensearch.client.RestHighLevelClient
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.OpenSearchTransport
import org.opensearch.client.transport.rest_client.RestClientTransport
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenSearchClientConfig(
    private val openSearchConfigProperties: OpenSearchConfigProperties
) {

    @Bean
    fun openSearchClient(restHighLevelClient: RestHighLevelClient): OpenSearchClient {
        val transport: OpenSearchTransport = RestClientTransport(restHighLevelClient.lowLevelClient, JacksonJsonpMapper())
        return OpenSearchClient(transport)
    }

    @Bean
    fun openSearchHighLevelRestClient(): RestHighLevelClient {
        val restClientBuilder: RestClientBuilder =
            RestClient.builder(HttpHost(openSearchConfigProperties.host, openSearchConfigProperties.port, openSearchConfigProperties.scheme))
        return RestHighLevelClient(restClientBuilder)
    }
}