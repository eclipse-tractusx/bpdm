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

package org.eclipse.tractusx.bpdm.gate.entity

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.model.BaseEntity
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.entity.RelationStageDb.Companion.COLUMN_RELATION
import org.eclipse.tractusx.bpdm.gate.entity.RelationStageDb.Companion.COLUMN_STAGE

@Entity
@Table(name = "business_partner_relation_stages",
    uniqueConstraints = [
        UniqueConstraint(name = "uc_relation_stage", columnNames = [COLUMN_RELATION, COLUMN_STAGE])
    ]
)
class RelationStageDb (
    @ManyToOne
    @JoinColumn(name = COLUMN_RELATION, nullable = false)
    var relation: RelationDb,
    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false)
    var relationType: RelationType,
    @Column(name = COLUMN_STAGE, nullable = false)
    @Enumerated(EnumType.STRING)
    var stage: StageType,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_sharing_state_id", nullable = false)
    var source: SharingStateDb,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_sharing_state_id", nullable = false)
    var target: SharingStateDb,
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "relation_stage_validity_periods",
        joinColumns = [JoinColumn(name = "relation_stage_id", foreignKey = ForeignKey(name = "fk_stage_validity_periods_relation"))],
        indexes = [Index(name = "idx_stage_validity_periods_relation_id", columnList = "relation_stage_id")]
    )
    var validityPeriods: MutableList<RelationValidityPeriodDb>,
    @Column(name = "reason_code", nullable = false)
    var reasonCode: String
): BaseEntity(){
    companion object{
        const val COLUMN_RELATION = "relation_id"
        const val COLUMN_STAGE = "stage"
    }
}