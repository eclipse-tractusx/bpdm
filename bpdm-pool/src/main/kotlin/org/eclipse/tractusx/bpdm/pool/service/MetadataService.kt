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
import org.eclipse.tractusx.bpdm.common.dto.IBaseLegalEntityDto
import org.eclipse.tractusx.bpdm.common.dto.IBaseLogisticAddressDto
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.dto.LegalEntityInvariantHeaderMetadataDto
import org.eclipse.tractusx.bpdm.pool.entity.FieldQualityRuleDb
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.eclipse.tractusx.bpdm.pool.entity.RegionDb
import org.eclipse.tractusx.bpdm.pool.repository.*
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * Service for fetching and creating metadata entities
 */
@Service
class MetadataService(
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val legalFormRepository: LegalFormRepository,
    private val fieldQualityRuleRepository: FieldQualityRuleRepository,
    private val regionRepository: RegionRepository,
    private val scriptCodeRepository: ScriptCodeRepository,
    private val reasonCodeRepository: ReasonCodeRepository,
) {

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

    fun getScriptCodes(paginationRequest: PaginationRequest): PageDto<ScriptCodeDto> {
        return scriptCodeRepository.findAll(PageRequest.of(paginationRequest.page, paginationRequest.size)).toDto {
            ScriptCodeDto(it.technicalKey, it.description)
        }
    }

    fun getMetadata(requests: Collection<IBaseLegalEntityDto>): LegalEntityInvariantHeaderMetadataDto {
        val idTypeKeys = requests.flatMap { it.identifiers }.map { it.type }.toSet()
        val idTypes = identifierTypeRepository.findByBusinessPartnerTypeAndTechnicalKeyIn(IdentifierBusinessPartnerType.LEGAL_ENTITY, idTypeKeys)

        val legalFormKeys = requests.mapNotNull { it.legalForm }.toSet()
        val legalForms = legalFormRepository.findByTechnicalKeyIn(legalFormKeys)

        return LegalEntityInvariantHeaderMetadataDto(idTypes, legalForms)
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

    fun getReasonCodes(paginationRequest: PaginationRequest): PageDto<ReasonCodeDto>{
        val pageRequest = PageRequest.of(paginationRequest.page, paginationRequest.size)
        val pageResponse = reasonCodeRepository.findAll(pageRequest)

        return pageResponse.toDto { ReasonCodeDto(technicalKey = it.technicalKey, description = it.description) }
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