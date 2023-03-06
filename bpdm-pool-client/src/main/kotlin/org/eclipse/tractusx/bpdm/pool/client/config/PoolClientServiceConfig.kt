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

package org.eclipse.tractusx.bpdm.pool.client.config

import org.eclipse.tractusx.bpdm.pool.client.service.*
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

class PoolClientServiceConfig(webClient: WebClient) {

    private val httpServiceProxyFactory = HttpServiceProxyFactory
        .builder(WebClientAdapter.forClient(webClient))
        .build()




    private val poolClientAddress= httpServiceProxyFactory.createClient(PoolClientAddressInterface::class.java)
    private val poolClientBpn = httpServiceProxyFactory.createClient(PoolClientBpnInterface::class.java)
    private val oolClientBusinessPartner = httpServiceProxyFactory.createClient(PoolClientBusinessPartnerInterface::class.java)
    private val poolClientLegalEntity= httpServiceProxyFactory.createClient(PoolClientLegalEntityInterface::class.java)
    private val poolClientMetadata= httpServiceProxyFactory.createClient(PoolClientMetadataInterface::class.java)
    private val poolClientSite= httpServiceProxyFactory.createClient(PoolClientSiteInterface::class.java)
    private val poolClientSuggestion= httpServiceProxyFactory.createClient(PoolClientSuggestionInterface::class.java)
    private val poolClientOpenSearch = httpServiceProxyFactory.createClient(PoolClientOpenSearchInterface::class.java)
    private val poolClientSaas = httpServiceProxyFactory.createClient(PoolClientSaasInterface::class.java)


    fun getPoolClientAddress(): PoolClientAddressInterface {
        return poolClientAddress
    }

    fun getPoolClientBpn(): PoolClientBpnInterface {
        return poolClientBpn
    }

    fun getPoolClientBusinessPartner(): PoolClientBusinessPartnerInterface {
        return oolClientBusinessPartner
    }

    fun getPoolClientLegalEntity(): PoolClientLegalEntityInterface {
        return poolClientLegalEntity
    }

    fun getPoolClientMetadata(): PoolClientMetadataInterface {
        return poolClientMetadata
    }

    fun getPoolClientSite(): PoolClientSiteInterface {
        return poolClientSite
    }

    fun getPoolClientSuggestion(): PoolClientSuggestionInterface {
        return poolClientSuggestion
    }

    fun getPoolClientOpenSearch(): PoolClientOpenSearchInterface {
        return poolClientOpenSearch
    }

    fun getPoolClientSaasInterface(): PoolClientSaasInterface {
        return poolClientSaas
    }

}