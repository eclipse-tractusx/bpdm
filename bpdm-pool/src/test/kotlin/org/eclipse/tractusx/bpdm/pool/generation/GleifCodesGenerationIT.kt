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

package org.eclipse.tractusx.bpdm.pool.generation

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import com.opencsv.bean.CsvBindByName
import com.opencsv.bean.CsvToBeanBuilder
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.mapping.BpdmCountryCodeMapper
import org.eclipse.tractusx.bpdm.common.mapping.BpdmLanguageCodeMapper
import org.eclipse.tractusx.bpdm.common.mapping.ValidationContext
import org.eclipse.tractusx.bpdm.common.mapping.types.FilledHugeString
import org.eclipse.tractusx.bpdm.common.mapping.types.FilledLongString
import org.eclipse.tractusx.bpdm.common.mapping.types.FilledTinyString
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.entity.RegionDb
import org.eclipse.tractusx.bpdm.pool.repository.RegionRepository
import org.eclipse.tractusx.bpdm.pool.util.TestHelpers
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ResourceLoader
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter

/**
 * This is not a real test but more of a comfortable way to generate
 * legal form migration script from the given GLEIF CSV
 *
 * Therefore, the test is disabled on default
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles("test-no-auth")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class GleifCodesGenerationIT @Autowired constructor(
    private val resourceLoader: ResourceLoader,
    private val regionRepository: RegionRepository
) {

    private val logger = KotlinLogging.logger { }

    private val importEntryMapper = ImportEntryMapper(
        countryCodeMapper = BpdmCountryCodeMapper(),
        languageCodeMapper = BpdmLanguageCodeMapper(),
        adminAreaMapper = AdminAreaMapper(regionRepository),
        elfStatusMapper = ElfStatusMapper()
    )

    private val insertHeader = createInsertHeader()

    @Test
    @Disabled
    fun build() {
        val gleifCodesCsv = resourceLoader.getResource("classpath:legalforms/2023-09-28-elf-code-list-v1.5.csv")

        val reader = InputStreamReader(gleifCodesCsv.inputStream)
        val rows = CsvToBeanBuilder<UnvalidatedImportEntry>(reader)
            .withType(UnvalidatedImportEntry::class.java)
            .withIgnoreLeadingWhiteSpace(true)
            .build()
            .parse()

        val uniqueRows = rows.groupBy { it.elfCode }.map { (_, group) -> group.first() }

        val mappingResults = uniqueRows.mapIndexed{ index, entry ->
            importEntryMapper.mapValidNull(entry, ValidationContext.fromRoot(UnvalidatedImportEntry::class, index.toString()))
        }

        val (successfulResults, errorResults) = mappingResults.partition { it.isSuccess }

        logger.warn { "Could not read the following entries: ${errorResults.joinToString()}" }

        val sqlOutput = File("${gleifCodesCsv.file.parent}/create-legal-forms.sql")
        val sqlPrinter = PrintWriter(sqlOutput)

        successfulResults
            .mapNotNull { it.successfulResult }
            .sortedBy { it.administrativeAreaLevel1 == null }
            .forEach { legalForm ->
                val sqlEntry = if(legalForm.administrativeAreaLevel1 == null)
                    toSqlEntry(legalForm)
                else
                    toReferencingSqlEntry(legalForm)

                sqlPrinter.appendLine(sqlEntry)
                sqlPrinter.flush()
        }
    }

    fun toSqlEntry(validatedImportEntry: ValidatedImportEntry): String{
        val builder = StringBuilder()
        builder.appendLine(insertHeader)
        builder.append("VALUES ")
        builder.append("(nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP")
        builder.append(toSqlScriptValue(validatedImportEntry.elfCode))
        builder.append(toSqlScriptValue(validatedImportEntry.name))
        builder.append(toSqlScriptValue(validatedImportEntry.transliteratedName))
        builder.append(toSqlScriptValue(validatedImportEntry.abbreviations))
        builder.append(toSqlScriptValue(validatedImportEntry.transliteratedAbbreviations))
        builder.append(toSqlScriptValue(validatedImportEntry.countryCode?.toString()))
        builder.append(toSqlScriptValue(validatedImportEntry.languageCode?.toString()))
        builder.append(toSqlScriptValue(validatedImportEntry.administrativeAreaLevel1?.id))
        builder.append(toSqlScriptValue(validatedImportEntry.isActive))
        builder.append(");")

        return builder.toString()
    }

    fun toReferencingSqlEntry(validatedImportEntry: ValidatedImportEntry): String{
        val builder = StringBuilder()
        builder.appendLine(insertHeader)
        builder.append("SELECT ")
        builder.append("nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP")
        builder.append(toSqlScriptValue(validatedImportEntry.elfCode))
        builder.append(toSqlScriptValue(validatedImportEntry.name))
        builder.append(toSqlScriptValue(validatedImportEntry.transliteratedName))
        builder.append(toSqlScriptValue(validatedImportEntry.abbreviations))
        builder.append(toSqlScriptValue(validatedImportEntry.transliteratedAbbreviations))
        builder.append(toSqlScriptValue(validatedImportEntry.countryCode?.toString()))
        builder.append(toSqlScriptValue(validatedImportEntry.languageCode?.toString()))
        builder.append(", regions.id")
        builder.append(toSqlScriptValue(validatedImportEntry.isActive))
        builder.append(" FROM regions WHERE region_code = '${validatedImportEntry.administrativeAreaLevel1!!.regionCode}'")
        builder.append(";")

        return builder.toString()
    }

    private fun createInsertHeader(): String{
        val builder = StringBuilder()
        builder.append("INSERT INTO legal_forms (id ,uuid, created_at , updated_at")
        builder.append(", technical_key")
        builder.append(", name")
        builder.append(", transliterated_name")
        builder.append(", abbreviation")
        builder.append(", transliterated_abbreviations")
        builder.append(", country_code")
        builder.append(", language_code")
        builder.append(", region_id")
        builder.append(", is_active")
        builder.append(")")

        return builder.toString()
    }

    fun toSqlScriptValue(value: String?): String {
        return if (value == null) ", NULL" else ", '${value.replace("'", "''")}'"
    }

    fun toSqlScriptValue(value: Boolean?): String {
        return ", $value"
    }

    fun toSqlScriptValue(value: Long?): String {
        return ", $value"
    }

    /**
     * Represents the DTO from 'outside' which has no formal validation yet
     * We are very permissive with the types here to gather all the values so we can validate them more precisely later
     */
    data class UnvalidatedImportEntry(
        @CsvBindByName(column = "ELF Code")
        val elfCode: String? = null,
        @CsvBindByName(column = "Entity Legal Form name Local name")
        val name: String? = null,
        @CsvBindByName(column = "Abbreviations Local language")
        val abbreviation: String? = null,
        @CsvBindByName(column = "Language Code (ISO 639-1)")
        val languageCode: String? = null,
        @CsvBindByName(column = "Country Code (ISO 3166-1)")
        val countryCode: String? = null,
        @CsvBindByName(column = "Country sub-division code (ISO 3166-2)")
        val countrySubdivisionCode: String? = null,
        @CsvBindByName(column = "Entity Legal Form name Transliterated name (per ISO 01-140-10)")
        val transliteratedName: String? = null,
        @CsvBindByName(column = "Abbreviations transliterated")
        val transliteratedAbbreviations: String? = null,
        @CsvBindByName(column = "ELF Status ACTV/INAC")
        val elfStatus: String? = null
    )


    /**
     * The DTO for an import entry containing fully validated entries
     */
    data class ValidatedImportEntry(
        private val _elfCode: FilledTinyString,
        private val _name: FilledHugeString,
        private val _transliteratedName: FilledHugeString?,
        private val _abbreviations: FilledLongString?,
        private val _transliteratedAbbreviations: FilledLongString?,
        val countryCode: CountryCode?,
        val languageCode: LanguageCode?,
        val administrativeAreaLevel1: RegionDb?,
        val isActive: Boolean
    ){
        val elfCode = _elfCode.value
        val name = _name.value
        val transliteratedName = _transliteratedName?.value
        val abbreviations = _abbreviations?.value
        val transliteratedAbbreviations = _transliteratedAbbreviations?.value
    }
}














