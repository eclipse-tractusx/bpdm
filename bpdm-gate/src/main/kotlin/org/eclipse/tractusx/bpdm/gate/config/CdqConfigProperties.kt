/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm.cdq")
@ConstructorBinding
class CdqConfigProperties(
    val host: String = "https://api.cdq.com",
    val storage: String = "38d2277e20c730b4b8e8f182adcef7ca",
    val datasourceLegalEntity: String = "62c2fe532b9abd437140a5c1",
    val datasourceSite: String = "62c301de2b9abd437140b704",
    val datasourceAddress: String = "62c30bdb84160a51f6bb227a",
    val apiKey: String = "",
    val dataExchangeApiUrl: String = "/data-exchange/rest/v4/storages/${storage}",
    val dataClinicApiUrl: String = "/data-clinic/rest/storages/${storage}"
)