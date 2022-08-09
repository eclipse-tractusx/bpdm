/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.common.model.RelationClass
import org.eclipse.tractusx.bpdm.common.model.RelationType
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(
    name = "relations",
    indexes = [
        Index(columnList = "start_node_id"),
        Index(columnList = "end_node_id")
    ]
)
class Relation (
    @Column(name = "class", nullable = false)
    @Enumerated(EnumType.STRING)
    val relationClass: RelationClass,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: RelationType,
    @ManyToOne
    @JoinColumn(name = "start_node_id", nullable = false)
    val startNode: BusinessPartner,
    @ManyToOne
    @JoinColumn(name = "end_node_id", nullable = false)
    val endNode: BusinessPartner,
    @Column(name = "started_at")
    val startedAt: LocalDateTime?,
    @Column(name = "ended_at")
    val endedAt: LocalDateTime?
) : BaseEntity()

