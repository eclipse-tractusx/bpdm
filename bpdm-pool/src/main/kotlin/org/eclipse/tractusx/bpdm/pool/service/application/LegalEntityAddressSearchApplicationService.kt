/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.bpdm.pool.service.application

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.mapper.DbToOutputMapper
import org.eclipse.tractusx.bpdm.pool.model.output.address.LegalEntityAddressSearchResult
import org.eclipse.tractusx.bpdm.pool.model.raw.address.LegalEntityAddressSearchRaw
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityAddressSearchParser
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Lists the addresses that belong to a legal entity directly instead of through one of its sites.
 */
@Service
class LegalEntityAddressSearchApplicationService(
    private val legalEntityAddressSearchParser: LegalEntityAddressSearchParser,
    private val addressSearchService: AddressSearchService,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Returns the requested page of addresses of the legal entity [request] names that belong to none of its sites, or
     * the reason the search was rejected.
     */
    @Transactional(readOnly = true)
    fun search(request: LegalEntityAddressSearchRaw, pageable: Pageable): LegalEntityAddressSearchResult =
        when (val parseResult = legalEntityAddressSearchParser.parse(request)) {
            is ParseResult.Failure -> LegalEntityAddressSearchResult.Rejected(parseResult.errors)
            is ParseResult.Success -> LegalEntityAddressSearchResult.Found(
                addressSearchService.search(parseResult.parsed, pageable).map(dbToOutputMapper::toRecord)
            )
        }
}
