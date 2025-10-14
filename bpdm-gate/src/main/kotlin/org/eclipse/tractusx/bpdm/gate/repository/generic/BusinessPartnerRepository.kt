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

package org.eclipse.tractusx.bpdm.gate.repository.generic

import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository


@Repository
interface BusinessPartnerRepository : PagingAndSortingRepository<BusinessPartnerDb, Long>, CrudRepository<BusinessPartnerDb, Long>,
    JpaSpecificationExecutor<BusinessPartnerDb> {

    object Specs {
        /**
         * Restrict to entries with any one of the given externalIds; ignore if null
         */
        fun byExternalIdsIn(externalIds: Collection<String>?) =
            Specification<BusinessPartnerDb> { root, _, _ ->
                externalIds?.takeIf { it.isNotEmpty() }?.let {
                    root
                        .get<SharingStateDb>(BusinessPartnerDb::sharingState.name)
                        .get<String>(SharingStateDb::externalId.name).`in`(externalIds)
                }
            }


        fun byTenantBpnl(tenantBpnl: String?) =
            Specification<BusinessPartnerDb> { root, _, builder ->
                val path = root.get<SharingStateDb>(BusinessPartnerDb::sharingState.name).get<String?>(SharingStateDb::tenantBpnl.name)
                tenantBpnl?.let {
                    builder.equal(path, tenantBpnl)
                } ?: builder.isNull(path)

            }

        fun byStage(stage: StageType) =
            Specification<BusinessPartnerDb> { root, _, builder ->
                val path = root.get<StageType>(BusinessPartnerDb::stage.name)
                builder.equal(path, stage)
            }

    }

    fun findBySharingStateInAndStage(sharingStates: Collection<SharingStateDb>, stage: StageType): Set<BusinessPartnerDb>
    fun findByStageAndSharingStateSharingStateType(stage: StageType, sharingStateType: SharingStateType, pageable: Pageable): Page<BusinessPartnerDb>

    @Query("SELECT b.stage as stage, COUNT(b.stage) as count FROM BusinessPartnerDb AS b GROUP BY b.stage")
    fun countPerStages(): List<PartnersPerStageCount>

    fun findByStageAndBpnLIn(stage: StageType, bpnL: Collection<String>): Set<BusinessPartnerDb>
    fun findByStageAndBpnSIn(stage: StageType, bpnS: Collection<String>): Set<BusinessPartnerDb>
    fun findByStageAndBpnAIn(stage: StageType, bpnA: Collection<String>): Set<BusinessPartnerDb>

    interface PartnersPerStageCount {
        val stage: StageType
        val count: Int
    }
}
