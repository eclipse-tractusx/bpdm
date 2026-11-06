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

import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.QualityLevel
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.exception.BpdmValidationErrorException
import org.eclipse.tractusx.bpdm.common.mapping.ValidationContext
import org.eclipse.tractusx.bpdm.common.mapping.ValidationError
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.CountrySubdivisionV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.CountryV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.FieldQualityRuleV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.IdentifierBusinessPartnerTypeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.IdentifierTypeDetailV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.IdentifierTypeV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.LegalFormV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.LegalFormV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.QualityLevelV6
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalFormDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmAlreadyExists
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.AdministrativeAreaRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.CountryRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.FieldQualityRuleRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.IdentifierTypeRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.LegalFormRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.FieldQualityRuleSearchError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.IdentifierTypeCreateError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.IdentifierTypeSearchError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.LegalFormCreateError
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
            businessPartnerType = toDto(record.businessPartnerType),
            name = record.name,
            abbreviation = record.abbreviation,
            transliteratedName = record.transliteratedName,
            transliteratedAbbreviation = record.transliteratedAbbreviation,
            details = record.details.map { IdentifierTypeDetailV6Dto(country = it.countryCode, mandatory = it.mandatory) }
        )

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: FieldQualityRuleRecord): FieldQualityRuleV6Dto =
        FieldQualityRuleV6Dto(
            fieldPath = record.fieldPath,
            schemaName = record.schemaName,
            country = record.countryCode,
            qualityLevel = toDto(record.qualityLevel)
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

    /**
     * Returns the exception the API reports [error] with.
     */
    fun toException(error: FieldQualityRuleSearchError): BpdmNotFoundException =
        when (error) {
            is FieldQualityRuleSearchError.CountryNotFound -> BpdmNotFoundException("Country", error.country)
        }

    /**
     * Returns the exception the API reports [errors] with, reporting a technical key that is already taken ahead of
     * every other error because a client has to free the key before the rest can be judged.
     */
    fun toException(errors: List<IdentifierTypeCreateError>): RuntimeException {
        val alreadyTaken = errors.filterIsInstance<IdentifierTypeCreateError.TechnicalKeyAlreadyTaken>().firstOrNull()
        if (alreadyTaken != null)
            return BpdmAlreadyExists(IdentifierTypeDb::class.simpleName!!, "${alreadyTaken.technicalKey}/${alreadyTaken.businessPartnerType}")
        return BpdmValidationErrorException(errors.map { error ->
            when (error) {
                is IdentifierTypeCreateError.CountryNotFound -> ValidationError(
                    "CountryNotFound", "Country '${error.country}' not found in catalogue.", error.country,
                    ValidationContext.fromRoot(IdentifierTypeV6Dto::class, "request", IdentifierTypeV6Dto::details)
                )
                is IdentifierTypeCreateError.TechnicalKeyAlreadyTaken -> error("Technical key conflict must be handled first")
            }
        })
    }

    /**
     * Returns the exception the API reports [errors] with, reporting a technical key that is already taken ahead of
     * every other error because a client has to free the key before the rest can be judged.
     */
    fun toLegalFormCreateException(errors: List<LegalFormCreateError>): RuntimeException {
        val alreadyTaken = errors.filterIsInstance<LegalFormCreateError.TechnicalKeyAlreadyTaken>().firstOrNull()
        if (alreadyTaken != null)
            return BpdmAlreadyExists(LegalFormDb::class.simpleName!!, alreadyTaken.technicalKey)

        return BpdmValidationErrorException(errors.map { error ->
            when (error) {
                is LegalFormCreateError.UnresolvableAdministrativeArea -> ValidationError(
                    "AdministrativeAreaNotFound", "Administrative area '${error.regionCode}' not found in system.", error.regionCode,
                    ValidationContext.fromRoot(LegalFormV6Request::class, "request", LegalFormV6Request::administrativeAreaLevel1)
                )
                is LegalFormCreateError.CountryNotFound -> ValidationError(
                    "CountryNotFound", "Country '${error.country}' not found in catalogue.", error.country,
                    ValidationContext.fromRoot(LegalFormV6Request::class, "request", LegalFormV6Request::country)
                )
                is LegalFormCreateError.InvalidLanguage -> ValidationError(
                    "InvalidLanguage", "Language '${error.language}' is not an accepted ISO 639-1 code.", error.language,
                    ValidationContext.fromRoot(LegalFormV6Request::class, "request", LegalFormV6Request::language)
                )
                is LegalFormCreateError.TechnicalKeyAlreadyTaken -> error("Technical key conflict must be handled first")
            }
        })
    }

    private fun toDto(businessPartnerType: IdentifierBusinessPartnerType): IdentifierBusinessPartnerTypeV6 =
        when (businessPartnerType) {
            IdentifierBusinessPartnerType.LEGAL_ENTITY -> IdentifierBusinessPartnerTypeV6.LEGAL_ENTITY
            IdentifierBusinessPartnerType.ADDRESS -> IdentifierBusinessPartnerTypeV6.ADDRESS
        }

    private fun toDto(qualityLevel: QualityLevel): QualityLevelV6 =
        when (qualityLevel) {
            QualityLevel.MANDATORY -> QualityLevelV6.MANDATORY
            QualityLevel.OPTIONAL -> QualityLevelV6.OPTIONAL
            QualityLevel.FORBIDDEN -> QualityLevelV6.FORBIDDEN
        }
}
