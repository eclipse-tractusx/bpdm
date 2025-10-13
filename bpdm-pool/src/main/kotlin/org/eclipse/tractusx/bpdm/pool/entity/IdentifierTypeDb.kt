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

package org.eclipse.tractusx.bpdm.pool.entity

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.model.BaseEntity
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierTypeCategory

@Entity
@Table(
    name = "identifier_types",
    uniqueConstraints = [UniqueConstraint(
        name = "uc_identifier_types_technical_key_business_partner_type",
        columnNames = ["technical_key", "business_partner_type"]
    )]
)
class IdentifierTypeDb(
    @Column(name = "technical_key", nullable = false)
    val technicalKey: String,

    @Column(name = "business_partner_type", nullable = false)
    @Enumerated(EnumType.STRING)
    val businessPartnerType: IdentifierBusinessPartnerType,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "abbreviation")
    val abbreviation: String? = null,

    @Column(name = "transliterated_name")
    val transliteratedName: String? = null,

    @Column(name = "transliterated_abbreviation")
    val transliteratedAbbreviation: String? = null,

    @Column(name = "format")
    val format: String? = null,

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
        name = "identifier_type_categories",
        joinColumns = [JoinColumn(name = "identifier_type_id", foreignKey = ForeignKey(name = "fk_identifier_categories_types"))],
        uniqueConstraints = [UniqueConstraint(name = "uc_identifier_categories", columnNames = ["identifier_type_id", "category"])],
    )
    @Column(name = "category")
    val categories: MutableSet<IdentifierTypeCategory> = mutableSetOf()

    ) : BaseEntity() {
    @OneToMany(mappedBy = "identifierType", cascade = [CascadeType.ALL], orphanRemoval = true)
    val details: MutableSet<IdentifierTypeDetailDb> = mutableSetOf()
}
