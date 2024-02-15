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

package org.eclipse.tractusx.bpdm.gate.repository.generic

import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartner
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository


@Repository
interface BusinessPartnerRepository : JpaRepository<BusinessPartner, Long>, CrudRepository<BusinessPartner, Long> {

    fun findByStageAndExternalIdIn(stage: StageType, externalId: Collection<String>): Set<BusinessPartner>

    fun findByStageAndExternalIdIn(stage: StageType, externalId: Collection<String>, pageable: Pageable): Page<BusinessPartner>

    fun findByStage(stage: StageType, pageable: Pageable): Page<BusinessPartner>

    @Query("SELECT e FROM BusinessPartner e WHERE e.stage = :stage AND (e.bpnL IN :bpnL OR e.bpnS IN :bpnS OR e.bpnA IN :bpnA)")
    fun findByStageAndBpnLInOrBpnSInOrBpnAIn(
        @Param("stage") stage: StageType?,
        @Param("bpnL") bpnLList: List<String?>?,
        @Param("bpnS") bpnSList: List<String?>?,
        @Param("bpnA") bpnAList: List<String?>?
    ): Set<BusinessPartner>

    @Query("SELECT b.stage as stage, COUNT(b.stage) as count FROM BusinessPartner AS b GROUP BY b.stage")
    fun countPerStages(): List<PartnersPerStageCount>

    interface PartnersPerStageCount {
        val stage: StageType
        val count: Int
    }
}
