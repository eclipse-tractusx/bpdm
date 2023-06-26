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

package org.eclipse.tractusx.bpdm.gate.entity

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.model.BaseEntity
import org.eclipse.tractusx.bpdm.common.model.OutputInputEnum

@Entity
@Table(
    name = "logistic_addresses",
    indexes = [
        Index(columnList = "legal_entity_id"),
        Index(columnList = "site_id"),
    ]
)
class LogisticAddress(
    @Column(name = "bpn")
    var bpn: String? = null,

    @Column(name = "external_id", nullable = false, unique = true)
    var externalId: String,

    @ManyToOne
    @JoinColumn(name = "legal_entity_id")
    var legalEntity: LegalEntity?,

    @ManyToOne
    @JoinColumn(name = "site_id")
    var site: Site?,

    @Column(name = "name_parts", nullable = true)
    var nameParts: Collection<String> = emptyList(),

    @Column(name = "data_type")
    @Enumerated(EnumType.STRING)
    var dataType: OutputInputEnum,

    @Embedded
    var physicalPostalAddress: PhysicalPostalAddress,

    @Embedded
    var alternativePostalAddress: AlternativePostalAddress?

) : BaseEntity() {

    @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], orphanRemoval = true)
    val states: MutableSet<AddressState> = mutableSetOf()
}