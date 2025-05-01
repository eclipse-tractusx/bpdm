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

package org.eclipse.tractusx.bpdm.migration.helper.identifier.type

import com.opencsv.bean.CsvToBeanBuilder
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter

class MigrationFileCreator {

    fun create(){
        val importEntryValidator = ImportEntryValidator()
        val sqlMapper = SqlMapper()

        val identifierTypesCsv = Thread.currentThread().contextClassLoader.getResourceAsStream("identifier_types/identifier-types.csv")!!

        val reader = InputStreamReader(identifierTypesCsv)
        val csvParser = CsvToBeanBuilder<UnvalidatedImportEntry>(reader)
            .withType(UnvalidatedImportEntry::class.java)
            .withIgnoreLeadingWhiteSpace(true)
            .withSeparator(';')
            .build()

        val importEntries = csvParser.parse()

        val validatedEntries = importEntries.map { importEntryValidator.validate(it) }
        val sql = sqlMapper.createSql(validatedEntries)

        val sqlOutput = File("create_identifier_types.sql")
        val sqlPrinter = PrintWriter(sqlOutput)
        sqlPrinter.print(sql)
        sqlPrinter.flush()
    }

}