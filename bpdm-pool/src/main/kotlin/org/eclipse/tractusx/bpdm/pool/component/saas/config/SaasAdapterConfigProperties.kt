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

package org.eclipse.tractusx.bpdm.pool.component.saas.config

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = "bpdm.saas")
class SaasAdapterConfigProperties(
    val enabled: Boolean = true,
    val host: String = "https://api.cdq.com",
    val storage: String = "8888865cc59a3b4aa079b8e00313cf53",
    val datasource: String = "61c096613b4b824755a62641",
    val apiKey: String = "",
    val importLimit: Int = 100,
    val importSchedulerCronExpr: String = "-",
    val legalEntityType: String = "LEGAL_ENTITY",
    val siteType: String = "ORGANIZATIONAL_UNIT",
    val addressType: String = "BP_ADDRESS",
    val parentRelationType: String = "PARENT",
    val bpnKey: String = "CX_BPN",
    val importIdKey: String = "CX_POOL_ID"
) {
    private val exchangeApiUrl: String = "data-exchange/rest/v4"
    private val referenceApiUrl: String = "referencedata/rest/v3"

    val readBusinessPartnerUrl = "/${exchangeApiUrl}/storages/${storage}/businesspartners"
    val fetchBusinessPartnersBatchUrl = "/${referenceApiUrl}/businesspartners/fetch-batch"

}