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
import org.eclipse.tractusx.bpdm.pool.api.model.AddressRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType

@Entity
@Table(
    name = "address_relations",
    indexes = [
        Index(columnList = "start_address_id"),
        Index(columnList = "end_address_id")
    ]
)
class AddressRelationDb(
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: AddressRelationType = AddressRelationType.IsReplacedBy, //Only allowed type for address relations

    @ManyToOne
    @JoinColumn(name = "start_address_id", nullable = false)
    val startAddress: LogisticAddressDb,

    @ManyToOne
    @JoinColumn(name = "end_address_id", nullable = false)
    val endAddress: LogisticAddressDb,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "address_relation_validity_periods",
        joinColumns = [JoinColumn(name = "relation_id", foreignKey = ForeignKey(name = "fk_address_relation_validity_periods_relation"))],
        indexes = [Index(name = "idx_address_relation_validity_periods_relation_id", columnList = "relation_id")]
    )
    var validityPeriods: MutableList<RelationValidityPeriodDb> = mutableListOf()

): BaseEntity()