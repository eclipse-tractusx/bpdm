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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound

import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.BpnIdentifierMappingDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.BpnRequestIdentifierMappingDtoV6
import org.eclipse.tractusx.bpdm.pool.entity.BpnRequestIdentifierMappingDb
import org.eclipse.tractusx.bpdm.pool.model.BpnIdentifierMatch
import org.springframework.stereotype.Component

/**
 * Maps the results of the BPN search operations to the v6 API BPN mapping DTOs.
 */
@Component
class BpnSearchResponseMapperV6 {

    /**
     * Returns the identifier-to-BPN mappings the identifier search found.
     */
    fun toIdentifierMappings(matches: Set<BpnIdentifierMatch>): Set<BpnIdentifierMappingDtoV6> =
        matches.map { BpnIdentifierMappingDtoV6(idValue = it.identifierValue, bpn = it.bpn) }.toSet()

    /**
     * Returns the request-identifier-to-BPN mappings the request identifier search found.
     */
    fun toRequestIdentifierMappings(mappings: Set<BpnRequestIdentifierMappingDb>): Set<BpnRequestIdentifierMappingDtoV6> =
        mappings.map { BpnRequestIdentifierMappingDtoV6(requestedIdentifier = it.requestIdentifier, bpn = it.bpn) }.toSet()
}
