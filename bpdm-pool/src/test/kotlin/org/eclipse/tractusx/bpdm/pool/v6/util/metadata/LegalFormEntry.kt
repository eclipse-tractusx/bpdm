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

package org.eclipse.tractusx.bpdm.pool.v6.util.metadata

import com.opencsv.bean.CsvBindByName
import org.eclipse.tractusx.bpdm.common.util.NoArg

@NoArg
data class LegalFormEntry (
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