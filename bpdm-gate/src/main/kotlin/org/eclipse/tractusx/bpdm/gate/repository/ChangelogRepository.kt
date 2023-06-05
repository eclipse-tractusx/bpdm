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

import org.eclipse.tractusx.bpdm.gate.api.model.LsaType
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntry
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import java.time.Instant


interface ChangelogRepository : JpaRepository<ChangelogEntry, Long>, JpaSpecificationExecutor<ChangelogEntry> {

    object Specs {

        /**
         * Restrict to entries with any one of the given ExternalIds; ignore if null
         */
        fun byExternalIdsIn(externalIds: Collection<String>) =
            Specification<ChangelogEntry> { root, _, _ ->

                externalIds.let {
                    root.get<String>(ChangelogEntry::externalId.name).`in`(externalIds.map { externalId -> externalId })
                }

            }

        /**
         * Restrict to entries created at or after the given instant; ignore if null
         */
        fun byCreatedAtGreaterThan(createdAt: Instant?) =
            Specification<ChangelogEntry> { root, _, builder ->
                createdAt?.let {
                    builder.greaterThanOrEqualTo(root.get(ChangelogEntry::createdAt.name), createdAt)
                }
            }

        /**
         * Restrict to entries for the LsaType; ignore if null
         */
        fun byLsaType(lsaType: LsaType?) =
            Specification<ChangelogEntry> { root, _, builder ->
                lsaType?.let {
                    builder.equal(root.get<LsaType>(ChangelogEntry::businessPartnerType.name), lsaType)
                }
            }
    }

    @Query("select distinct u.externalId from ChangelogEntry u where u.externalId in :externalIdList")
    fun findExternalIdsInListDistinct(externalIdList: Collection<String>): Set<String>


}
