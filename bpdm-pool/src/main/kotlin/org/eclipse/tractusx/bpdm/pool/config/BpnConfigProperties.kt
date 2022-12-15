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

package org.eclipse.tractusx.bpdm.pool.config

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = "bpdm.bpn")
data class BpnConfigProperties(
    val agencyName: String = "Catena-X",
    val agencyKey: String = "CATENAX",
    var name: String = "Business Partner Number",
    val id: String = "BPN",
    val legalEntityChar: Char = 'L',
    val siteChar: Char = 'S',
    val addressChar: Char = 'A',
    val counterKeyLegalEntities: String = "bpn-l-counter",
    val counterKeySites: String = "bpn-s-counter",
    val counterKeyAddresses: String = "bpn-a-counter",
    val counterDigits: Int = 10,
    val checksumModulus: Int = 1271,
    val checksumRadix: Int = 36,
    val alphabet: String = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ",
    /**
     * Maximum number of bpn to identifier mappings that can be retrieved per request via the bpn search endpoint.
     */
    val searchRequestLimit: Int = 5000
)