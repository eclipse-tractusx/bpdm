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

import org.eclipse.tractusx.bpdm.pool.dto.response.BpnIdentifierMappingResponse
import org.eclipse.tractusx.bpdm.pool.entity.Identifier
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierType
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface IdentifierRepository : CrudRepository<Identifier, Long> {
    fun findByValueIn(identifierValues: Collection<String>): Set<Identifier>

    @Query("SELECT DISTINCT i FROM Identifier i LEFT JOIN FETCH i.type WHERE i IN :identifiers")
    fun joinType(identifiers: Set<Identifier>): Set<Identifier>

    @Query("SELECT DISTINCT i FROM Identifier i LEFT JOIN FETCH i.status WHERE i IN :identifiers")
    fun joinStatus(identifiers: Set<Identifier>): Set<Identifier>

    @Query("SELECT DISTINCT i FROM Identifier i LEFT JOIN FETCH i.issuingBody WHERE i IN :identifiers")
    fun joinIssuingBody(identifiers: Set<Identifier>): Set<Identifier>

    @Query("SELECT new org.eclipse.tractusx.bpdm.pool.dto.response.BpnIdentifierMappingResponse(i.value,i.legalEntity.bpn) FROM Identifier i WHERE i.type = :identifierType AND i.value in :values")
    fun findBpnsByIdentifierTypeAndValues(identifierType: IdentifierType, values: Collection<String>): Set<BpnIdentifierMappingResponse>


}
