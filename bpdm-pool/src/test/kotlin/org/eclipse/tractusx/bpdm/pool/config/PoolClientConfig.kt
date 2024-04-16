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

package org.eclipse.tractusx.bpdm.pool.config


import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.test.config.SelfClientConfigProperties
import org.eclipse.tractusx.bpdm.test.util.BpdmOAuth2ClientFactory
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient


@Configuration
class PoolClientConfig(
    private val selfClientConfigProperties: SelfClientConfigProperties
){

    @Bean
    fun authorizedPoolClient(webServerAppCtxt: ServletWebServerApplicationContext,
                   bpdmOAuth2ClientFactory: BpdmOAuth2ClientFactory?)
    : PoolClientImpl {
        return PoolClientImpl {
            val baseUrl = "http://localhost:${webServerAppCtxt.webServer.port}"
            val client = bpdmOAuth2ClientFactory?.createClient(baseUrl, selfClientConfigProperties.oauth2ClientRegistration)
                ?: WebClient.create("http://localhost:${webServerAppCtxt.webServer.port}")
            client
        }
    }
}