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

package org.eclipse.tractusx.bpdm.migration.helper.country

class CountrySqlMapper {

    companion object {
        const val countryInsertHeader = "INSERT INTO countries (id, uuid, created_at, updated_at, country_code, name, description)"
    }

    fun createSql(entries: List<ValidatedCountryImportEntry>): String {
        val builder = StringBuilder()
        entries.forEach { builder.appendLine(createCountrySql(it)) }
        return builder.toString()
    }

    private fun createCountrySql(entry: ValidatedCountryImportEntry): String {
        val builder = StringBuilder()
        builder.appendLine(countryInsertHeader)
        builder.append("VALUES ")
        builder.append("(nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP")
        builder.append(toInsertValue(entry.countryCode))
        builder.append(toInsertValue(entry.name))
        builder.append(toInsertValue(entry.description))
        builder.appendLine(")")
        builder.appendLine("ON CONFLICT (country_code) DO NOTHING;")
        return builder.toString()
    }

    private fun toInsertValue(value: String?): String {
        return if (value == null) ", NULL" else ", '${value.replace("'", "''")}'"
    }

}
