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

import org.eclipse.tractusx.bpdm.common.model.BusinessPartnerType
import java.time.Instant
import jakarta.persistence.*

@Entity
@Table(
    name = "legal_entities",
    indexes = [Index(columnList = "legal_form_id")]
)
class LegalEntity(
    @Column(name = "bpn", nullable = false, unique = true)
    var bpn: String,
    @ManyToOne
    @JoinColumn(name = "legal_form_id")
    var legalForm: LegalForm?,
    @ElementCollection(targetClass = BusinessPartnerType::class)
    @JoinTable(name = "legal_entity_types", joinColumns = [JoinColumn(name = "legal_entity_id")], indexes = [Index(columnList = "legal_entity_id")])
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    var types: MutableSet<BusinessPartnerType>,
    @ManyToMany(cascade = [CascadeType.ALL])
    @JoinTable(
        name = "legal_entity_roles",
        joinColumns = [JoinColumn(name = "legal_entity_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")],
        indexes = [Index(columnList = "legal_entity_id")]
    )
    val roles: MutableSet<Role>,
    @Column(name = "currentness", nullable = false)
    var currentness: Instant,
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "legal_address_id", nullable = false)
    var legalAddress: Address
) : BaseEntity() {
    @OneToMany(mappedBy = "legalEntity", cascade = [CascadeType.ALL], orphanRemoval = true)
    val identifiers: MutableSet<Identifier> = mutableSetOf()

    @OneToMany(mappedBy = "legalEntity", cascade = [CascadeType.ALL], orphanRemoval = true)
    val names: MutableSet<Name> = mutableSetOf()

    @OneToMany(mappedBy = "legalEntity", cascade = [CascadeType.ALL], orphanRemoval = true)
    val stati: MutableSet<BusinessStatus> = mutableSetOf()

    @OneToMany(mappedBy = "legalEntity", cascade = [CascadeType.ALL], orphanRemoval = true)
    val addresses: MutableSet<AddressPartner> = mutableSetOf()

    @OneToMany(mappedBy = "legalEntity", cascade = [CascadeType.ALL], orphanRemoval = true)
    val sites: MutableSet<Site> = mutableSetOf()

    @OneToMany(mappedBy = "legalEntity", cascade = [CascadeType.ALL], orphanRemoval = true)
    val classification: MutableSet<Classification> = mutableSetOf()

    @OneToMany(mappedBy = "legalEntity", cascade = [CascadeType.ALL], orphanRemoval = true)
    val bankAccounts: MutableSet<BankAccount> = mutableSetOf()

    @OneToMany(mappedBy = "startNode", cascade = [CascadeType.ALL], orphanRemoval = true)
    val startNodeRelations: MutableSet<Relation> = mutableSetOf()

    @OneToMany(mappedBy = "endNode", cascade = [CascadeType.ALL], orphanRemoval = true)
    val endNodeRelations: MutableSet<Relation> = mutableSetOf()
}

