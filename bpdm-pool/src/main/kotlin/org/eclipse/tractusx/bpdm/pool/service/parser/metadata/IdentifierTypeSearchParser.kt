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

package org.eclipse.tractusx.bpdm.pool.service.parser.metadata

import org.eclipse.tractusx.bpdm.pool.model.parsed.IdentifierTypeSearchParsed
import org.eclipse.tractusx.bpdm.pool.model.request.IdentifierTypeSearchRequest
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.IdentifierTypeSearchParseError
import org.eclipse.tractusx.bpdm.pool.repository.CountryRepository
import org.springframework.stereotype.Service

/**
 * Turns loose identifier type search criteria into the form the search operation queries with.
 */
@Service
class IdentifierTypeSearchParser(private val countryRepository: CountryRepository) {

    /**
     * Returns the criteria the search filters identifier types by.
     */
    fun parse(request: IdentifierTypeSearchRequest): ParseResult<IdentifierTypeSearchParsed, IdentifierTypeSearchParseError> {
        val country = request.country?.let { code ->
            countryRepository.findByCountryCode(code)
                ?: return ParseResult.ofSingleFailure(IdentifierTypeSearchParseError.CountryNotFound(code))
        }
        return ParseResult.Success(IdentifierTypeSearchParsed(
            businessPartnerType = request.businessPartnerType,
            country = country
        ))
    }
}
