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

package org.eclipse.tractusx.bpdm.pool.entity

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.model.BaseEntity

@Entity
@Table(
<<<<<<<< HEAD:bpdm-pool/src/main/kotlin/org/eclipse/tractusx/bpdm/pool/entity/AddressIdentifier.kt
    name = "address_identifiers",
========
    name = "logistic_addresses",
>>>>>>>> cd7b397a (fix/feat(datamodel/pool):Data model implementation changes):bpdm-pool/src/main/kotlin/org/eclipse/tractusx/bpdm/pool/entity/LogisticAddress.kt
    indexes = [
        Index(columnList = "address_id"),
        Index(columnList = "type_id")
    ]
)
<<<<<<<< HEAD:bpdm-pool/src/main/kotlin/org/eclipse/tractusx/bpdm/pool/entity/AddressIdentifier.kt
class AddressIdentifier(
    @Column(name = "`value`", nullable = false)
    var value: String,

    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false)
    var type: IdentifierType,

    @ManyToOne
    @JoinColumn(name = "address_id", nullable = false)
    var address: LogisticAddress

) : BaseEntity()
========
class LogisticAddress(
    @Column(name = "bpn", nullable = false, unique = true)
    var bpn: String,

    @ManyToOne
    @JoinColumn(name = "legal_entity_id")
    var legalEntity: LegalEntity?,

    @ManyToOne
    @JoinColumn(name = "site_id")
    var site: Site?,

    @Column(name = "name")
    var name: String? = null,

    @Embedded
    var physicalPostalAddress: PhysicalPostalAddress,

    @Embedded
    var alternativePostalAddress: AlternativePostalAddress?

) : BaseEntity() {
    @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], orphanRemoval = true)
    val identifiers: MutableSet<AddressIdentifier> = mutableSetOf()

    @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], orphanRemoval = true)
    val states: MutableSet<AddressState> = mutableSetOf()
}
>>>>>>>> cd7b397a (fix/feat(datamodel/pool):Data model implementation changes):bpdm-pool/src/main/kotlin/org/eclipse/tractusx/bpdm/pool/entity/LogisticAddress.kt
