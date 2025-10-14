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

package org.eclipse.tractusx.bpdm.pool.util

import com.neovisionaries.i18n.CountryCode
import org.apache.commons.csv.CSVFormat
import org.eclipse.tractusx.bpdm.pool.entity.RegionDb
import java.io.InputStream

/**
 * Tool to create a SQL script to write regions read from a CSV file into a database
 */
class CreateSQLScriptToInsertRegions {

    fun readCsv(inputStream: InputStream): List<RegionDb> =
        CSVFormat.Builder.create(CSVFormat.DEFAULT).apply {
            setIgnoreSurroundingSpaces(true)
        }.get().parse(inputStream.reader())
            .drop(1) // Dropping the header
            .map {
                RegionDb(
                    countryCode = CountryCode.getByAlpha2Code(it[0]),
                    regionName = it[1],
                    regionCode = it[2],
                )
            }

    fun createSqlScript(regions: List<RegionDb>): String {

        val builder = StringBuilder()
        regions
            .filter { region -> region.regionCode.isNotBlank() && region.regionCode != "-" }
            .forEach { region ->
                builder.append("insert into regions")
                    .append("(id ,uuid, created_at , updated_at,")
                    .appendLine("country_code, region_code, region_name)")
                    .append("    values ")
                    .append("(nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP,")
                    .append("'").append(region.countryCode.alpha2).append("',")
                    .append("'").append(region.regionCode).append("',")
                    .append("'").append(region.regionName.replace("'", "''")).append("')")
                    .appendLine(";")
            }
        return builder.toString()
    }

}

/**
 * Start generation of SQL - script. Copy result to a migration file in bpdm-pool/src/main/resources/db/migration
 * Second main conflicts with spring boot repackage. Uncomment to start generation.
 */
/*
fun generate() {

    val regionsCsvStream = CreateSQLScriptToInsertRegions::class.java.getResourceAsStream("/regions/IP2LOCATION-ISO3166-2.CSV")
    if (regionsCsvStream != null) {
        val regions = CreateSQLScriptToInsertRegions().readCsv(regionsCsvStream)
        println(CreateSQLScriptToInsertRegions().createSqlScript(regions))
    }
}*/
