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

package org.eclipse.tractusx.bpdm.pool.util.metadata

import com.opencsv.bean.CsvToBeanBuilder
import org.springframework.core.io.ResourceLoader
import java.io.InputStreamReader

class TestMetadataEntryImporter(
    private val resourceLoader: ResourceLoader
) {

    fun <T: Any> importFromResource(resourcePath: String, clazz: Class<T>, separator: Char): List<T> {
        val resource = resourceLoader.getResource(resourcePath)

        val reader = InputStreamReader(resource.inputStream)
        val rows = CsvToBeanBuilder<T>(reader)
            .withType(clazz)
            .withIgnoreLeadingWhiteSpace(true)
            .withSeparator(separator)
            .build()
            .parse()

        return rows.filterNotNull()
    }
}