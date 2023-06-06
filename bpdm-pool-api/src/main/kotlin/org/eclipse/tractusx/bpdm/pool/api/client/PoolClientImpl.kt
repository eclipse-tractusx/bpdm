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

package org.eclipse.tractusx.bpdm.pool.api.client

import org.eclipse.tractusx.bpdm.common.service.ParameterObjectArgumentResolver
import org.eclipse.tractusx.bpdm.pool.api.*
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

class PoolClientImpl(
    private val webClientProvider: () -> WebClient
) : PoolApiClient {


    private val httpServiceProxyFactory: HttpServiceProxyFactory by lazy {
        HttpServiceProxyFactory
            .builder(WebClientAdapter.forClient(webClientProvider()))
            .customArgumentResolver(ParameterObjectArgumentResolver())
            .build()
    }


    private val poolClientAddress by lazy { httpServiceProxyFactory.createClient(PoolAddressApi::class.java) }
    private val poolClientBpn by lazy { httpServiceProxyFactory.createClient(PoolBpnApi::class.java) }
    private val poolClientChangelog by lazy { httpServiceProxyFactory.createClient(PoolChangelogApi::class.java) }
    private val poolClientLegalEntity by lazy { httpServiceProxyFactory.createClient(PoolLegalEntityApi::class.java) }
    private val poolClientMetadata by lazy { httpServiceProxyFactory.createClient(PoolMetadataApi::class.java) }
    private val poolClientSite by lazy { httpServiceProxyFactory.createClient(PoolSiteApi::class.java) }
    private val poolClientOpenSearch by lazy { httpServiceProxyFactory.createClient(PoolOpenSearchApi::class.java) }
    private val poolClientSaas by lazy { httpServiceProxyFactory.createClient(PoolSaasApi::class.java) }

    override fun addresses() = poolClientAddress

    override fun bpns() = poolClientBpn

    override fun changelogs() = poolClientChangelog

    override fun legalEntities() = poolClientLegalEntity

    override fun metadata() = poolClientMetadata

    override fun sites() = poolClientSite

    override fun opensearch() = poolClientOpenSearch

    override fun saas() = poolClientSaas
}