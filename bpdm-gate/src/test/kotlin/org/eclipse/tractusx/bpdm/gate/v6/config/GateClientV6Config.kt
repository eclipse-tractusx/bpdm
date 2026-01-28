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

package org.eclipse.tractusx.bpdm.gate.v6.config

import org.eclipse.tractusx.bpdm.common.util.BpdmWebClientProvider
import org.eclipse.tractusx.bpdm.gate.api.v6.client.GateClientV6
import org.eclipse.tractusx.bpdm.gate.v6.util.GateTestClientProviderV6
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["test.v6"], havingValue = "true", matchIfMissing = false)
class GateClientV6Config{
    @Bean
    fun testClientProviderV6(
        webServerApplicationContext: ServletWebServerApplicationContext,
        clientProvider: BpdmWebClientProvider
    ): GateTestClientProviderV6{
        return GateTestClientProviderV6(webServerApplicationContext.webServer!!, clientProvider)
    }

    @Bean
    fun gateClientV6(testClientProvider: GateTestClientProviderV6): GateClientV6{
        return testClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_OPERATOR)
    }
}