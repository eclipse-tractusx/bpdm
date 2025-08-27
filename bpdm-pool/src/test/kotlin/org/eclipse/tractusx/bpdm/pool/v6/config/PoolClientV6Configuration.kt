/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.v6.config

import org.eclipse.tractusx.bpdm.common.util.BpdmWebClientProvider
import org.eclipse.tractusx.bpdm.pool.api.v6.client.PoolApiClient as PoolApiClientV6
import org.eclipse.tractusx.bpdm.pool.api.v6.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.test.config.SelfClientConfigProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["test.v6"], havingValue = "true", matchIfMissing = false)
class PoolClientV6Configuration {

    @Bean
    fun poolClientV6(
        webServerAppCtxt: ServletWebServerApplicationContext,
        selfClientConfigProperties: SelfClientConfigProperties,
        webClientProvider: BpdmWebClientProvider
    ): PoolApiClientV6 {
        return PoolClientImpl {
            val properties = selfClientConfigProperties.copy(baseUrl = "http://localhost:${webServerAppCtxt.webServer.port}")
            webClientProvider.builder(properties).build()
        }
    }
}