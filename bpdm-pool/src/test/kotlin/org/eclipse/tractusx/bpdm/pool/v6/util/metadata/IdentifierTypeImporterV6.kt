/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.v6.util.metadata

import com.opencsv.bean.CsvToBeanBuilder
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierTypeDto
import org.springframework.core.io.Resource
import java.io.InputStreamReader

class IdentifierTypeImporterV6(
    private val identifierTypeResource: Resource
) {

    fun importFromResource(): List<IdentifierTypeDto>{
        val reader = InputStreamReader(identifierTypeResource.inputStream)
        val rows = CsvToBeanBuilder<IdentifierTypeEntry>(reader)
            .withType(IdentifierTypeEntry::class.java)
            .withIgnoreLeadingWhiteSpace(true)
            .withSeparator(';')
            .build()
            .parse()

        val uniqueRows = rows.filterNotNull().groupBy { it.technicalKey }.map { (_, group) -> group.first() }
        val adminAreas = uniqueRows.map { entry ->
            IdentifierTypeDto(
                businessPartnerType = toBusinessPartnerType(entry.businessPartnerType!!),
                technicalKey = entry.technicalKey?.takeIf { it.isNotBlank() }!!,
                name = entry.name?.takeIf { it.isNotBlank() }!!,
                transliteratedName = entry.transliteratedName?.takeIf { it.isNotBlank() },
                abbreviation = entry.abbreviations?.takeIf { it.isNotBlank() },
                transliteratedAbbreviation = entry.transliteratedAbbreviations?.takeIf { it.isNotBlank() },
                details = listOf()
            )
        }

        return adminAreas
    }

    private fun toBusinessPartnerType(typeDescription: String): IdentifierBusinessPartnerType{
        return when(typeDescription){
            "L" -> IdentifierBusinessPartnerType.LEGAL_ENTITY
            "A" -> IdentifierBusinessPartnerType.ADDRESS
            else -> throw RuntimeException("Invalid business partner type '$typeDescription'")
        }
    }
}