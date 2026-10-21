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

package org.eclipse.tractusx.bpdm.pool.service.application.v6

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.exception.BpdmValidationErrorException
import org.eclipse.tractusx.bpdm.common.mapping.ValidationContext
import org.eclipse.tractusx.bpdm.common.mapping.ValidationError
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.IdentifierTypeSearchParseError
import org.eclipse.tractusx.bpdm.pool.model.request.IdentifierTypeSearchRequest
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierBusinessPartnerTypeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierTypeDtoV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.inbound.IdentifierTypeRequestMapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.IdentifierTypeResponseMapperV6
import org.eclipse.tractusx.bpdm.pool.service.operation.metadata.IdentifierTypeSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.metadata.IdentifierTypeSearchParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "search identifier types" operation, using the v6 request/response shapes.
 */
@Service
class IdentifierTypeSearchApplicationV6Service(
    private val identifierTypeSearchParser: IdentifierTypeSearchParser,
    private val identifierTypeSearchService: IdentifierTypeSearchService,
    private val identifierTypeRequestMapperV6: IdentifierTypeRequestMapperV6,
    private val identifierTypeResponseMapperV6: IdentifierTypeResponseMapperV6
) {

    /**
     * Returns the requested page of identifier types matching the given criteria.
     */
    @Transactional(readOnly = true)
    fun searchIdentifierTypes(
        businessPartnerType: IdentifierBusinessPartnerTypeV6,
        country: String?,
        paginationRequest: PaginationRequest
    ): PageDto<IdentifierTypeDtoV6> {
        val criteria = when (val result = identifierTypeSearchParser.parse(identifierTypeRequestMapperV6.toSearchRequest(businessPartnerType, country))) {
            is ParseResult.Success -> result.parsed
            is ParseResult.Failure -> throw BpdmValidationErrorException(result.errors.map { error ->
                when (error) {
                    is IdentifierTypeSearchParseError.CountryNotFound -> ValidationError(
                        "CountryNotFound", "Country '${error.country}' not found in catalogue.", error.country,
                        ValidationContext.fromRoot(IdentifierTypeSearchRequest::class, "request", IdentifierTypeSearchRequest::country)
                    )
                }
            })
        }

        return identifierTypeSearchService.search(criteria, paginationRequest.toPageRequest())
            .toPageDto { identifierTypeResponseMapperV6.toIdentifierType(it) }
    }
}
