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

package org.eclipse.tractusx.bpdm.pool.service.parser

import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.UnresolvableLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressSearchParsed
import org.eclipse.tractusx.bpdm.pool.model.request.AddressSearchRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates address search criteria that name the legal entity whose addresses are being listed.
 */
@Service
class LegalEntityAddressSearchParser(
    private val addressSearchParser: AddressSearchParser,
    private val legalEntityBpnParser: LegalEntityBpnParser
) {

    /**
     * Normalizes the criteria and fails them when a legal entity they filter by does not exist, so that listing the
     * addresses of an unknown legal entity is distinguishable from a legal entity that has none.
     */
    @Transactional(readOnly = true)
    fun parse(request: AddressSearchRequest): ParseResult<AddressSearchParsed, UnresolvableLegalEntity> {
        val criteria = addressSearchParser.parse(request)
        val unresolvedLegalEntities = legalEntityBpnParser.parse(criteria.legalEntityBpns)
            .filterIsInstance<ParseResult.Failure<UnresolvableLegalEntity>>()
            .flatMap { it.errors }

        return if (unresolvedLegalEntities.isEmpty()) ParseResult.Success(criteria) else ParseResult.Failure(unresolvedLegalEntities)
    }
}
