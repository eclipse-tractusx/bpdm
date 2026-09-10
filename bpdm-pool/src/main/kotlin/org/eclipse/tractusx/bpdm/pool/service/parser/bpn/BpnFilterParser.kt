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

package org.eclipse.tractusx.bpdm.pool.service.parser.bpn

import org.springframework.stereotype.Service

/**
 * Turns the BPNs a search filters by into the normalized form the search operations query with.
 *
 * Unlike the upsert parsers this one returns its parsed value directly instead of a `ParseResult`: no filter value can
 * be rejected — an unknown or malformed one matches nothing — so there is no failure to report.
 */
@Service
class BpnFilterParser {

    /**
     * Normalizes the filter by dropping blank values and reading BPNs case-insensitively.
     */
    fun parse(bpns: Collection<String>): List<String> =
        bpns.filter { it.isNotBlank() }.map { it.uppercase() }
}
