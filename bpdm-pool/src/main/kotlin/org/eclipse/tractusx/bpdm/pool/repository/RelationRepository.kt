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

package org.eclipse.tractusx.bpdm.pool.repository

import jakarta.persistence.criteria.Predicate
import org.eclipse.tractusx.bpdm.pool.api.model.RelationType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query

interface RelationRepository : JpaRepository<RelationDb, Long>, JpaSpecificationExecutor<RelationDb> {

    companion object {
        fun byRelation(startNode: LegalEntityDb?, endNode: LegalEntityDb?, type: RelationType?) =
            Specification<RelationDb> { root, _, builder ->
                val predicates = mutableListOf<Predicate>()

                startNode?.let {
                    predicates.add(builder.equal(root.get<LegalEntityDb>(RelationDb::startNode.name), it))
                }

                endNode?.let {
                    predicates.add(builder.equal(root.get<LegalEntityDb>(RelationDb::endNode.name), it))
                }

                type?.let {
                    predicates.add(builder.equal(root.get<RelationType>(RelationDb::type.name), it))
                }

                builder.and(*predicates.toTypedArray())
            }
    }

    @Query("SELECT r FROM RelationDb r WHERE r.type = :relationType AND (r.startNode = :legalEntity OR r.endNode = :legalEntity)")
    fun findInSourceOrTarget(relationType: RelationType, legalEntity: LegalEntityDb): Set<RelationDb>

    fun findByTypeAndStartNode(relationType: RelationType, legalEntity: LegalEntityDb): Set<RelationDb>
}