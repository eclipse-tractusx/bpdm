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

package org.eclipse.tractusx.bpdm.pool.service

import com.neovisionaries.i18n.CountryCode
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.IBaseLegalEntityDto
import org.eclipse.tractusx.bpdm.common.dto.IBaseLogisticAddressDto
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalFormRequest
import org.eclipse.tractusx.bpdm.pool.dto.AddressMetadataDto
import org.eclipse.tractusx.bpdm.pool.dto.LegalEntityMetadataDto
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.exception.BpdmAlreadyExists
import org.eclipse.tractusx.bpdm.pool.repository.FieldQualityRuleRepository
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalFormRepository
import org.eclipse.tractusx.bpdm.pool.repository.RegionRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for fetching and creating metadata entities
 */
@Service
class MetadataService(
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val legalFormRepository: LegalFormRepository,
    private val fieldQualityRuleRepository: FieldQualityRuleRepository,
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
            transliteratedAbbreviation = type.transliteratedAbbreviation,
            format = type.format
        )
        entity.categories.addAll(type.categories)
        entity.details.addAll(
            type.details.map { IdentifierTypeDetailDb(entity, it.country, it.mandatory) }.toSet()
        )
        return identifierTypeRepository.save(entity).toDto()
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

        val region = request.administrativeAreaLevel1?.let { regionRepository.findByRegionCodeIn(setOf(it)) }?.firstOrNull()

        val legalForm = LegalFormDb(
            technicalKey = request.technicalKey,
            name = request.name,
            transliteratedName = request.transliteratedName,
            abbreviation = request.abbreviations,
            transliteratedAbbreviations = request.transliteratedAbbreviations,
            countryCode = request.country,
            languageCode = request.language,
            administrativeArea = region,
            isActive = request.isActive
        )

        return legalFormRepository.save(legalForm).toDto()
    }

    fun getLegalForms(pageRequest: Pageable): PageDto<LegalFormDto> {
        val page = legalFormRepository.findAll(pageRequest)
        return page.toDto(page.content.map { it.toDto() })
    }

    /**
     * Get quality rules for the given country merged with the default rules. Forbidden rules are ignored.
     */
    fun getFieldQualityRules(country: CountryCode): Collection<FieldQualityRuleDto> {

        val defaultRules = fieldQualityRuleRepository.findByCountryCodeIsNullOrderBySchemaNameAscFieldPathAsc()
        val rulesForCountry = fieldQualityRuleRepository.findByCountryCodeOrderBySchemaNameAscFieldPathAsc(country)

        val pathToDefaultRule = defaultRules.associateBy(
            { it.schemaName + "." + it.fieldPath }, { it }
        )
        val pathToCountrRule = rulesForCountry.associateBy(
            { it.schemaName + "." + it.fieldPath }, { it }
        )

        val pathsFromDefaultAndCountry = pathToDefaultRule.keys + pathToCountrRule.keys

        val mergedRulesForCountry = pathsFromDefaultAndCountry.mapNotNull { path ->
            mergeRules(pathToDefaultRule[path], pathToCountrRule[path])
        }

        val resultList = mergedRulesForCountry.filter {
            it.qualityLevel != QualityLevel.FORBIDDEN
        }.map { rule ->
            FieldQualityRuleDto(
                fieldPath = rule.fieldPath,
                schemaName = rule.schemaName,
                country = (if (rule.countryCode != null) rule.countryCode else country)!!,
                qualityLevel = rule.qualityLevel
            )
        }

        resultList.sortedWith(compareBy({ it.schemaName }, { it.fieldPath }))
        return resultList
    }

    fun getMetadata(requests: Collection<IBaseLegalEntityDto>): LegalEntityMetadataDto {
        val idTypeKeys = requests.flatMap { it.identifiers }.map { it.type }.toSet()
        val idTypes = identifierTypeRepository.findByBusinessPartnerTypeAndTechnicalKeyIn(IdentifierBusinessPartnerType.LEGAL_ENTITY, idTypeKeys)

        val legalFormKeys = requests.mapNotNull { it.legalForm }.toSet()
        val legalForms = legalFormRepository.findByTechnicalKeyIn(legalFormKeys)

        return LegalEntityMetadataDto(idTypes, legalForms)
    }

    fun getMetadata(requests: Collection<IBaseLogisticAddressDto>): AddressMetadataDto {
        val idTypeKeys = requests.flatMap { it.identifiers }.map { it.type }.toSet()
        val idTypes = identifierTypeRepository.findByBusinessPartnerTypeAndTechnicalKeyIn(IdentifierBusinessPartnerType.ADDRESS, idTypeKeys)

        val regionKeys = requests.mapNotNull { it.physicalPostalAddress?.administrativeAreaLevel1 }
            .plus(requests.mapNotNull { it.alternativePostalAddress?.administrativeAreaLevel1 })
            .toSet()
        val regions = regionRepository.findByRegionCodeIn(regionKeys)

        return AddressMetadataDto(idTypes, regions)
    }

    fun getRegions(requests: Collection<IBaseLogisticAddressDto>): Set<RegionDb> {

        val regionKeys = requests.mapNotNull { it.physicalPostalAddress?.administrativeAreaLevel1 }
            .plus(requests.mapNotNull { it.alternativePostalAddress?.administrativeAreaLevel1 })
            .toSet()
        val regions = regionRepository.findByRegionCodeIn(regionKeys)
        return regions
    }

    fun getIdentifiers(requests: Collection<IBaseLogisticAddressDto>): Set<IdentifierTypeDb> {
        val idTypeKeys = requests.flatMap { it.identifiers }.map { it.type }.toSet()
        val idTypes = identifierTypeRepository.findByBusinessPartnerTypeAndTechnicalKeyIn(IdentifierBusinessPartnerType.ADDRESS, idTypeKeys)
        return idTypes
    }


    /**
     * If no country rule exists use default rules
     */
    private fun mergeRules(defaultRule: FieldQualityRuleDb?, countryRule: FieldQualityRuleDb?): FieldQualityRuleDb? {

        if (countryRule == null) {
            return defaultRule
        }

        return countryRule
    }

    fun getCountrySubdivisions(paginationRequest: PaginationRequest): PageDto<CountrySubdivisionDto> {
        val pageRequest = paginationRequest.toPageRequest(RegionRepository.DEFAULT_SORTING)
        val page = regionRepository.findAll(pageRequest)
        return page.toDto(page.content.map { it.toCountrySubdivisionDto() })
    }
}
