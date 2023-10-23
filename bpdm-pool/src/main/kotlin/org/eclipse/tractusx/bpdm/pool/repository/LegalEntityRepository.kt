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

import org.eclipse.tractusx.bpdm.pool.entity.IdentifierType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface LegalEntityRepository : PagingAndSortingRepository<LegalEntity, Long>, CrudRepository<LegalEntity, Long> {
    fun findByBpn(bpn: String): LegalEntity?

    fun existsByBpn(bpn: String): Boolean

    fun findDistinctByBpnIn(bpns: Collection<String>): Set<LegalEntity>

    fun findByUpdatedAtAfter(updatedAt: Instant, pageable: Pageable): Page<LegalEntity>

    @Query("SELECT p FROM LegalEntity p WHERE LOWER(p.legalName.value) LIKE :value ORDER BY LENGTH(p.legalName.value)")
    fun findByLegalNameValue(value: String, pageable: Pageable): Page<LegalEntity>

    @Query("SELECT DISTINCT i.legalEntity FROM LegalEntityIdentifier i WHERE i.type = :type AND upper(i.value) = upper(:idValue)")
    fun findByIdentifierTypeAndValueIgnoreCase(type: IdentifierType, idValue: String): LegalEntity?

    @Query("SELECT DISTINCT p FROM LegalEntity p LEFT JOIN FETCH p.legalForm WHERE p IN :partners")
    fun joinLegalForm(partners: Set<LegalEntity>): Set<LegalEntity>

    @Query("SELECT DISTINCT p FROM LegalEntity p LEFT JOIN FETCH p.identifiers WHERE p IN :partners")
    fun joinIdentifiers(partners: Set<LegalEntity>): Set<LegalEntity>

    @Query("SELECT DISTINCT p FROM LegalEntity p LEFT JOIN FETCH p.states WHERE p IN :partners")
    fun joinStates(partners: Set<LegalEntity>): Set<LegalEntity>

    @Query("SELECT DISTINCT p FROM LegalEntity p LEFT JOIN FETCH p.classifications WHERE p IN :partners")
    fun joinClassifications(partners: Set<LegalEntity>): Set<LegalEntity>

    @Query("SELECT DISTINCT p FROM LegalEntity p LEFT JOIN FETCH p.startNodeRelations LEFT JOIN FETCH p.endNodeRelations WHERE p IN :partners")
    fun joinRelations(partners: Set<LegalEntity>): Set<LegalEntity>

    @Query("SELECT DISTINCT p FROM LegalEntity p LEFT JOIN FETCH p.legalAddress WHERE p IN :partners")
    fun joinLegalAddresses(partners: Set<LegalEntity>): Set<LegalEntity>
}