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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv6.inbound

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierBusinessPartnerTypeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierTypeDtoV6
import org.eclipse.tractusx.bpdm.pool.model.request.IdentifierTypeCreateRequest
import org.eclipse.tractusx.bpdm.pool.model.request.IdentifierTypeDetailRequest
import org.eclipse.tractusx.bpdm.pool.model.request.IdentifierTypeSearchRequest
import org.springframework.stereotype.Component

/**
 * Creates identifier type requests from the v6 API identifier type DTO and search parameters.
 */
@Component
class IdentifierTypeRequestMapperV6 {

    /**
     * Returns the create request holding the identifier type a client sent.
     */
    fun toCreateRequest(identifierType: IdentifierTypeDtoV6): IdentifierTypeCreateRequest =
        IdentifierTypeCreateRequest(
            technicalKey = identifierType.technicalKey,
            businessPartnerType = IdentifierBusinessPartnerType.valueOf(identifierType.businessPartnerType.name),
            name = identifierType.name,
            abbreviation = identifierType.abbreviation,
            transliteratedName = identifierType.transliteratedName,
            transliteratedAbbreviation = identifierType.transliteratedAbbreviation,
            // v6 knows neither format nor categories, so an identifier type created over v6 carries none.
            format = null,
            categories = sortedSetOf(),
            details = identifierType.details.map { IdentifierTypeDetailRequest(it.country, it.mandatory) }
        )

    /**
     * Returns the search request holding the criteria a client sent.
     */
    fun toSearchRequest(businessPartnerType: IdentifierBusinessPartnerTypeV6, country: CountryCode?): IdentifierTypeSearchRequest =
        IdentifierTypeSearchRequest(
            businessPartnerType = IdentifierBusinessPartnerType.valueOf(businessPartnerType.name),
            country = country
        )
}
