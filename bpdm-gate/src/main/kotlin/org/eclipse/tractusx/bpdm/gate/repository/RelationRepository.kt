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

import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.entity.RelationSharingStateDb
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface RelationRepository: JpaRepository<RelationDb, Long>, JpaSpecificationExecutor<RelationDb> {

    object Specs {

        fun byTenantBpnL(tenantBpnL: String?) =
            Specification<RelationDb> { root, _, builder ->
                tenantBpnL?.let {
                    builder.equal(root.get<String>(RelationDb::tenantBpnL.name), tenantBpnL)
                }
            }

        fun byExternalIds(externalIds: Collection<String>?) =
            Specification<RelationDb> { root, _, _ ->
                externalIds?.takeIf { it.isNotEmpty() }?.let {
                    root
                        .get<String>(RelationDb::externalId.name)
                        .`in`(externalIds)
                }
            }

        fun bySharingStateUpdatedAfter(updatedAfter: Instant?) =
            Specification<RelationDb> { root, _, builder ->
                updatedAfter?.let {
                    builder.greaterThan(root
                        .get<RelationSharingStateDb>(RelationDb::sharingState.name)
                        .get<Instant>(RelationSharingStateDb::updatedAt.name), updatedAfter)
                }
            }

        fun bySharingStateTypes(sharingStateTypes: Collection<RelationSharingStateType>?) =
            Specification<RelationDb> { root, _, builder ->
                sharingStateTypes?.takeIf{ it.isNotEmpty() }?.let {
                    root
                        .get<RelationSharingStateDb>(RelationDb::sharingState.name)
                        .get<SharingStateType>(RelationSharingStateDb::sharingStateType.name)
                        .`in`(sharingStateTypes)
                }
            }

        fun bySharingStateNotNull() =
            Specification<RelationDb> { root, _, builder ->
                    root
                        .get<RelationSharingStateDb>(RelationDb::sharingState.name)
                        .get<SharingStateType>(RelationSharingStateDb::sharingStateType.name)
                        .isNotNull
            }

    }


    fun findByTenantBpnLAndExternalId(tenantBpnL: String,  externalId: String): RelationDb?

    @Query("SELECT r FROM RelationDb r WHERE r.sharingState.sharingStateType = :sharingStateType AND r.sharingState.isStaged = :isStaged")
    fun findBySharingStateAndStaged(sharingStateType: RelationSharingStateType, isStaged: Boolean, pageable: Pageable): Page<RelationDb>
}