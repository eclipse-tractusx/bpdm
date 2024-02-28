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

package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.api.model.response.BpnIdentifierMappingDto
import org.eclipse.tractusx.bpdm.pool.entity.AddressIdentifierDb
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface AddressIdentifierRepository : CrudRepository<AddressIdentifierDb, Long> {
    fun findByValueIn(identifierValues: Collection<String>): Set<AddressIdentifierDb>

    @Query("SELECT DISTINCT i FROM AddressIdentifierDb i LEFT JOIN FETCH i.type WHERE i IN :identifiers")
    fun joinType(identifiers: Set<AddressIdentifierDb>): Set<AddressIdentifierDb>

    @Query("SELECT new org.eclipse.tractusx.bpdm.pool.api.model.response.BpnIdentifierMappingDto(i.value,i.address.bpn) FROM AddressIdentifierDb i WHERE i.type = :identifierType AND i.value in :values")
    fun findBpnsByIdentifierTypeAndValues(identifierType: IdentifierTypeDb, values: Collection<String>): Set<BpnIdentifierMappingDto>

}
