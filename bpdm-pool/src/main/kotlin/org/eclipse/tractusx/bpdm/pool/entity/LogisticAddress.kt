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

@Entity
@Table(
    name = "logistic_addresses",
    indexes = [
        Index(columnList = "legal_entity_id"),
        Index(columnList = "site_id"),
    ]
)
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
    var name: String?,

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