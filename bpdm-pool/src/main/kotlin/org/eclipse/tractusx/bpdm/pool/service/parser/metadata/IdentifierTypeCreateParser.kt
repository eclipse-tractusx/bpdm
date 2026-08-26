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

import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierTypeCategory
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.IdentifierTypeCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.error.IdentifierTypeCreateParseError.TechnicalKeyAlreadyTaken
import org.eclipse.tractusx.bpdm.pool.model.parsed.IdentifierTypeCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.IdentifierTypeDetailParsed
import org.eclipse.tractusx.bpdm.pool.model.request.IdentifierTypeCreateRequest
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.springframework.stereotype.Service

/**
 * Validates an identifier type a client asks to create.
 */
@Service
class IdentifierTypeCreateParser(
    private val identifierTypeRepository: IdentifierTypeRepository
) {

    /**
     * Validates the request and reports either the validated identifier type or that its technical key is already taken
     * for the business partner type it applies to. An identifier type named without a category falls under `OTH`.
     */
    fun parse(request: IdentifierTypeCreateRequest): ParseResult<IdentifierTypeCreateParsed, IdentifierTypeCreateParseError> {
        val existing = identifierTypeRepository.findByBusinessPartnerTypeAndTechnicalKey(request.businessPartnerType, request.technicalKey)
        if (existing != null)
            return ParseResult.ofSingleFailure(TechnicalKeyAlreadyTaken(request.technicalKey, request.businessPartnerType))

        return ParseResult.Success(
            IdentifierTypeCreateParsed(
                technicalKey = request.technicalKey,
                businessPartnerType = request.businessPartnerType,
                name = request.name,
                abbreviation = request.abbreviation,
                transliteratedName = request.transliteratedName,
                transliteratedAbbreviation = request.transliteratedAbbreviation,
                format = request.format,
                categories = request.categories.ifEmpty { setOf(IdentifierTypeCategory.OTH) },
                details = request.details.map { IdentifierTypeDetailParsed(it.country, it.mandatory) }
            )
        )
    }
}
