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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound

import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierTypeDetailDto
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierTypeDto
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.springframework.stereotype.Component

/**
 * Maps stored identifier types to the v7 API identifier type DTOs.
 */
@Component
class IdentifierTypeResponseMapper {

    /**
     * Returns the given identifier type as the API reports it.
     */
    fun toIdentifierType(identifierType: IdentifierTypeDb): IdentifierTypeDto =
        IdentifierTypeDto(
            technicalKey = identifierType.technicalKey,
            businessPartnerType = identifierType.businessPartnerType,
            name = identifierType.name,
            abbreviation = identifierType.abbreviation,
            transliteratedName = identifierType.transliteratedName,
            transliteratedAbbreviation = identifierType.transliteratedAbbreviation,
            format = identifierType.format,
            categories = identifierType.categories.toSortedSet(),
            details = identifierType.details.map { IdentifierTypeDetailDto(it.countryCode, it.mandatory) }
        )

    /**
     * Returns the given identifier type as the key/name pair by which identifiers reference their type.
     */
    fun toTypeKeyName(identifierType: IdentifierTypeDb): TypeKeyNameVerboseDto<String> =
        TypeKeyNameVerboseDto(identifierType.technicalKey, identifierType.name)
}
