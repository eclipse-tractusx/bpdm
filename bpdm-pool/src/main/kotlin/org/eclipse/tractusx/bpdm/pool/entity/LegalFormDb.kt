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

package org.eclipse.tractusx.bpdm.pool.entity

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.model.BaseEntity

@Entity
@Table(name = "legal_forms")
class LegalFormDb(
    @Column(name = "technical_key", nullable = false)
    val technicalKey: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "transliterated_name", nullable = false)
    val transliteratedName: String?,

    @Column(name = "abbreviation")
    val abbreviation: String?,

    @Column(name = "transliterated_abbreviations")
    val transliteratedAbbreviations: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "country_code")
    val countryCode: CountryCode?,

    @Enumerated(EnumType.STRING)
    @Column(name = "language_code")
    val languageCode: LanguageCode?,

    @ManyToOne
    @JoinColumn(name = "region_id")
    val administrativeArea: RegionDb?,

    @Column(name = "is_active")
    val isActive: Boolean

) : BaseEntity()
