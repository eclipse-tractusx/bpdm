/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

import io.swagger.v3.oas.annotations.Parameter
import org.eclipse.tractusx.bpdm.gate.api.model.response.LsaType
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntity
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import java.time.Instant


interface ChangelogRepository : JpaRepository<ChangelogEntity, Long>, JpaSpecificationExecutor<ChangelogEntity> {

    object Specs {

        /**
         * Restrict to entries with any one of the given ExternalIds; ignore if null
         */
        fun byExternalIdsIn(externalIds: Collection<String>) =
            Specification<ChangelogEntity> { root, _, _ ->

                externalIds.let {
                    root.get<String>(ChangelogEntity::externalId.name).`in`(externalIds.map { externalId -> externalId })
                }

            }

        /**
         * Restrict to entries created at or after the given instant; ignore if null
         */
        fun byCreatedAtGreaterThan(createdAt: Instant?) =
            Specification<ChangelogEntity> { root, _, builder ->
                createdAt?.let {
                    builder.greaterThanOrEqualTo(root.get(ChangelogEntity::createdAt.name), createdAt)
                }
            }

        /**
         * Restrict to entries for the LsaType; ignore if null
         */
        fun byLsaType(lsaType: LsaType?) =
            Specification<ChangelogEntity> { root, _, builder ->
                lsaType?.let {
                    builder.equal(root.get<LsaType>(ChangelogEntity::businessPartnerType.name), lsaType)
                }
            }
    }

    @Query("select distinct u.externalId from ChangelogEntity u where u.externalId in :externalIdList")
    fun findExternalIdsInListDistinct(externalIdList: Collection<String>): Set<String>


}
