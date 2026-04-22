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

package org.eclipse.tractusx.bpdm.pool.v7.util

import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierTypeCategory
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierTypeDto
import org.eclipse.tractusx.bpdm.pool.util.metadata.IdentifierTypeEntryImporter

class IdentifierTypeV7Importer(
    private val identifierTypeEntryImporter: IdentifierTypeEntryImporter
) {

    fun importFromResource(): List<IdentifierTypeDto> {
        val entries = identifierTypeEntryImporter.importFromResource()
        return entries.map { entry ->
            IdentifierTypeDto(
                technicalKey = entry.technicalKey?.takeIf { it.isNotBlank() }!!,
                businessPartnerType = toBusinessPartnerType(entry.businessPartnerType!!),
                name = entry.name?.takeIf { it.isNotBlank() }!!,
                abbreviation = entry.abbreviations?.takeIf { it.isNotBlank() },
                transliteratedName = entry.transliteratedName?.takeIf { it.isNotBlank() },
                transliteratedAbbreviation = entry.transliteratedAbbreviations?.takeIf { it.isNotBlank() },
                format = entry.format,
                details = listOf(),
                categories = entry.categories?.split("/")?.map { toCategory(it) }?.toSortedSet() ?: sortedSetOf()
            )
        }
    }

    private fun toBusinessPartnerType(typeDescription: String): IdentifierBusinessPartnerType{
        return when(typeDescription){
            "L" -> IdentifierBusinessPartnerType.LEGAL_ENTITY
            "A" -> IdentifierBusinessPartnerType.ADDRESS
            else -> throw RuntimeException("Invalid business partner type '$typeDescription'")
        }
    }

    private fun toCategory(categoryDescription: String): IdentifierTypeCategory{
        return IdentifierTypeCategory.valueOf(categoryDescription.uppercase())
    }
}