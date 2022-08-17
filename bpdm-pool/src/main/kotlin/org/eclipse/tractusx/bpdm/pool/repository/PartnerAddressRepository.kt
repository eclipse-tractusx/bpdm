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

import org.eclipse.tractusx.bpdm.pool.entity.BusinessPartner
import org.eclipse.tractusx.bpdm.pool.entity.PartnerAddress
import org.eclipse.tractusx.bpdm.pool.entity.Site
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository

interface PartnerAddressRepository : PagingAndSortingRepository<PartnerAddress, Long> {

    @Query("SELECT a FROM PartnerAddress a join a.partner p where p.bpn=:bpn")
    fun findByPartnerBpn(bpn: String, pageable: Pageable): Page<PartnerAddress>

    fun findByPartnerInOrSiteIn(partners: Collection<BusinessPartner>, sites: Collection<Site>, pageable: Pageable): Page<PartnerAddress>

    fun findByBpn(bpn: String): PartnerAddress?

    fun findDistinctByBpnIn(bpns: Collection<String>): Set<PartnerAddress>

    @Query("SELECT DISTINCT a FROM PartnerAddress a LEFT JOIN FETCH a.partner WHERE a IN :addresses")
    fun joinLegalEntities(addresses: Set<PartnerAddress>): Set<PartnerAddress>

    @Query("SELECT DISTINCT a FROM PartnerAddress a LEFT JOIN FETCH a.site WHERE a IN :addresses")
    fun joinSites(addresses: Set<PartnerAddress>): Set<PartnerAddress>

    @Query("SELECT DISTINCT a FROM PartnerAddress a LEFT JOIN FETCH a.address WHERE a IN :addresses")
    fun joinAddresses(addresses: Set<PartnerAddress>): Set<PartnerAddress>

}