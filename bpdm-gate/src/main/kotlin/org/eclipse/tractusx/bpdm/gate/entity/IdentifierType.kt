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
import org.eclipse.tractusx.bpdm.common.dto.IdentifierLsaType
import org.eclipse.tractusx.bpdm.common.model.BaseEntity

@Entity
@Table(
    name = "identifier_types",
    uniqueConstraints = [UniqueConstraint(
        name = "uc_identifier_types_technical_key_lsa_type",
        columnNames = ["technical_key", "lsa_type"]
    )]
)
class IdentifierType(
    @Column(name = "technical_key", nullable = false)
    val technicalKey: String,

    @Column(name = "lsa_type", nullable = false)
    @Enumerated(EnumType.STRING)
    val lsaType: IdentifierLsaType,

    @Column(name = "name", nullable = false)
    val name: String,

    ) : BaseEntity() {
    @OneToMany(mappedBy = "identifierType", cascade = [CascadeType.ALL], orphanRemoval = true)
    val details: MutableSet<IdentifierTypeDetail> = mutableSetOf()
}
