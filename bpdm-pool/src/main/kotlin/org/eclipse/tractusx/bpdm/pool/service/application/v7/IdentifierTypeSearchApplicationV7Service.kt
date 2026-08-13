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

package org.eclipse.tractusx.bpdm.pool.service.application.v7

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierTypeDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.IdentifierTypeRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.IdentifierTypeResponseMapper
import org.eclipse.tractusx.bpdm.pool.service.operation.IdentifierTypeSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.IdentifierTypeSearchParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the V7 "search identifier types" operation.
 */
@Service
class IdentifierTypeSearchApplicationV7Service(
    private val identifierTypeSearchParser: IdentifierTypeSearchParser,
    private val identifierTypeSearchService: IdentifierTypeSearchService,
    private val identifierTypeRequestMapper: IdentifierTypeRequestMapper,
    private val identifierTypeResponseMapper: IdentifierTypeResponseMapper
) {

    /**
     * Returns the requested page of identifier types matching the given criteria.
     */
    @Transactional(readOnly = true)
    fun searchIdentifierTypes(
        businessPartnerType: IdentifierBusinessPartnerType,
        country: CountryCode?,
        paginationRequest: PaginationRequest
    ): PageDto<IdentifierTypeDto> {
        val criteria = identifierTypeSearchParser.parse(identifierTypeRequestMapper.toSearchRequest(businessPartnerType, country))

        return identifierTypeSearchService.search(criteria, paginationRequest.toPageRequest())
            .toPageDto { identifierTypeResponseMapper.toIdentifierType(it) }
    }
}
