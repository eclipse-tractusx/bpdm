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

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query

interface SiteRepository : JpaRepository<SiteDb, Long>, JpaSpecificationExecutor<SiteDb> {

    companion object {
        fun byBpns(bpns: Collection<String>?) =
            Specification<SiteDb> { root, _, _ ->
                bpns?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }?.let {
                    root.get<String>(SiteDb::bpn.name).`in`(bpns)
                }
            }

        fun byParentBpns(bpns: Collection<String>?) =
            Specification<SiteDb> { root, _, _ ->
                bpns?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }?.let {
                    root.get<LegalEntityDb>(SiteDb::legalEntity.name).get<String>(LegalEntityDb::bpn.name).`in`(bpns)
                }
            }

        fun byName(name: String?) =
            Specification<SiteDb> { root, _, builder ->
                name?.takeIf { it.isNotBlank() }?.let {
                    builder.like(builder.lower(root.get(SiteDb::name.name)), "%${name.lowercase()}%")
                }
            }

        fun byIsMember(isCatenaXMemberData: Boolean?) =
            Specification<SiteDb> { root, _, builder ->
                isCatenaXMemberData?.let {
                    builder.equal(root.get<LegalEntityDb>(SiteDb::legalEntity.name).get<Boolean>(LegalEntityDb::isCatenaXMemberData.name), isCatenaXMemberData)
                }
            }
    }

    @Query("SELECT DISTINCT s FROM SiteDb s LEFT JOIN FETCH s.addresses WHERE s IN :sites")
    fun joinAddresses(sites: Set<SiteDb>): Set<SiteDb>

    @Query("SELECT DISTINCT p FROM SiteDb p LEFT JOIN FETCH p.states WHERE p IN :partners")
    fun joinStates(partners: Set<SiteDb>): Set<SiteDb>

    fun findByLegalEntity(legalEntity: LegalEntityDb, pageable: Pageable): Page<SiteDb>

    fun findByBpn(bpn: String): SiteDb?

    fun findDistinctByBpnIn(bpns: Collection<String>): Set<SiteDb>
}