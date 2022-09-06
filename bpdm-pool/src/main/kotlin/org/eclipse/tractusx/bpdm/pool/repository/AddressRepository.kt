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

import org.eclipse.tractusx.bpdm.pool.entity.Address
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository

interface AddressRepository : PagingAndSortingRepository<Address, Long> {

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.contexts WHERE a IN :addresses")
    fun joinContexts(addresses: Set<Address>): Set<Address>

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.types WHERE a IN :addresses")
    fun joinTypes(addresses: Set<Address>): Set<Address>

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.version WHERE a IN :addresses")
    fun joinVersions(addresses: Set<Address>): Set<Address>

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.administrativeAreas WHERE a IN :addresses")
    fun joinAdminAreas(addresses: Set<Address>): Set<Address>

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.postCodes WHERE a IN :addresses")
    fun joinPostCodes(addresses: Set<Address>): Set<Address>

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.thoroughfares WHERE a IN :addresses")
    fun joinThoroughfares(addresses: Set<Address>): Set<Address>

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.premises WHERE a IN :addresses")
    fun joinPremises(addresses: Set<Address>): Set<Address>

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.postalDeliveryPoints WHERE a IN :addresses")
    fun joinPostalDeliveryPoints(addresses: Set<Address>): Set<Address>

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.localities WHERE a IN :addresses")
    fun joinLocalities(addresses: Set<Address>): Set<Address>

}