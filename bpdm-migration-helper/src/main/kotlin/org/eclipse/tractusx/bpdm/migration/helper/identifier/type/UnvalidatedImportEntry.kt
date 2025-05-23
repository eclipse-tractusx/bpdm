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

import com.opencsv.bean.CsvBindByName
import org.eclipse.tractusx.bpdm.migration.helper.util.NoArg

@NoArg
data class UnvalidatedImportEntry(
    @CsvBindByName(column = "BPT")
    val businessPartnerType: String?,
    @CsvBindByName(column = "Category")
    val categories: String?,
    @CsvBindByName(column = "ITC")
    val technicalKey: String?,
    @CsvBindByName(column = "Name")
    val name: String?,
    @CsvBindByName(column = "Transliterated Name")
    val transliteratedName: String?,
    @CsvBindByName(column = "Abbr")
    val abbreviations: String?,
    @CsvBindByName(column = "TAbbr")
    val transliteratedAbbreviations: String?,
    @CsvBindByName(column = "Format")
    val format: String?,
)
