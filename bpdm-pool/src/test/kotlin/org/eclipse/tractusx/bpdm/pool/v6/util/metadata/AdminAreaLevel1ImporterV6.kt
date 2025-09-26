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
import com.opencsv.bean.CsvToBeanBuilder
import org.eclipse.tractusx.bpdm.pool.api.model.CountrySubdivisionDto
import org.springframework.core.io.Resource
import java.io.InputStreamReader

class AdminAreaLevel1ImporterV6(
    private val adminAreaResource: Resource
) {

    fun importFromResource(): List<CountrySubdivisionDto>{
        val reader = InputStreamReader(adminAreaResource.inputStream)
        val rows = CsvToBeanBuilder<AdminAreaLevel1Entry>(reader)
            .withType(AdminAreaLevel1Entry::class.java)
            .withIgnoreLeadingWhiteSpace(true)
            .build()
            .parse()

        val uniqueRows = rows.filterNotNull().groupBy { it.code }.map { (_, group) -> group.first() }
        val adminAreas = uniqueRows.map { entry ->
            CountrySubdivisionDto(
                countryCode = CountryCode.getByAlpha2Code(entry.countryCode!!),
                code = entry.code?.takeIf { it.isNotBlank() }!!,
                name = entry.subdivisionName?.takeIf { it.isNotBlank() }!!
            )
        }

        return adminAreas
    }

}