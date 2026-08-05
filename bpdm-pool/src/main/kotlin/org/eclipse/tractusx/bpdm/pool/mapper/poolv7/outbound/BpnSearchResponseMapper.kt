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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound

import org.eclipse.tractusx.bpdm.pool.api.model.response.BpnIdentifierMappingDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.BpnRequestIdentifierMappingDto
import org.eclipse.tractusx.bpdm.pool.entity.BpnRequestIdentifierMappingDb
import org.eclipse.tractusx.bpdm.pool.model.BpnIdentifierMatch
import org.springframework.stereotype.Component

/**
 * Maps the results of the BPN search operations to the v7 API BPN mapping DTOs.
 */
@Component
class BpnSearchResponseMapper {

    /**
     * Returns the identifier-to-BPN mappings the identifier search found.
     */
    fun toIdentifierMappings(matches: Set<BpnIdentifierMatch>): Set<BpnIdentifierMappingDto> =
        matches.map { BpnIdentifierMappingDto(idValue = it.identifierValue, bpn = it.bpn) }.toSet()

    /**
     * Returns the request-identifier-to-BPN mappings the request identifier search found.
     */
    fun toRequestIdentifierMappings(mappings: Set<BpnRequestIdentifierMappingDb>): Set<BpnRequestIdentifierMappingDto> =
        mappings.map { BpnRequestIdentifierMappingDto(requestedIdentifier = it.requestIdentifier, bpn = it.bpn) }.toSet()
}
