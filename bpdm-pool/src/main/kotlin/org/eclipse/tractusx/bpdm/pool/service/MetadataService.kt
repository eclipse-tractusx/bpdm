/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.common.dto.response.LegalFormResponse
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalFormRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.CountryIdentifierTypeResponse
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.exception.BpdmAlreadyExists
import org.eclipse.tractusx.bpdm.pool.repository.*
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for fetching and creating metadata entities
 */
@Service
class MetadataService(
    val identifierTypeRepository: IdentifierTypeRepository,
    val issuingBodyRepository: IssuingBodyRepository,
    val legalFormRepository: LegalFormRepository,
    val identifierStatusRepository: IdentifierStatusRepository,
    val countryIdentifierTypeRepository: CountryIdentifierTypeRepository
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createIdentifierType(type: TypeKeyNameDto<String>): TypeKeyNameDto<String> {
        if (identifierTypeRepository.findByTechnicalKey(type.technicalKey) != null)
            throw BpdmAlreadyExists(IdentifierType::class.simpleName!!, type.technicalKey)

        logger.info { "Create new Identifier-Type with key ${type.technicalKey} and name ${type.name}" }
        return identifierTypeRepository.save(IdentifierType(type.name, "", type.technicalKey)).toDto()
    }

    fun getIdentifierTypes(pageRequest: Pageable): PageResponse<TypeKeyNameDto<String>> {
        val page = identifierTypeRepository.findAll(pageRequest)
        return page.toDto(page.content.map { it.toDto() })
    }

    fun getValidIdentifierTypesForCountry(countryCode: CountryCode): Collection<CountryIdentifierTypeResponse> {
        val countryIdentifierTypes = countryIdentifierTypeRepository.findByCountryCodeInOrCountryCodeIsNull(setOf(countryCode))
        return countryIdentifierTypes.map { CountryIdentifierTypeResponse(it.identifierType.toDto(), it.mandatory) }
    }

    // TODO remove
    @Transactional
    fun createIdentifierStatus(status: TypeKeyNameDto<String>): TypeKeyNameDto<String> {
        if (identifierStatusRepository.findByTechnicalKey(status.technicalKey) != null)
            throw BpdmAlreadyExists(IdentifierStatus::class.simpleName!!, status.technicalKey)

        logger.info { "Create new Identifier-Status with key ${status.technicalKey} and name ${status.name}" }
        return identifierStatusRepository.save(IdentifierStatus(status.name, status.technicalKey)).toDto()
    }

    fun getIdentifierStati(pageRequest: Pageable): PageResponse<TypeKeyNameDto<String>> {
        val page = identifierStatusRepository.findAll(pageRequest)
        return page.toDto( page.content.map { it.toDto() } )
    }

    // TODO remove
    @Transactional
    fun createIssuingBody(type: TypeKeyNameDto<String>): TypeKeyNameDto<String> {
        if (issuingBodyRepository.findByTechnicalKey(type.technicalKey) != null)
            throw BpdmAlreadyExists(IssuingBody::class.simpleName!!, type.technicalKey)

        logger.info { "Create new Issuing-Body with key ${type.technicalKey} and name ${type.name}" }
//        return issuingBodyRepository.save(IssuingBody(type.name, "", type.technicalKey)).toDto()
        return TypeKeyNameDto(type.technicalKey, type.name)
    }

    // TODO remove
    fun getIssuingBodies(pageRequest: Pageable): PageResponse<TypeKeyNameDto<String>> {
        val page = issuingBodyRepository.findAll(pageRequest)
//        return page.toDto( page.content.map { it.toDto() } )
        return page.toDto( listOf() )
    }

    @Transactional
    fun createLegalForm(request: LegalFormRequest): LegalFormResponse {
        if (legalFormRepository.findByTechnicalKey(request.technicalKey) != null)
            throw BpdmAlreadyExists(LegalForm::class.simpleName!!, request.technicalKey)

        logger.info { "Create new Legal-Form with key ${request.technicalKey}, name ${request.name} and ${request.category.size} categories" }
        val categories = request.category.map { LegalFormCategory(it.name, it.url) }.toMutableSet()
        val legalForm = LegalForm(
            name = request.name,
            url = request.url,
            language = request.language,
            mainAbbreviation = request.mainAbbreviation,
            categories = categories,
            technicalKey = request.technicalKey
        )

        return legalFormRepository.save(legalForm).toDto()
    }

    fun getLegalForms(pageRequest: Pageable): PageResponse<LegalFormResponse> {
        val page = legalFormRepository.findAll(pageRequest)
        return page.toDto( page.content.map { it.toDto() } )
    }
}