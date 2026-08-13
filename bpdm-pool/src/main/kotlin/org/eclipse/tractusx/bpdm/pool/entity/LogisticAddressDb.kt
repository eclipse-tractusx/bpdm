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
import org.eclipse.tractusx.bpdm.common.dto.AddressType
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
     * `bpnSite` is derived as the oldest member by `createdAt` and the remainder is exposed as [additionalSites].
     * A site's main address is also a member of its own set, which is how it is classified (see [addressType]).
     */
    @ManyToMany
    @JoinTable(
        name = "address_sites",
        joinColumns = [JoinColumn(name = "address_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "site_id", referencedColumnName = "id")]
    )
    val sites: MutableSet<SiteDb> = mutableSetOf()

    /**
     * The site rendered as the API's `bpnSite` for backward compatibility: the oldest member by `createdAt`, which for
     * pre-existing data is the address's former single site. The remaining members are exposed as [additionalSites].
     */
    val mainSite: SiteDb?
        get() = sites.minByOrNull { it.createdAt }

    val additionalSites: List<SiteDb>
        get() = sites.sortedBy { it.createdAt }.drop(1)

    /** The role this address plays for its legal entity and its sites. */
    val addressType: AddressType
        get() = when {
            isLegalAddress() && isSiteMainAddress() -> AddressType.LegalAndSiteMainAddress
            !isLegalAddress() && !isSiteMainAddress() -> AddressType.AdditionalAddress
            isLegalAddress() -> AddressType.LegalAddress
            isSiteMainAddress() -> AddressType.SiteMainAddress
            else -> throw IllegalStateException("Unable to determine address type.")
        }

    fun scriptCodes(): List<String> = scriptVariants.map { it.scriptCode.technicalKey }

    // Identity is compared by BPN, not by reference: navigating to an address yields a lazy proxy while `this` is always
    // the unproxied instance, so a reference comparison inside the entity reports a false mismatch.
    private fun isLegalAddress() = legalEntity?.legalAddress?.bpn == bpn

    /** An address is a site's main address iff one of the sites it belongs to has it as its main address. */
    private fun isSiteMainAddress() = sites.any { it.mainAddress.bpn == bpn }
}
