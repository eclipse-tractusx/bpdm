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

package org.eclipse.tractusx.bpdm.pool.api.v6.client

import org.eclipse.tractusx.bpdm.common.service.ParameterObjectArgumentResolver
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

open class PoolV6ClientImpl(
    private val webClientProvider: () -> WebClient
): PoolV6ApiClient {

    private val httpServiceProxyFactory: HttpServiceProxyFactory by lazy {
        HttpServiceProxyFactory.builder()
            .exchangeAdapter(WebClientAdapter.create(webClientProvider()))
            .customArgumentResolver(ParameterObjectArgumentResolver())
            .build()
    }

    override val metadata by lazy { createClient<MetadataV6ApiClient>() }

    override val legalEntities by lazy { createClient<LegalEntityV6ApiClient>() }

    override val sites by lazy { createClient<SiteV6ApiClient>() }

    override val addresses by lazy { createClient<AddressV6ApiClient>() }

    override val memberships by lazy { createClient<CxMembershipV6ApiClient>() }

    override val changelogs by lazy { createClient<ChangelogV6ApiClient>() }

    override val bpns  by lazy { createClient<BpnV6ApiClient>() }

    override val members  by lazy { createClient<MembersV6ApiClient>() }

    private inline fun <reified T: Any> createClient() =
        httpServiceProxyFactory.createClient(T::class.java)!!
}