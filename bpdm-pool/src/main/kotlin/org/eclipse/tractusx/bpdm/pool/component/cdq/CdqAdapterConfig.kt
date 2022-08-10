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

package org.eclipse.tractusx.bpdm.pool.component.cdq

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.component.cdq.config.CdqAdapterConfigProperties
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