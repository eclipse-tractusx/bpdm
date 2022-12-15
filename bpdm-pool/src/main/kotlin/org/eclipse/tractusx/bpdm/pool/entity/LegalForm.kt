/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

import com.neovisionaries.i18n.LanguageCode
import jakarta.persistence.*

@Entity
@Table(name = "legal_forms")
class LegalForm(
    @Column(name = "name")
    val name: String?,
    @Column(name = "url")
    val url: String?,
    @Column(name = "language", nullable = false)
    val language: LanguageCode,
    @Column(name = "abbreviation")
    val mainAbbreviation: String?,
    @ManyToMany(cascade = [CascadeType.ALL])
    @JoinTable(
        name = "legal_forms_legal_categories",
        joinColumns = [JoinColumn(name = "form_id")],
        inverseJoinColumns = [JoinColumn(name = "category_id")],
        indexes = [Index(columnList = "form_id"), Index(columnList = "category_id")]
    )
    val categories: MutableSet<LegalFormCategory>,
    @Column(name = "technical_key", nullable = false)
    val technicalKey: String
) : BaseEntity()