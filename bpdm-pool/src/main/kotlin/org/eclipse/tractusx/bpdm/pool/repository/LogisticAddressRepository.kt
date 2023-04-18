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

package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntity
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddress
import org.eclipse.tractusx.bpdm.pool.entity.Site
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface LogisticAddressRepository : PagingAndSortingRepository<LogisticAddress, Long>, CrudRepository<LogisticAddress, Long> {
    fun findByBpn(bpn: String): LogisticAddress?

    fun findDistinctByBpnIn(bpns: Collection<String>): Set<LogisticAddress>

    @Query("SELECT a FROM LogisticAddress a join a.legalEntity p where p.bpn=:bpn")
    fun findByLegalEntityBpn(bpn: String, pageable: Pageable): Page<LogisticAddress>

    fun findByLegalEntityInOrSiteInOrBpnIn(
        legalEntities: Collection<LegalEntity>,
        sites: Collection<Site>,
        bpns: Collection<String>,
        pageable: Pageable
    ): Page<LogisticAddress>

    @Query("SELECT DISTINCT a FROM LogisticAddress a LEFT JOIN FETCH a.legalEntity LEFT JOIN FETCH a.legalEntity.legalAddress WHERE a IN :addresses")
    fun joinLegalEntities(addresses: Set<LogisticAddress>): Set<LogisticAddress>

    @Query("SELECT DISTINCT a FROM LogisticAddress a LEFT JOIN FETCH a.site LEFT JOIN FETCH a.site.mainAddress WHERE a IN :addresses")
    fun joinSites(addresses: Set<LogisticAddress>): Set<LogisticAddress>

    @Query("SELECT DISTINCT a FROM LogisticAddress a LEFT JOIN FETCH a.physicalPostalAddress.administrativeAreaLevel1 LEFT JOIN FETCH a.alternativePostalAddress.administrativeAreaLevel1 WHERE a IN :addresses")
    fun joinRegions(addresses: Set<LogisticAddress>): Set<LogisticAddress>

    @Query("SELECT DISTINCT p FROM LogisticAddress p LEFT JOIN FETCH p.identifiers WHERE p IN :partners")
    fun joinIdentifiers(partners: Set<LogisticAddress>): Set<LogisticAddress>

    @Query("SELECT DISTINCT p FROM LogisticAddress p LEFT JOIN FETCH p.states WHERE p IN :partners")
    fun joinStates(partners: Set<LogisticAddress>): Set<LogisticAddress>
}