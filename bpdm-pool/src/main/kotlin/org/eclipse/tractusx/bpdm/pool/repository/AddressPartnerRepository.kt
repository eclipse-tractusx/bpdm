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

import org.eclipse.tractusx.bpdm.pool.entity.AddressPartner
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntity
import org.eclipse.tractusx.bpdm.pool.entity.Site
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface AddressPartnerRepository : PagingAndSortingRepository<AddressPartner, Long>, CrudRepository<AddressPartner, Long> {

    @Query("SELECT a FROM AddressPartner a join a.legalEntity p where p.bpn=:bpn")
    fun findByLegalEntityBpn(bpn: String, pageable: Pageable): Page<AddressPartner>

    fun findByLegalEntityInOrSiteInOrBpnIn(
        partners: Collection<LegalEntity>,
        sites: Collection<Site>,
        bpns: Collection<String>,
        pageable: Pageable
    ): Page<AddressPartner>

    fun findByBpn(bpn: String): AddressPartner?

    fun findDistinctByBpnIn(bpns: Collection<String>): Set<AddressPartner>

    @Query("SELECT DISTINCT a FROM AddressPartner a LEFT JOIN FETCH a.legalEntity WHERE a IN :addresses")
    fun joinLegalEntities(addresses: Set<AddressPartner>): Set<AddressPartner>

    @Query("SELECT DISTINCT a FROM AddressPartner a LEFT JOIN FETCH a.site WHERE a IN :addresses")
    fun joinSites(addresses: Set<AddressPartner>): Set<AddressPartner>

    @Query("SELECT DISTINCT a FROM AddressPartner a LEFT JOIN FETCH a.address WHERE a IN :addresses")
    fun joinAddresses(addresses: Set<AddressPartner>): Set<AddressPartner>

}