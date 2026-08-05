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

package org.eclipse.tractusx.bpdm.pool.controller.v6

import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.BpnRequestIdentifierSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.IdentifiersSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.BpnIdentifierMappingDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.BpnRequestIdentifierMappingDtoV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV7
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerFetchService
import org.springframework.stereotype.Service

@Service
class BpnLegacyServiceMapper(
    private val businessPartnerFetchService: BusinessPartnerFetchService
) {

    fun findBpnsByIdentifiers(request: IdentifiersSearchRequestV6): Set<BpnIdentifierMappingDtoV6> {
        return businessPartnerFetchService
            .findBpnsByIdentifiers(request.idType, request.businessPartnerType.toV7(), request.idValues)
            .map { it.toV6() }
            .toSet()
    }

    fun findBpnByRequestedIdentifiers(request: BpnRequestIdentifierSearchRequestV6): Set<BpnRequestIdentifierMappingDtoV6> {
        return businessPartnerFetchService
            .findBpnByRequestedIdentifiers(request.requestedIdentifiers)
            .map { it.toV6() }
            .toSet()
    }
}
