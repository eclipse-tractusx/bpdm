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

import org.eclipse.tractusx.bpdm.pool.config.ControllerConfigProperties
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.BpnIdentifierSearchParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SearchValuesTooMany
import org.eclipse.tractusx.bpdm.pool.model.parsed.BpnIdentifierSearchParsed
import org.eclipse.tractusx.bpdm.pool.model.request.BpnIdentifierSearchRequest
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.springframework.stereotype.Service

/**
 * Turns loose BPN-by-identifier search criteria into the resolved form the search operation queries with.
 */
@Service
class BpnIdentifierSearchParser(
    private val controllerConfigProperties: ControllerConfigProperties,
    private val identifierTypeRepository: IdentifierTypeRepository
) {

    /**
     * Resolves the requested identifier type and de-duplicates the identifier values, failing when the request holds
     * more values than the configured search request limit or names an unknown identifier type.
     */
    fun parse(request: BpnIdentifierSearchRequest): ParseResult<BpnIdentifierSearchParsed, BpnIdentifierSearchParseError> {
        val errors = mutableListOf<BpnIdentifierSearchParseError>()

        if (request.identifierValues.size > controllerConfigProperties.searchRequestLimit)
            errors.add(SearchValuesTooMany(request.identifierValues.size, controllerConfigProperties.searchRequestLimit))

        val identifierType = identifierTypeRepository.findByBusinessPartnerTypeAndTechnicalKey(request.businessPartnerType, request.identifierTypeKey)
        if (identifierType == null)
            errors.add(BpnIdentifierSearchParseError.IdentifierTypeNotFound(request.identifierTypeKey, request.businessPartnerType))

        return if (identifierType != null && errors.isEmpty())
            ParseResult.Success(BpnIdentifierSearchParsed(identifierType, request.identifierValues.toSet()))
        else
            ParseResult.Failure(errors)
    }
}
