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

package org.eclipse.tractusx.bpdm.gate.entity

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.model.BaseEntity
import org.eclipse.tractusx.bpdm.common.model.StageType

@Entity
@Table(
    name = "legal_entities",
    indexes = [Index(columnList = "legal_form_id")]
)
class LegalEntityDb(
    @Column(name = "bpn")
    var bpn: String? = null,

    @Column(name = "externalId", nullable = false, unique = true)
    var externalId: String,

    @Column(name = "name_shortname")
    var shortName: String?,

    @Column(name = "legal_form_id")
    var legalForm: String? = null,

    @Column(name = "data_type")
    @Enumerated(EnumType.STRING)
    var stage: StageType

) : BaseEntity() {

    @OneToMany(mappedBy = "legalEntity", cascade = [CascadeType.ALL], orphanRemoval = true)
    val states: MutableSet<LegalEntityStateDb> = mutableSetOf()

    @OneToMany(mappedBy = "legalEntity", cascade = [CascadeType.ALL], orphanRemoval = true)
    val classifications: MutableSet<LegalEntityClassificationDb> = mutableSetOf()

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "legal_address_id", nullable = false)
    lateinit var legalAddress: LogisticAddressDb

    @OneToMany(mappedBy = "legalEntity", cascade = [CascadeType.ALL], orphanRemoval = true)
    val roles: MutableSet<RolesDb> = mutableSetOf()

    @OneToMany(mappedBy = "legalEntity", cascade = [CascadeType.ALL], orphanRemoval = true)
    val addresses: MutableSet<LogisticAddressDb> = mutableSetOf()

    @OneToMany(mappedBy = "legalEntity", cascade = [CascadeType.ALL], orphanRemoval = true)
    val nameParts: MutableSet<NamePartsDb> = mutableSetOf()

    @OneToMany(mappedBy = "legalEntity", cascade = [CascadeType.ALL], orphanRemoval = true)
    val identifiers: MutableSet<LegalEntityIdentifierDb> = mutableSetOf()
}
