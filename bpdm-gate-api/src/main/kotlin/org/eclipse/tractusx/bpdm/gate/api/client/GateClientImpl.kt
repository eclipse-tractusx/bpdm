/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.api.client

import org.eclipse.tractusx.bpdm.common.service.ParameterObjectArgumentResolver
import org.eclipse.tractusx.bpdm.gate.api.*
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import java.time.Duration

class GateClientImpl(
    private val webClientProvider: () -> WebClient
) : GateClient {

    private val httpServiceProxyFactory: HttpServiceProxyFactory by lazy {
        HttpServiceProxyFactory
            .builder(WebClientAdapter.forClient(webClientProvider()))
            .customArgumentResolver(ParameterObjectArgumentResolver())
            .blockTimeout(Duration.ofSeconds(30))
            .build()
    }

    private val gateClientBusinessPartner by lazy { httpServiceProxyFactory.createClient(GateBusinessPartnerApi::class.java) }
    private val gateClientAddress by lazy { httpServiceProxyFactory.createClient(GateAddressApi::class.java) }
    private val gateClientLegalEntity by lazy { httpServiceProxyFactory.createClient(GateLegalEntityApi::class.java) }
    private val gateClientSite by lazy { httpServiceProxyFactory.createClient(GateSiteApi::class.java) }
    private val gateClientChangelog by lazy { httpServiceProxyFactory.createClient(GateChangelogApi::class.java) }
    private val gateClientSharingState by lazy { httpServiceProxyFactory.createClient(GateSharingStateApi::class.java) }

    override fun businessParters() = gateClientBusinessPartner

    override fun addresses() = gateClientAddress

    override fun legalEntities() = gateClientLegalEntity

    override fun sites() = gateClientSite

    override fun changelog() = gateClientChangelog

    override fun sharingState() = gateClientSharingState

}



