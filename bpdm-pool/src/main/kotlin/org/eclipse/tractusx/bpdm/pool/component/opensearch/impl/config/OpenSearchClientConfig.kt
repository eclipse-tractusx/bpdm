/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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