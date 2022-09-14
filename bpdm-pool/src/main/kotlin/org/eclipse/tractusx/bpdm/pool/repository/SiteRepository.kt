/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntity
import org.eclipse.tractusx.bpdm.pool.entity.Site
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository

interface SiteRepository : PagingAndSortingRepository<Site, Long> {
    @Query("SELECT DISTINCT s FROM Site s LEFT JOIN FETCH s.addresses WHERE s IN :sites")
    fun joinAddresses(sites: Set<Site>): Set<Site>

    @Query("SELECT s FROM Site s join s.legalEntity p where p.bpn=:bpn")
    fun findByLegalEntityBpn(bpn: String, pageable: Pageable): Page<Site>

    fun findByLegalEntityInOrBpnIn(partners: Collection<LegalEntity>, bpns: Collection<String>, pageable: Pageable): Page<Site>

    fun findByBpn(bpn: String): Site?

    fun findDistinctByBpnIn(bpns: Collection<String>): Set<Site>
}