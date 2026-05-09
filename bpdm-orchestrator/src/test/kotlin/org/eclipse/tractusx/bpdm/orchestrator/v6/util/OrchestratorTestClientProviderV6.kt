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

package org.eclipse.tractusx.bpdm.orchestrator.v6.util

import org.eclipse.tractusx.bpdm.common.util.BpdmClientCreateProperties
import org.eclipse.tractusx.bpdm.common.util.BpdmWebClientProvider
import org.eclipse.tractusx.orchestrator.api.v6.client.OrchestratorApiClientV6
import org.eclipse.tractusx.orchestrator.api.v6.client.OrchestratorApiClientV6Impl
import org.springframework.boot.web.server.WebServer

class OrchestratorTestClientProviderV6(
    private val ownWebServer: WebServer,
    private val bpdmWebClientProvider: BpdmWebClientProvider
) {

    fun createClient(oauth2RegistrationId: String?): OrchestratorApiClientV6 {
        return OrchestratorApiClientV6Impl {
            bpdmWebClientProvider.builder(
                BpdmClientCreateProperties(
                    oauth2RegistrationId ?: "",
                    "http://localhost:${ownWebServer.port}",
                    oauth2RegistrationId != null
                )
            ).build()
        }
    }
}