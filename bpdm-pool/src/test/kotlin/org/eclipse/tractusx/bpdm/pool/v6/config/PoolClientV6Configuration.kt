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

package org.eclipse.tractusx.bpdm.pool.v6.config

import org.eclipse.tractusx.bpdm.common.util.BpdmClientCreateProperties
import org.eclipse.tractusx.bpdm.common.util.BpdmUnauthorizedWebClientProvider
import org.eclipse.tractusx.bpdm.common.util.BpdmWebClientProvider
import org.eclipse.tractusx.bpdm.pool.v6.util.*
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["test.v6"], havingValue = "true", matchIfMissing = false)
class PoolClientV6Configuration(
    private val clientProvider: BpdmWebClientProvider,
    private val webServerAppCtxt: ServletWebServerApplicationContext
) {

    @Bean
    fun poolOperatorClientV6(): PoolOperatorClientV6 {
        return PoolOperatorClientV6(clientProvider, webServerAppCtxt.webServer!!)
    }

    @Bean
    fun sharingMemberClientV6(): PoolSharingMemberClientV6 {
        return PoolSharingMemberClientV6(clientProvider, webServerAppCtxt.webServer!!)
    }

    @Bean
    fun participantClientV6(): PoolParticipantClientV6 {
        return PoolParticipantClientV6(clientProvider, webServerAppCtxt.webServer!!)
    }

    @Bean
    fun unauthorizedClientV6(): PoolUnauthorizedClientV6 {
        return PoolUnauthorizedClientV6(clientProvider, webServerAppCtxt.webServer!!)
    }

    @Bean
    fun anonymousClientV6(): PoolAnonymousClientV6 {
        return PoolAnonymousClientV6 {
            BpdmUnauthorizedWebClientProvider().builder(BpdmClientCreateProperties(
                registrationId = "",
                baseUrl = "http://localhost:${webServerAppCtxt.webServer!!.port}",
                securityEnabled = false
            )).build()
        }
    }
}