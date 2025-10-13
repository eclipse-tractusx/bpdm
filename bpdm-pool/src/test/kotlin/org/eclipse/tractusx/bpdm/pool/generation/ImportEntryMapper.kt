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

package org.eclipse.tractusx.bpdm.pool.generation

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.mapping.BpdmMapper
import org.eclipse.tractusx.bpdm.common.mapping.MappingResult
import org.eclipse.tractusx.bpdm.common.mapping.ValidationContext
import org.eclipse.tractusx.bpdm.common.mapping.ValidationContext.Companion.onProperty
import org.eclipse.tractusx.bpdm.common.mapping.types.FilledHugeString
import org.eclipse.tractusx.bpdm.common.mapping.types.FilledLongString
import org.eclipse.tractusx.bpdm.common.mapping.types.FilledTinyString
import org.eclipse.tractusx.bpdm.pool.entity.RegionDb
import org.eclipse.tractusx.bpdm.pool.generation.GleifCodesGenerationIT.UnvalidatedImportEntry
import org.eclipse.tractusx.bpdm.pool.generation.GleifCodesGenerationIT.ValidatedImportEntry

class ImportEntryMapper(
    private val countryCodeMapper: BpdmMapper<String, CountryCode>,
    private val languageCodeMapper: BpdmMapper<String, LanguageCode>,
    private val adminAreaMapper: BpdmMapper<String, RegionDb>,
    private val elfStatusMapper: BpdmMapper<String, Boolean>
): BpdmMapper<UnvalidatedImportEntry, ValidatedImportEntry> {

    override fun checkTreatAsNull(valueToMap: UnvalidatedImportEntry): Boolean {
        return valueToMap.elfCode.isNullOrBlank() || valueToMap.name.isNullOrBlank()
    }

    override fun map(
        valueToMap: UnvalidatedImportEntry,
        context: ValidationContext
    ): MappingResult<ValidatedImportEntry> {

        //Create validated properties
        val elfCode =  FilledTinyString.mapInvalidNull(valueToMap.elfCode, context.onProperty(UnvalidatedImportEntry::elfCode))
        val name = FilledHugeString.mapInvalidNull(valueToMap.name, context.onProperty(UnvalidatedImportEntry::name))
        val transliteratedName = FilledHugeString.mapValidNull(valueToMap.transliteratedName, context.onProperty(UnvalidatedImportEntry::transliteratedName))
        val abbreviation = FilledLongString.mapValidNull(valueToMap.abbreviation, context.onProperty(UnvalidatedImportEntry::abbreviation))
        val transliteratedAbbreviations = FilledLongString.mapValidNull(valueToMap.transliteratedAbbreviations, context.onProperty(UnvalidatedImportEntry::transliteratedAbbreviations))
        val countryCode = countryCodeMapper.mapValidNull(valueToMap.countryCode, context.onProperty(UnvalidatedImportEntry::countryCode))
        val languageCode = languageCodeMapper.mapValidNull(valueToMap.languageCode, context.onProperty(UnvalidatedImportEntry::languageCode))
        val adminArea = adminAreaMapper.mapValidNull(valueToMap.countrySubdivisionCode, context.onProperty(UnvalidatedImportEntry::countrySubdivisionCode))
        val status = elfStatusMapper.mapInvalidNull(valueToMap.elfStatus, context.onProperty(UnvalidatedImportEntry::elfStatus))


        //Gather errors
        val validationErrors = listOf(elfCode.errors, name.errors, transliteratedName.errors, abbreviation.errors, transliteratedAbbreviations.errors,
            countryCode.errors, languageCode.errors, adminArea.errors, status.errors).flatten()


        //Return mapping result
        return MappingResult.invalidOnError(validationErrors) {
            ValidatedImportEntry(
                _elfCode = elfCode.successfulResult,
                _name = name.successfulResult,
                _transliteratedName = transliteratedName.successfulResult,
                _abbreviations = abbreviation.successfulResult,
                _transliteratedAbbreviations = transliteratedAbbreviations.successfulResult,
                countryCode = countryCode.successfulResult,
                languageCode = languageCode.successfulResult,
                administrativeAreaLevel1 = adminArea.successfulResult,
                isActive = status.successfulResult
            )
        }
    }
}