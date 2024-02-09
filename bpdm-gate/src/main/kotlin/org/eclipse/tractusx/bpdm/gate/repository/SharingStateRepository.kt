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
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.entity.SharingState
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface SharingStateRepository : PagingAndSortingRepository<SharingState, Long>, CrudRepository<SharingState, Long>, JpaSpecificationExecutor<SharingState> {

    object Specs {
        /**
         * Restrict to entries with any one of the given externalIds; ignore if null
         */
        fun byExternalIdsIn(externalIds: Collection<String>?) =
            Specification<SharingState> { root, _, _ ->
                externalIds?.let {
                    root.get<String>(SharingState::externalId.name).`in`(externalIds)
                }
            }

        /**
         * Restrict to entries with the given businessPartnerType; ignore if null
         */
        fun byBusinessPartnerType(businessPartnerType: BusinessPartnerType?) =
            Specification<SharingState> { root, _, builder ->
                businessPartnerType?.let {
                    builder.equal(root.get<BusinessPartnerType>(SharingState::businessPartnerType.name), businessPartnerType)
                }
            }
    }

    fun findByExternalIdInAndBusinessPartnerType(externalId: Collection<String>, businessPartnerType: BusinessPartnerType): Collection<SharingState>

    fun findBySharingStateType(sharingStateType: SharingStateType): Set<SharingState>

    fun findBySharingStateType(sharingStateType: SharingStateType, pageable: Pageable): Page<SharingState>

    fun findBySharingStateTypeAndTaskIdNotNull(sharingStateType: SharingStateType, pageable: Pageable): Page<SharingState>

    @Query("SELECT s.sharingStateType as type, COUNT(s.sharingStateType) as count FROM SharingState AS s GROUP BY s.sharingStateType")
    fun countSharingStateTypes(): List<SharingStateTypeCount>

    interface SharingStateTypeCount {
        val type: SharingStateType
        val count: Int
    }
}
