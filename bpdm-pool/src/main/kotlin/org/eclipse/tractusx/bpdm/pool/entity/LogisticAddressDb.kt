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

@Entity
@Table(
    name = "logistic_addresses",
    indexes = [
        Index(columnList = "legal_entity_id"),
    ]
)
class LogisticAddressDb(
    @Column(name = "bpn", nullable = false, unique = true)
    var bpn: String,

    @ManyToOne
    @JoinColumn(name = "legal_entity_id")
    var legalEntity: LegalEntityDb?,

    @Column(name = "name")
    var name: String?,

    @Embedded
    var physicalPostalAddress: PhysicalPostalAddressDb,

    @Embedded
    var alternativePostalAddress: AlternativePostalAddressDb?,

    @Embedded
    var confidenceCriteria: ConfidenceCriteriaDb,

    @ElementCollection
    @CollectionTable(name = "address_script_variants", joinColumns = [JoinColumn(name = "logistic_address_id")])
    val scriptVariants: MutableList<LogisticAddressScriptVariantDb> = mutableListOf(),

    ) : BaseEntity() {
    @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], orphanRemoval = true)
    val identifiers: MutableSet<AddressIdentifierDb> = mutableSetOf()

    @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], orphanRemoval = true)
    val states: MutableSet<AddressStateDb> = mutableSetOf()

    @OneToMany(mappedBy = "startAddress", cascade = [CascadeType.ALL], orphanRemoval = true)
    val startAddressRelations: MutableSet<AddressRelationDb> = mutableSetOf()

    @OneToMany(mappedBy = "endAddress", cascade = [CascadeType.ALL], orphanRemoval = true)
    val endAddressRelations: MutableSet<AddressRelationDb> = mutableSetOf()

    /**
     * The sites this address belongs to. A single unified relationship: there is no stored "primary" site — the API's
     * `bpnSite` is derived as the oldest member by `createdAt` and the remainder is exposed as `additionalSites`
     * (see [mainSite] / [additionalSites] in ResponseMappings). A site's main address is also a member of its own set,
     * which is how it is classified (see `getAddressType`).
     */
    @ManyToMany
    @JoinTable(
        name = "address_sites",
        joinColumns = [JoinColumn(name = "address_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "site_id", referencedColumnName = "id")]
    )
    val sites: MutableSet<SiteDb> = mutableSetOf()
}