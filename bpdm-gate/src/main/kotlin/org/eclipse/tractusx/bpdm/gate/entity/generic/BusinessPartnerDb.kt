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

package org.eclipse.tractusx.bpdm.gate.entity.generic

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.model.BaseEntity
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import java.time.Instant
import java.util.*

@Entity
@Table(name = "business_partners")
class BusinessPartnerDb(

    @ManyToOne
    @JoinColumn(name = "sharing_state_id", nullable = false)
    var sharingState: SharingStateDb,

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
    val identifiers: SortedSet<IdentifierDb> = sortedSetOf(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "business_partners_states", joinColumns = [JoinColumn(name = "business_partner_id")])
    val states: SortedSet<StateDb> = sortedSetOf(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "business_partners_classifications", joinColumns = [JoinColumn(name = "business_partner_id")])
    val classifications: SortedSet<ClassificationDb> = sortedSetOf(),

    @Column(name = "short_name")
    var shortName: String? = null,

    @Column(name = "legal_name")
    var legalName: String? = null,

    @Column(name = "site_name")
    var siteName: String? = null,

    @Column(name = "address_name")
    var addressName: String? = null,

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
    var postalAddress: PostalAddressDb,

    @Column(name = "stage")
    @Enumerated(EnumType.STRING)
    var stage: StageType,

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "legal_entity_confidence_id", unique = true)
    var legalEntityConfidence: ConfidenceCriteriaDb?,

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "site_confidence_id", unique = true)
    var siteConfidence: ConfidenceCriteriaDb?,

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "address_confidence_id", unique = true)
    var addressConfidence: ConfidenceCriteriaDb?,

    @Column(name = "external_sequence_timestamp")
    var externalSequenceTimestamp: Instant? = null,

    ) : BaseEntity() {

    companion object {
        fun createEmpty(sharingState: SharingStateDb, stage: StageType) =
            BusinessPartnerDb(
                sharingState = sharingState,
                stage = stage,
                postalAddress = PostalAddressDb(),
                legalEntityConfidence = null,
                siteConfidence = null,
                addressConfidence = null
            )
    }
}

