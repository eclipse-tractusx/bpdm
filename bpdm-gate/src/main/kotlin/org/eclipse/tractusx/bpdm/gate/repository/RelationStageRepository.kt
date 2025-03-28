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

package org.eclipse.tractusx.bpdm.gate.repository

import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.entity.RelationStageDb
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.time.Instant

interface RelationStageRepository: JpaRepository<RelationStageDb, Long>, JpaSpecificationExecutor<RelationStageDb> {
    object Specs {
        fun byExternalIds(externalIds: Collection<String>?) =
            Specification<RelationStageDb> { root, _, _ ->
                externalIds?.takeIf { it.isNotEmpty() }?.let {
                    root
                        .get<RelationDb>(RelationStageDb::relation.name)
                        .get<String>(RelationDb::externalId.name)
                        .`in`(externalIds)
                }
            }

        fun byRelationshipType(relationType: RelationType?) =
            Specification<RelationStageDb> { root, _, builder ->
                relationType?.let {
                    builder.equal(root.get<RelationType>(RelationStageDb::relationType.name), relationType)
                }
            }

        fun bySourceExternalIds(sourceBusinessPartnerExternalIds: List<String>?) =
            Specification<RelationStageDb> { root, _, _ ->
                sourceBusinessPartnerExternalIds?.takeIf { it.isNotEmpty() }?.let {
                    root
                        .get<SharingStateDb>(RelationStageDb::source.name)
                        .get<String>(SharingStateDb::externalId.name)
                        .`in`(sourceBusinessPartnerExternalIds)
                }
            }

        fun byTargetExternalIds(targetBusinessPartnerExternalIds: List<String>?) =
            Specification<RelationStageDb> { root, _, _ ->
                targetBusinessPartnerExternalIds?.takeIf { it.isNotEmpty() }?.let {
                    root
                        .get<SharingStateDb>(RelationStageDb::target.name)
                        .get<String>(SharingStateDb::externalId.name)
                        .`in`(targetBusinessPartnerExternalIds)
                }
            }

        fun byUpdatedAfter(updatedAfter: Instant?) =
            Specification<RelationStageDb> { root, _, builder ->
                updatedAfter?.let {
                    builder.greaterThan(root.get(RelationStageDb::updatedAt.name), updatedAfter)
                }
            }

        fun byStage(stageType: StageType?) =
            Specification<RelationStageDb> { root, _, builder ->
                stageType?.let {
                    builder.equal(root.get<StageType>(RelationStageDb::stage.name), stageType)
                }
            }

        fun byTenantBpnL(tenantBpnL: String?) =
            Specification<RelationStageDb> { root, _, builder ->
                tenantBpnL?.let {
                    builder.equal(root
                        .get<RelationDb>(RelationStageDb::relation.name)
                        .get<String>(RelationDb::tenantBpnL.name), tenantBpnL)
                }
            }
    }

    fun findByRelationAndStage(relationDb: RelationDb, stage: StageType): RelationStageDb?
}