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

package org.eclipse.tractusx.bpdm.pool.v7.util

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.pool.api.model.CountrySubdivisionDto
import org.eclipse.tractusx.bpdm.pool.util.metadata.AdminAreaLevel1EntryImporter

class AdminAreaLevel1V7Importer(
    private val adminAreaLevel1EntryImporter: AdminAreaLevel1EntryImporter
) {

    fun importFromResource(): List<CountrySubdivisionDto> {
        val entries = adminAreaLevel1EntryImporter.importFromResource()
        return entries.map { entry ->
            CountrySubdivisionDto(
                countryCode = CountryCode.getByAlpha2Code(entry.countryCode!!),
                code = entry.code?.takeIf { it.isNotBlank() }!!,
                name = entry.subdivisionName?.takeIf { it.isNotBlank() }!!
            )
        }
    }
}