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
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.time.Instant

interface RelationRepository: JpaRepository<RelationDb, Long>, JpaSpecificationExecutor<RelationDb> {
    object Specs {
        fun byExternalIds(externalIds: Collection<String>?) =
            Specification<RelationDb> { root, _, _ ->
                externalIds?.takeIf { it.isNotEmpty() }?.let {
                    root.get<String>(RelationDb::externalId.name).`in`(externalIds)
                }
            }

        fun byRelationshipType(relationType: RelationType?) =
            Specification<RelationDb> { root, _, builder ->
                relationType?.let {
                    builder.equal(root.get<RelationType>(RelationDb::relationType.name), relationType)
                }
            }

        fun bySourceExternalIds(sourceBusinessPartnerExternalIds: List<String>?) =
            Specification<RelationDb> { root, _, _ ->
                sourceBusinessPartnerExternalIds?.takeIf { it.isNotEmpty() }?.let {
                    root
                        .get<SharingStateDb>(RelationDb::source.name)
                        .get<String>(SharingStateDb::externalId.name)
                        .`in`(sourceBusinessPartnerExternalIds)
                }
            }

        fun byTargetExternalIds(targetBusinessPartnerExternalIds: List<String>?) =
            Specification<RelationDb> { root, _, _ ->
                targetBusinessPartnerExternalIds?.takeIf { it.isNotEmpty() }?.let {
                    root
                        .get<SharingStateDb>(RelationDb::target.name)
                        .get<String>(SharingStateDb::externalId.name)
                        .`in`(targetBusinessPartnerExternalIds)
                }
            }

        fun byUpdatedAfter(updatedAfter: Instant?) =
            Specification<RelationDb> { root, _, builder ->
                updatedAfter?.let {
                    builder.greaterThan(root.get(RelationDb::updatedAt.name), updatedAfter)
                }
            }

        fun byStage(stageType: StageType?) =
            Specification<RelationDb> { root, _, builder ->
                stageType?.let {
                    builder.equal(root.get<StageType>(RelationDb::stage.name), stageType)
                }
            }

        fun byTenantBpnL(tenantBpnL: String?) =
            Specification<RelationDb> { root, _, builder ->
                tenantBpnL?.let {
                    builder.equal(root.get<String>(RelationDb::tenantBpnL.name), tenantBpnL)
                }
            }
    }

    fun findByTenantBpnLAndStageAndExternalId(tenantBpnL: String, stageType: StageType, externalId: String): RelationDb?
    fun findByRelationTypeAndSource(relationType: RelationType, source: SharingStateDb): Set<RelationDb>
    fun findByRelationTypeAndTarget(relationType: RelationType, target: SharingStateDb): Set<RelationDb>
    fun findBySourceInAndStage(sources: Set<SharingStateDb>, stageType: StageType): Set<RelationDb>
    fun findByTargetInAndStage(sources: Set<SharingStateDb>, stageType: StageType): Set<RelationDb>
}