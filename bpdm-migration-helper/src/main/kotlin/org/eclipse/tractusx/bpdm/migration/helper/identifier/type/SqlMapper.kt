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

class SqlMapper {

    companion object{
        const val identifierTypeInsertHeader = "INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)"
        const val identifierCategoryInsertHeader = "INSERT INTO identifier_type_categories (identifier_type_id, category)"
    }

    fun createSql(entries: List<ValidatedImportEntry>): String{
        val builder = StringBuilder()
        entries.forEach {  builder.appendLine(createIdentifierTypeSql(it)) }
        entries.forEach {  builder.appendLine(createIdentifierCategorySql(it)) }
        return builder.toString()
    }

    private fun createIdentifierTypeSql(entry: ValidatedImportEntry): String{
        val builder = StringBuilder()
        builder.appendLine(identifierTypeInsertHeader)
        builder.append("VALUES ")
        builder.append("(nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP")
        builder.append(toInsertValue(entry.technicalKey))
        builder.append(toInsertValue(entry.businessPartnerType.name))
        builder.append(toInsertValue(entry.name))
        builder.append(toInsertValue(entry.transliteratedName))
        builder.append(toInsertValue(entry.abbreviations))
        builder.append(toInsertValue(entry.transliteratedAbbreviations))
        builder.append(toInsertValue(entry.format))
        builder.appendLine(")")
        builder.appendLine(" ON CONFLICT (technical_key, business_partner_type)")
        builder.append("DO UPDATE SET")
        builder.append(" name = ${toUpdateValue(entry.name)}")
        builder.append(", transliterated_name = ${toUpdateValue(entry.transliteratedName)}")
        builder.append(", abbreviation = ${toUpdateValue(entry.abbreviations)}")
        builder.append(", transliterated_abbreviation = ${toUpdateValue(entry.transliteratedAbbreviations)}")
        builder.append(", format = ${toUpdateValue(entry.format)}")
        builder.appendLine(";")
        return builder.toString()
    }

    private fun createIdentifierCategorySql(entry: ValidatedImportEntry): String{
        val builder = StringBuilder()
        entry.categories.map { createIdentifierCategorySql(entry.technicalKey, entry.businessPartnerType, it) }.forEach { builder.appendLine(it) }
        return builder.toString()
    }

    private fun createIdentifierCategorySql(technicalKey: String, businessPartnerType: BusinessPartnerType, category: IdentifierTypeCategory): String{
        val builder = StringBuilder()
        builder.appendLine(identifierCategoryInsertHeader)
        builder.append("SELECT ")
        builder.append("identifier_types.id")
        builder.append(toInsertValue(category.name))
        builder.append(" FROM identifier_types WHERE technical_key = '$technicalKey' AND business_partner_type = '${businessPartnerType.name}'")
        builder.append(";")

        return builder.toString()
    }

    private fun toInsertValue(value: String?): String {
        return if (value == null) ", NULL" else ", '${value.replace("'", "''")}'"
    }

    private  fun toUpdateValue(value: String?): String{
        return if (value == null) "NULL" else "'${value.replace("'", "''")}'"
    }
}