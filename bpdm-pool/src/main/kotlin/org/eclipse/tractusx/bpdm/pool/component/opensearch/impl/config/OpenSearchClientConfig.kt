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

/**
 * Configures both the OpenSearch "High Level Rest Client" and "Java API Client", which are two alternative clients for accessing OpenSearch.
 */
@Configuration
class OpenSearchClientConfig(
    private val openSearchConfigProperties: OpenSearchConfigProperties
) {

    /**
     * Provides an OpenSearch Java API Client.
     * The client shares the same Low Level Rest Client as the [RestHighLevelClient], which is recommended in the elastic search documentation when using both clients.
     */
    @Bean
    fun openSearchClient(restHighLevelClient: RestHighLevelClient): OpenSearchClient {
        val transport: OpenSearchTransport = RestClientTransport(restHighLevelClient.lowLevelClient, JacksonJsonpMapper())
        return OpenSearchClient(transport)
    }

    /**
     * Provides the High Level Rest Client.
     */
    @Bean
    fun openSearchHighLevelRestClient(): RestHighLevelClient {
        val restClientBuilder: RestClientBuilder =
            RestClient.builder(HttpHost(openSearchConfigProperties.host, openSearchConfigProperties.port, openSearchConfigProperties.scheme))
        return RestHighLevelClient(restClientBuilder)
    }
}