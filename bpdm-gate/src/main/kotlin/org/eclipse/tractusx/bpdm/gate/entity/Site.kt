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
@Table(name = "sites")
class Site(
    @Column(name = "bpn")
    var bpn: String? = null,

    @Column(name = "external_id", nullable = false, unique = true)
    var externalId: String,

    @Column(name = "data_type")
    @Enumerated(EnumType.STRING)
    var dataType: OutputInputEnum,

    @ManyToOne
    @JoinColumn(name = "legal_entity_id", nullable = false)
    var legalEntity: LegalEntity,

    ) : BaseEntity() {

    @OneToMany(mappedBy = "site", cascade = [CascadeType.ALL], orphanRemoval = true)
    val states: MutableSet<SiteState> = mutableSetOf()

    @OneToMany(mappedBy = "site", cascade = [CascadeType.ALL], orphanRemoval = true)
    val addresses: MutableSet<LogisticAddress> = mutableSetOf()

    @OneToMany(mappedBy = "site", cascade = [CascadeType.ALL], orphanRemoval = true)
    val roles: MutableSet<Roles> = mutableSetOf()

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "main_address_id", nullable = false)
    lateinit var mainAddress: LogisticAddress

    @OneToMany(mappedBy = "site", cascade = [CascadeType.ALL], orphanRemoval = true)
    val nameParts: MutableSet<NameParts> = mutableSetOf()
}