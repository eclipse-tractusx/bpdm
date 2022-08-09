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

package org.eclipse.tractusx.bpdm.pool.component.cdq.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm.cdq")
@ConstructorBinding
class CdqAdapterConfigProperties(
    val enabled: Boolean = true,
    val host: String = "https://api.cdq.com",
    val api: String = "data-exchange/rest/v4",
    val storage: String = "8888865cc59a3b4aa079b8e00313cf53",
    val datasource: String = "61c096613b4b824755a62641",
    val apiKey: String = "",
    val timestampKey: String = "last-import",
    val importLimit: Int = 100,
    val exportPageSize: Int = 100
)