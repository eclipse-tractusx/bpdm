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

package org.eclipse.tractusx.bpdm.pool.service.parser.businesspartner

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.BusinessPartnerSearchError
import org.eclipse.tractusx.bpdm.pool.model.parsed.businesspartner.BusinessPartnerSearchParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.BusinessPartnerSearchRaw
import org.springframework.stereotype.Service

/**
 * Validates business partner search criteria.
 */
@Service
class BusinessPartnerSearchParser {

    companion object {
        private const val LEGAL_NAME_MIN_LENGTH = 3
    }

    /**
     * Returns the criteria the search queries business partners with, or every reason they are not acceptable.
     */
    fun parse(request: BusinessPartnerSearchRaw): ParseResult<BusinessPartnerSearchParsed, BusinessPartnerSearchError> {
        val errors = listOfNotNull(
            BusinessPartnerSearchError.BpnAndLegalNameMissing
                .takeIf { request.bpn.isNullOrBlank() && request.legalName.isNullOrBlank() },
            BusinessPartnerSearchError.NoBusinessPartnerKindIncluded
                .takeIf { !request.includesLegalEntities && !request.includesSites && !request.includesAdditionalAddresses },
            BusinessPartnerSearchError.LeadingWhitespace
                .takeIf { request.bpn.startsWithWhitespace() || request.legalName.startsWithWhitespace() },
            BusinessPartnerSearchError.LegalNameTooShort(LEGAL_NAME_MIN_LENGTH)
                .takeIf { request.legalName != null && request.legalName.length < LEGAL_NAME_MIN_LENGTH }
        )
        if (errors.isNotEmpty()) return ParseResult.Failure(errors)

        return ParseResult.Success(
            BusinessPartnerSearchParsed(
                bpn = request.bpn?.ifEmpty { null },
                legalName = request.legalName,
                streetName = request.streetName,
                postalCode = request.postalCode,
                city = request.city,
                country = request.country,
                includesLegalEntities = request.includesLegalEntities,
                includesSites = request.includesSites,
                includesAdditionalAddresses = request.includesAdditionalAddresses
            )
        )
    }

    private fun String?.startsWithWhitespace(): Boolean = this?.firstOrNull()?.isWhitespace() == true
}
