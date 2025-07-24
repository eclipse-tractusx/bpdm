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

package org.eclipse.tractusx.bpdm.pool.entity

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.model.BaseEntity
import org.eclipse.tractusx.bpdm.pool.api.model.RelationType

@Entity
@Table(
    name = "relations",
    indexes = [
        Index(columnList = "start_node_id"),
        Index(columnList = "end_node_id")
    ]
)
class RelationDb(
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: RelationType,

    @ManyToOne
    @JoinColumn(name = "start_node_id", nullable = false)
    val startNode: LegalEntityDb,

    @ManyToOne
    @JoinColumn(name = "end_node_id", nullable = false)
    val endNode: LegalEntityDb,

    @ElementCollection(fetch = FetchType.LAZY)
    @OrderColumn(name = "index", nullable = false)
    @CollectionTable(
        name = "relation_states",
        joinColumns = [JoinColumn(name = "relation_id", foreignKey = ForeignKey(name = "fk_relation_states_relation"))],
        indexes = [Index(name = "idx_relation_states_relation_id", columnList = "relation_id")]
    )
    val states: MutableList<RelationStateDb> = mutableListOf()

) : BaseEntity()
