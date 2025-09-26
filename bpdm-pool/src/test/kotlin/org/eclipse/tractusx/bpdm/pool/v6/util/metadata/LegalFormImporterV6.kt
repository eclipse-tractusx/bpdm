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

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import com.opencsv.bean.CsvToBeanBuilder
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalFormDto
import org.springframework.core.io.Resource
import java.io.InputStreamReader

class LegalFormImporterV6(
    private val legalFormResource: Resource
) {
    fun importFromResource(): List<LegalFormDto>{
        val reader = InputStreamReader(legalFormResource.inputStream)
        val rows = CsvToBeanBuilder<LegalFormEntry>(reader)
            .withType(LegalFormEntry::class.java)
            .withIgnoreLeadingWhiteSpace(true)
            .build()
            .parse()

        val uniqueRows = rows
            .filterNotNull()
            .filter { it.elfCode?.takeIf { it.isNotBlank() } != null }
            .filter { it.name?.takeIf { it.isNotBlank() } != null }
            .groupBy { it.elfCode }
            .map { (_, group) -> group.first() }
        val legalForms = uniqueRows.map { entry ->
            LegalFormDto(
                technicalKey = entry.elfCode!!,
                name = entry.name!!,
                transliteratedName = entry.transliteratedName?.takeIf { it.isNotBlank() },
                abbreviation = entry.abbreviation?.takeIf { it.isNotBlank() },
                country = entry.countryCode?.let { CountryCode.getByAlpha2Code(it) },
                language = entry.languageCode?.let { LanguageCode.getByCodeIgnoreCase(it) },
                administrativeAreaLevel1 = entry.countrySubdivisionCode?.takeIf { it.isNotBlank() },
                transliteratedAbbreviations = entry.transliteratedAbbreviations?.takeIf { it.isNotBlank() },
                isActive = entry.elfStatus?.uppercase() == "ACTV"
            )
        }

        return legalForms
    }
}