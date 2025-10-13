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

package org.eclipse.tractusx.bpdm.migration.helper.identifier.type

class ImportEntryValidator {
    fun validate(unvalidatedImportEntry: UnvalidatedImportEntry): ValidatedImportEntry{
        return ValidatedImportEntry(
            businessPartnerType = toBusinessPartnerType(unvalidatedImportEntry.businessPartnerType!!),
            categories = unvalidatedImportEntry.categories!!.split("/").map { IdentifierTypeCategory.valueOf(it) }.toSortedSet(),
            technicalKey = unvalidatedImportEntry.technicalKey?.takeIf { it.isNotBlank() }!!,
            name = unvalidatedImportEntry.name?.takeIf { it.isNotBlank() }!!,
            transliteratedName = unvalidatedImportEntry.transliteratedName?.takeIf { it.isNotBlank() },
            abbreviations = unvalidatedImportEntry.abbreviations?.takeIf { it.isNotBlank() },
            transliteratedAbbreviations = unvalidatedImportEntry.transliteratedAbbreviations?.takeIf { it.isNotBlank() },
            format = unvalidatedImportEntry.format?.takeIf { it.isNotBlank() }
        )
    }

    private fun toBusinessPartnerType(typeDescription: String): BusinessPartnerType{
        return when(typeDescription){
            "L" -> BusinessPartnerType.LEGAL_ENTITY
            "A" -> BusinessPartnerType.ADDRESS
            else -> throw RuntimeException("Invalid business partner type '$typeDescription'")
        }
    }




}