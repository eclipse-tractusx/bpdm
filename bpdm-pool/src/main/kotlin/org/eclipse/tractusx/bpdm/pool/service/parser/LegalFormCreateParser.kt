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

package org.eclipse.tractusx.bpdm.pool.service.parser

import org.eclipse.tractusx.bpdm.pool.entity.RegionDb
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.LegalFormCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.error.LegalFormCreateParseError.TechnicalKeyAlreadyTaken
import org.eclipse.tractusx.bpdm.pool.model.error.LegalFormCreateParseError.UnresolvableAdministrativeArea
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalFormCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.request.LegalFormCreateRequest
import org.eclipse.tractusx.bpdm.pool.repository.LegalFormRepository
import org.eclipse.tractusx.bpdm.pool.repository.RegionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates a legal form a client asks to create, resolving the administrative area it belongs to.
 */
@Service
class LegalFormCreateParser(
    private val legalFormRepository: LegalFormRepository,
    private val regionRepository: RegionRepository
) {

    /**
     * Validates the request and reports either the validated legal form or every problem found: a technical key that is
     * already taken and an administrative area that does not exist.
     */
    @Transactional(readOnly = true)
    fun parse(request: LegalFormCreateRequest): ParseResult<LegalFormCreateParsed, LegalFormCreateParseError> {
        val errors = mutableListOf<LegalFormCreateParseError>()

        if (legalFormRepository.findByTechnicalKey(request.technicalKey) != null)
            errors.add(TechnicalKeyAlreadyTaken(request.technicalKey))

        val administrativeArea = parseAdministrativeArea(request.administrativeAreaLevel1, errors)

        if (errors.isNotEmpty()) return ParseResult.Failure(errors)

        return ParseResult.Success(
            LegalFormCreateParsed(
                technicalKey = request.technicalKey,
                name = request.name,
                transliteratedName = request.transliteratedName,
                abbreviations = request.abbreviations,
                transliteratedAbbreviations = request.transliteratedAbbreviations,
                country = request.country,
                language = request.language,
                administrativeArea = administrativeArea,
                isActive = request.isActive
            )
        )
    }

    private fun parseAdministrativeArea(regionCode: String?, errors: MutableList<LegalFormCreateParseError>): RegionDb? {
        if (regionCode == null) return null

        return regionRepository.findByRegionCodeIn(setOf(regionCode)).firstOrNull()
            ?: run { errors.add(UnresolvableAdministrativeArea(regionCode)); null }
    }
}
