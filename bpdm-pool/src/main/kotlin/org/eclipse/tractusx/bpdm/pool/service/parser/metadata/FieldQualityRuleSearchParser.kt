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

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.FieldQualityRuleSearchParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.FieldQualityRuleSearchParsed
import org.eclipse.tractusx.bpdm.pool.model.request.FieldQualityRuleSearchRequest
import org.eclipse.tractusx.bpdm.pool.repository.CountryRepository
import org.springframework.stereotype.Service

/**
 * Resolves the country against the maintained catalogue before searching its field quality rules.
 */
@Service
class FieldQualityRuleSearchParser(private val countryRepository: CountryRepository) {

    /**
     * Returns the criteria the search filters field quality rules by.
     */
    fun parse(request: FieldQualityRuleSearchRequest): ParseResult<FieldQualityRuleSearchParsed, FieldQualityRuleSearchParseError> {
        val country = countryRepository.findByCountryCode(request.country)
            ?: return ParseResult.ofSingleFailure(FieldQualityRuleSearchParseError.CountryNotFound(request.country))
        return ParseResult.Success(FieldQualityRuleSearchParsed(country = country))
    }
}
