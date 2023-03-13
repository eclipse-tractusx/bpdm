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

package org.eclipse.tractusx.bpdm.gate.config

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = "bpdm.saas")
class SaasConfigProperties(
    val host: String = "http://localhost:1234",
    val storage: String = "storage_id",
    val datasource: String = "datasource_id",
    val legalEntityType: String = "LEGAL_ENTITY",
    val siteType: String = "ORGANIZATIONAL_UNIT",
    val addressType: String = "BP_ADDRESS",
    val apiKey: String = "",
    val dataExchangeApiUrl: String = "/data-exchange/rest/v4/storages/${storage}",
    val referenceDataApiUrl: String = "referencedata/rest/v3",
    val dataClinicApiUrl: String = "/data-clinic/rest/storages/${storage}",
    val dataValidationApiUrl: String = "/data-validation/rest/v2"
)