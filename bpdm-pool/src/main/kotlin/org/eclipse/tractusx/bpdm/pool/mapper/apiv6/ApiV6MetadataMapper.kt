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

package org.eclipse.tractusx.bpdm.pool.mapper.apiv6

import org.eclipse.tractusx.bpdm.common.mapping.ValidationContext
import org.eclipse.tractusx.bpdm.common.mapping.ValidationError
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.CountrySubdivisionV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.CountryV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.IdentifierBusinessPartnerTypeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.IdentifierTypeDetailV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.IdentifierTypeV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.LegalFormV6Dto
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.AdministrativeAreaRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.CountryRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.IdentifierTypeRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.LegalFormRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.metadata.IdentifierTypeSearchError
import org.springframework.stereotype.Component

/**
 * Maps metadata records to their API v6 form.
 */
@Component
class ApiV6MetadataMapper {

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: CountryRecord): CountryV6Dto =
        CountryV6Dto(
            countryCode = record.countryCode,
            name = record.name,
            description = record.description
        )

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: AdministrativeAreaRecord): CountrySubdivisionV6Dto =
        CountrySubdivisionV6Dto(
            countryCode = record.countryCode,
            code = record.code,
            name = record.name
        )

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: LegalFormRecord): LegalFormV6Dto =
        LegalFormV6Dto(
            technicalKey = record.technicalKey,
            name = record.name,
            transliteratedName = record.transliteratedName,
            abbreviation = record.abbreviation,
            transliteratedAbbreviations = record.transliteratedAbbreviations,
            country = record.countryCode,
            language = record.languageCode,
            administrativeAreaLevel1 = record.administrativeAreaCode,
            isActive = record.isActive
        )

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: IdentifierTypeRecord): IdentifierTypeV6Dto =
        IdentifierTypeV6Dto(
            technicalKey = record.technicalKey,
            businessPartnerType = IdentifierBusinessPartnerTypeV6.valueOf(record.businessPartnerType.name),
            name = record.name,
            abbreviation = record.abbreviation,
            transliteratedName = record.transliteratedName,
            transliteratedAbbreviation = record.transliteratedAbbreviation,
            details = record.details.map { IdentifierTypeDetailV6Dto(country = it.countryCode, mandatory = it.mandatory) }
        )

    /**
     * Returns the validation error the API reports [error] with.
     */
    fun toValidationError(error: IdentifierTypeSearchError): ValidationError =
        when (error) {
            is IdentifierTypeSearchError.CountryNotFound -> ValidationError(
                "CountryNotFound", "Country '${error.country}' not found in catalogue.", error.country,
                ValidationContext(classType = "", objectId = "request", propertyPath = listOf("country"))
            )
        }
}
