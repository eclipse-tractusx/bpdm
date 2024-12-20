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
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType


@Entity
@Table(name = "business_partner_relations",
    uniqueConstraints = [
        UniqueConstraint(name = "uc_business_partner_relations_external_id_stage_tenant", columnNames = ["external_id, stage, tenant_bpnl"]),
        UniqueConstraint(name = "uc_business_partner_relations_source_target", columnNames = ["source_sharing_state_id", "target_sharing_state_id"])
    ]
)
class RelationDb(
    @Column(name = "external_id", unique = true, nullable = false)
    var externalId: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false)
    var relationType: RelationType,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_sharing_state_id", nullable = false)
    var source: SharingStateDb,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_sharing_state_id", nullable = false)
    var target: SharingStateDb,
    @Column(name = "tenant_bpnl", nullable = false)
    var tenantBpnL: String,
    @Column(name = "stage", nullable = false)
    @Enumerated(EnumType.STRING)
    var stage: StageType,
) : BaseEntity()