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

package org.eclipse.tractusx.bpdm.pool.service.parser.metadata

import org.eclipse.tractusx.bpdm.pool.entity.RegionDb
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.LegalFormCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.error.LegalFormCreateParseError.TechnicalKeyAlreadyTaken
import org.eclipse.tractusx.bpdm.pool.model.error.LegalFormCreateParseError.UnresolvableAdministrativeArea
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalFormCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.request.LegalFormCreateRequest
import org.eclipse.tractusx.bpdm.pool.repository.LegalFormRepository
import org.eclipse.tractusx.bpdm.pool.repository.RegionRepository
import org.eclipse.tractusx.bpdm.pool.repository.CountryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates a legal form a client asks to create, resolving the administrative area it belongs to.
 */
@Service
class LegalFormCreateParser(
    private val legalFormRepository: LegalFormRepository,
    private val regionRepository: RegionRepository,
    private val countryRepository: CountryRepository
) {
    private companion object {
        val LANGUAGE_CODES: Set<String> = (
            "aa ab ae af ak am an ar as av ay az ba be bg bh bi bm bn bo br bs ca ce ch co cr cs cu cv cy " +
                "da de dv dz ee el en eo es et eu fa ff fi fj fo fr fy ga gd gl gn gu gv ha he hi ho hr ht hu hy hz " +
                "ia id ie ig ii ik io is it iu ja jv ka kg ki kj kk kl km kn ko kr ks ku kv kw ky la lb lg li ln lo lt lu lv " +
                "mg mh mi mk ml mn mr ms mt my na nb nd ne ng nl nn no nr nv ny oc oj om or os pa pi pl ps pt " +
                "qu rm rn ro ru rw sa sc sd se sg si sk sl sm sn so sq sr ss st su sv sw ta te tg th ti tk tl tn to tr " +
                "ts tt tw ty ug uk ur uz ve vi vo wa wo xh yi yo za zh zu"
            ).split(" ").toSet()
    }

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
        val country = request.country?.let { code ->
            countryRepository.findByCountryCode(code)
                ?: run { errors.add(LegalFormCreateParseError.CountryNotFound(code)); null }
        }
        if (request.language != null && request.language !in LANGUAGE_CODES)
            errors.add(LegalFormCreateParseError.InvalidLanguage(request.language))

        if (errors.isNotEmpty()) return ParseResult.Failure(errors)

        return ParseResult.Success(
            LegalFormCreateParsed(
                technicalKey = request.technicalKey,
                name = request.name,
                transliteratedName = request.transliteratedName,
                abbreviations = request.abbreviations,
                transliteratedAbbreviations = request.transliteratedAbbreviations,
                country = country,
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
