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

package org.eclipse.tractusx.bpdm.pool.mapper.apiv7

import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.exception.BpdmValidationErrorException
import org.eclipse.tractusx.bpdm.common.mapping.ValidationContext
import org.eclipse.tractusx.bpdm.common.mapping.ValidationError
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.CountrySubdivisionV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.CountryV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.FieldQualityRuleV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierTypeDetailV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierTypeV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.LegalFormV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.LegalFormV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.ReasonCodeV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.ScriptCodeV7Dto
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalFormDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmAlreadyExists
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.AdministrativeAreaRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.CountryRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.FieldQualityRuleRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.IdentifierTypeRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.LegalFormRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.ReasonCodeRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.ScriptCodeRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.metadata.FieldQualityRuleSearchError
import org.eclipse.tractusx.bpdm.pool.model.parsed.metadata.IdentifierTypeCreateError
import org.eclipse.tractusx.bpdm.pool.model.parsed.metadata.IdentifierTypeSearchError
import org.eclipse.tractusx.bpdm.pool.model.parsed.metadata.LegalFormCreateError
import org.springframework.stereotype.Component

/**
 * Maps metadata records to their API v7 form.
 */
@Component
class ApiV7MetadataMapper {

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: ReasonCodeRecord): ReasonCodeV7Dto =
        ReasonCodeV7Dto(
            technicalKey = record.technicalKey,
            description = record.description
        )

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: ScriptCodeRecord): ScriptCodeV7Dto =
        ScriptCodeV7Dto(
            technicalKey = record.technicalKey,
            description = record.description
        )

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: CountryRecord): CountryV7Dto =
        CountryV7Dto(
            countryCode = record.countryCode,
            name = record.name,
            description = record.description
        )

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: AdministrativeAreaRecord): CountrySubdivisionV7Dto =
        CountrySubdivisionV7Dto(
            countryCode = record.countryCode,
            code = record.code,
            name = record.name
        )

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: LegalFormRecord): LegalFormV7Dto =
        LegalFormV7Dto(
            technicalKey = record.technicalKey,
            name = record.name,
            transliteratedName = record.transliteratedName,
            abbreviations = record.abbreviation,
            transliteratedAbbreviations = record.transliteratedAbbreviations,
            country = record.countryCode,
            language = record.languageCode,
            administrativeAreaLevel1 = record.administrativeAreaCode,
            isActive = record.isActive
        )

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: IdentifierTypeRecord): IdentifierTypeV7Dto =
        IdentifierTypeV7Dto(
            technicalKey = record.technicalKey,
            businessPartnerType = record.businessPartnerType,
            name = record.name,
            abbreviation = record.abbreviation,
            transliteratedName = record.transliteratedName,
            transliteratedAbbreviation = record.transliteratedAbbreviation,
            format = record.format,
            categories = record.categories.toSortedSet(),
            details = record.details.map { IdentifierTypeDetailV7Dto(country = it.countryCode, mandatory = it.mandatory) }
        )

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: FieldQualityRuleRecord): FieldQualityRuleV7Dto =
        FieldQualityRuleV7Dto(
            fieldPath = record.fieldPath,
            schemaName = record.schemaName,
            country = record.countryCode,
            qualityLevel = record.qualityLevel
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
     * Returns the exception the API reports [errors] with, surfacing the first error's kind because a create covers a
     * single identifier type.
     */
    fun toException(errors: List<IdentifierTypeCreateError>): RuntimeException =
        when (val error = errors.first()) {
            is IdentifierTypeCreateError.TechnicalKeyAlreadyTaken ->
                BpdmAlreadyExists(IdentifierTypeDb::class.simpleName!!, "${error.technicalKey}/${error.businessPartnerType}")
            is IdentifierTypeCreateError.CountryNotFound ->
                BpdmValidationErrorException(errors.filterIsInstance<IdentifierTypeCreateError.CountryNotFound>().map {
                    ValidationError(
                        "CountryNotFound", "Country '${it.country}' not found in catalogue.", it.country,
                        ValidationContext(classType = "", objectId = "request", propertyPath = listOf("details"))
                    )
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
                    ValidationContext.fromRoot(LegalFormV7Request::class, "request", LegalFormV7Request::administrativeAreaLevel1)
                )
                is LegalFormCreateError.CountryNotFound -> ValidationError(
                    "CountryNotFound", "Country '${error.country}' not found in catalogue.", error.country,
                    ValidationContext.fromRoot(LegalFormV7Request::class, "request", LegalFormV7Request::country)
                )
                is LegalFormCreateError.InvalidLanguage -> ValidationError(
                    "InvalidLanguage", "Language '${error.language}' is not an accepted ISO 639-1 code.", error.language,
                    ValidationContext.fromRoot(LegalFormV7Request::class, "request", LegalFormV7Request::language)
                )
                is LegalFormCreateError.TechnicalKeyAlreadyTaken -> error("Technical key conflict must be handled first")
            }
        })
    }
}
