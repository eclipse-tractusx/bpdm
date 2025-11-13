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

package org.eclipse.tractusx.bpdm.pool.controller.v6

import com.neovisionaries.i18n.CountryCode
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.exception.BpdmValidationErrorException
import org.eclipse.tractusx.bpdm.common.mapping.ValidationContext
import org.eclipse.tractusx.bpdm.common.mapping.ValidationError
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierTypeDetailDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalFormDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalFormRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierTypeDto
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDetailDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalFormDb
import org.eclipse.tractusx.bpdm.pool.entity.RegionDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmAlreadyExists
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalFormRepository
import org.eclipse.tractusx.bpdm.pool.repository.RegionRepository
import org.eclipse.tractusx.bpdm.pool.service.toDto
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MetadataLegacyServiceMapper(
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val legalFormRepository: LegalFormRepository,
    private val regionRepository: RegionRepository
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createIdentifierType(type: IdentifierTypeDto): IdentifierTypeDto {
        if (identifierTypeRepository.findByBusinessPartnerTypeAndTechnicalKey(type.businessPartnerType, type.technicalKey) != null)
            throw BpdmAlreadyExists(IdentifierTypeDb::class.simpleName!!, "${type.technicalKey}/${type.businessPartnerType}")

        logger.info { "Create new Identifier-Type with key ${type.technicalKey}, businessPartnerType ${type.businessPartnerType} and name ${type.name}" }
        val entity = IdentifierTypeDb(
            technicalKey = type.technicalKey,
            businessPartnerType = type.businessPartnerType,
            name = type.name,
            abbreviation = type.abbreviation,
            transliteratedName = type.transliteratedName,
            transliteratedAbbreviation = type.transliteratedAbbreviation
        )
        entity.details.addAll(
            type.details.map { IdentifierTypeDetailDb(entity, it.country, it.mandatory) }.toSet()
        )
        return identifierTypeRepository.save(entity).toDto()
    }

    fun IdentifierTypeDb.toDto(): IdentifierTypeDto {
        return IdentifierTypeDto(
            technicalKey, businessPartnerType, name, abbreviation, transliteratedName, transliteratedAbbreviation,
            details.map { IdentifierTypeDetailDto(it.countryCode, it.mandatory) })
    }

    fun getIdentifierTypes(
        pageRequest: Pageable,
        businessPartnerType: IdentifierBusinessPartnerType,
        country: CountryCode? = null
    ): PageDto<IdentifierTypeDto> {
        val spec = Specification.allOf(
            IdentifierTypeRepository.Specs.byBusinessPartnerType(businessPartnerType),
            IdentifierTypeRepository.Specs.byCountry(country)
        )
        val page = identifierTypeRepository.findAll(spec, pageRequest)
        return page.toDto(page.content.map { it.toDto() })
    }

    @Transactional
    fun createLegalForm(request: LegalFormRequest): LegalFormDto {
        if (legalFormRepository.findByTechnicalKey(request.technicalKey) != null)
            throw BpdmAlreadyExists(LegalFormDb::class.simpleName!!, request.technicalKey)

        logger.info { "Create new Legal-Form with key ${request.technicalKey} and name ${request.name}" }

        val region: RegionDb? = request.administrativeAreaLevel1?.let { code ->
            val regionDb = regionRepository.findByRegionCodeIn(setOf(code)).firstOrNull()
            if (regionDb == null) {
                val validationError = ValidationError(
                    validationErrorCode = "AdministrativeAreaNotFound",
                    errorDetails = "Administrative area '$code' not found in system.",
                    erroneousValue = code,
                    context = ValidationContext.fromRoot( org.eclipse.tractusx.bpdm.pool.api.model.request.LegalFormRequest::class, "request", org.eclipse.tractusx.bpdm.pool.api.model.request.LegalFormRequest::administrativeAreaLevel1)
                )
                throw BpdmValidationErrorException(listOf(validationError))
            }
            regionDb
        }

        val legalForm = LegalFormDb(
            technicalKey = request.technicalKey,
            name = request.name,
            transliteratedName = request.transliteratedName,
            abbreviation = request.abbreviation,
            transliteratedAbbreviations = request.transliteratedAbbreviations,
            countryCode = request.country,
            languageCode = request.language,
            administrativeArea = region,
            isActive = request.isActive
        )

        return legalFormRepository.save(legalForm).toDto()
    }

    fun LegalFormDb.toDto(): LegalFormDto {
        return LegalFormDto(
            technicalKey = technicalKey,
            name = name,
            transliteratedName = transliteratedName,
            abbreviation = abbreviation,
            transliteratedAbbreviations = transliteratedAbbreviations,
            country = countryCode,
            language = languageCode,
            administrativeAreaLevel1 = administrativeArea?.regionCode,
            isActive = isActive
        )
    }

    fun getLegalForms(pageRequest: Pageable): PageDto<LegalFormDto> {
        val page = legalFormRepository.findAll(pageRequest)
        return page.toDto(page.content.map { it.toDto() })
    }


}