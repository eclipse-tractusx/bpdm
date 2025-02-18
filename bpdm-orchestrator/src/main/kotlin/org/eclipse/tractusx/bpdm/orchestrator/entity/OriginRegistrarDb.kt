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

package org.eclipse.tractusx.bpdm.orchestrator.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import org.eclipse.tractusx.orchestrator.api.model.PriorityEnum
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity
@Table(
    name = "origin_register",
    indexes = [
        Index(name = "index_priority_indicator_origin_id", columnList = "origin_id")
    ]
)
class OriginRegistrarDb (
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bpdm_sequence")
    @SequenceGenerator(name = "bpdm_sequence", sequenceName = "bpdm_sequence", allocationSize = 1)
    @Column(name = "id", nullable = false, updatable = false, insertable = false)
    val id: Long = 0,

    @Column(name = "name")
    var name: String,

    @Column(updatable = false, unique = true, nullable = false, name = "origin_id")
    @NotNull
    var originId: String,

    @Column(nullable = false, name = "threshold")
    @NotNull
    var threshold: Long,

    @Column(nullable = false, name = "priority")
    @Enumerated(EnumType.ORDINAL)
    var priority: PriorityEnum = PriorityEnum.Low,

    @Column(updatable = false, nullable = false, name = "CREATED_AT")
    @CreationTimestamp
    var createdAt: Instant = Instant.now(),

    @Column(nullable = false, name = "UPDATED_AT")
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now(),
)