/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.entity.generic

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.BaseEntity
import org.eclipse.tractusx.bpdm.common.model.StageType
import java.util.*

@Entity
@Table(name = "business_partners")
class BusinessPartner(

    @Column(name = "external_id")
    var externalId: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "business_partners_name_parts", joinColumns = [JoinColumn(name = "business_partner_id")])
    @OrderColumn(name = "name_parts_order")
    val nameParts: MutableList<String> = mutableListOf(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "business_partners_roles", joinColumns = [JoinColumn(name = "business_partner_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name")
    val roles: SortedSet<BusinessPartnerRole> = sortedSetOf(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "business_partners_identifiers", joinColumns = [JoinColumn(name = "business_partner_id")])
    val identifiers: SortedSet<Identifier> = sortedSetOf(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "business_partners_states", joinColumns = [JoinColumn(name = "business_partner_id")])
    val states: SortedSet<State> = sortedSetOf(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "business_partners_classifications", joinColumns = [JoinColumn(name = "business_partner_id")])
    val classifications: SortedSet<Classification> = sortedSetOf(),

    @Column(name = "short_name")
    var shortName: String? = null,

    @Column(name = "legal_name")
    var legalName: String? = null,

    @Column(name = "legal_form")
    var legalForm: String? = null,

    @Column(name = "is_own_company_data", nullable = false)
    var isOwnCompanyData: Boolean = false,

    @Column(name = "bpnl")
    var bpnL: String? = null,

    @Column(name = "bpns")
    var bpnS: String? = null,

    @Column(name = "bpna")
    var bpnA: String? = null,

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "postal_address_id", unique = true)
    var postalAddress: PostalAddress,

    @Column(name = "stage")
    @Enumerated(EnumType.STRING)
    var stage: StageType,

    @Column(name = "parent_id")
    var parentId: String? = null,

    @Column(name = "parent_type")
    @Enumerated(EnumType.STRING)
    var parentType: BusinessPartnerType? = null

) : BaseEntity()

