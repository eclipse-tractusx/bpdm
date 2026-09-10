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

package org.eclipse.tractusx.bpdm.test.system.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

/**
 * Configures the HTTP connector all tester clients send their requests through, with a connection pool that drops
 * connections before the services can close them.
 *
 * Reactor Netty pools connections without an idle limit by default, so a service closing an idle connection on its own
 * keep-alive timeout leaves the pool handing out a dead one - which surfaces as "Connection prematurely closed BEFORE
 * response" on the next request. A scenario leaves minutes between its steps while it waits for the golden record
 * process, so its connections regularly idle past that timeout. Retrying is no remedy here: the tester posts golden
 * record task step results, which must not be sent twice.
 */
@Configuration
class ClientConnectorConfig {

    companion object {
        // Well below the keep-alive timeout of an untuned Tomcat, which none of the services override.
        private val MAX_IDLE_TIME = Duration.ofSeconds(5)
        private val MAX_LIFE_TIME = Duration.ofMinutes(5)
        private val BACKGROUND_EVICTION_INTERVAL = Duration.ofSeconds(30)
    }

    /** The connector every tester client is built with. */
    @Bean
    fun clientConnector(): ClientHttpConnector {
        val connectionProvider = ConnectionProvider.builder("bpdm-system-tester")
            .maxIdleTime(MAX_IDLE_TIME)
            .maxLifeTime(MAX_LIFE_TIME)
            .evictInBackground(BACKGROUND_EVICTION_INTERVAL)
            .build()

        return ReactorClientHttpConnector(HttpClient.create(connectionProvider))
    }
}
