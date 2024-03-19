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

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntryDb
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import java.time.Instant


interface ChangelogRepository : JpaRepository<ChangelogEntryDb, Long>, JpaSpecificationExecutor<ChangelogEntryDb> {

    object Specs {

        /**
         * Restrict to entries with any one of the given ExternalIds; ignore if empty
         */
        fun byExternalIdsIn(externalIds: Collection<String>?) =
            Specification<ChangelogEntryDb> { root, _, _ ->
                externalIds?.let {
                    if (externalIds.isNotEmpty())
                        root.get<String>(ChangelogEntryDb::externalId.name).`in`(externalIds)
                    else
                        null
                }
            }

        /**
         * Restrict to entries created at or after the given instant; ignore if null
         */
        fun byCreatedAtGreaterThan(createdAt: Instant?) =
            Specification<ChangelogEntryDb> { root, _, builder ->
                createdAt?.let {
                    builder.greaterThanOrEqualTo(root.get(ChangelogEntryDb::createdAt.name), createdAt)
                }
            }

        /**
         * Restrict to entries for the BusinessPartnerType; ignore if empty
         */
        fun byBusinessPartnerTypes(businessPartnerTypes: Set<BusinessPartnerType>?) =
            Specification<ChangelogEntryDb> { root, _, builder ->
                businessPartnerTypes?.let {
                    if (businessPartnerTypes.isNotEmpty())
                        root.get<String>(ChangelogEntryDb::businessPartnerType.name).`in`(businessPartnerTypes)
                    else
                        null
                }
            }

        fun byStage(stage: StageType?) =
            Specification<ChangelogEntryDb> { root, _, builder ->
                stage?.let {
                    builder.equal(root.get<StageType>(ChangelogEntryDb::stage.name), stage)
                }
            }

        fun byAssociatedOwnerBpnl(associatedOwnerBpnl: String?) =
            Specification<ChangelogEntryDb> { root, _, builder ->
                associatedOwnerBpnl?.takeIf { it.isNotBlank() }?.let {
                    builder.like(root.get(ChangelogEntryDb::associatedOwnerBpnl.name), associatedOwnerBpnl)
                }
            }

    }

    @Query("select distinct u.externalId from ChangelogEntryDb u where u.externalId in :externalIdList")
    fun findExternalIdsInListDistinct(externalIdList: Collection<String>): Set<String>


}
