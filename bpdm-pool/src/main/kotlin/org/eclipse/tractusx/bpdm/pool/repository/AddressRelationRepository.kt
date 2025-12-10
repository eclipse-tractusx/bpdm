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

import jakarta.persistence.criteria.Predicate
import org.eclipse.tractusx.bpdm.pool.api.model.AddressRelationType
import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query

interface AddressRelationRepository : JpaRepository<AddressRelationDb, Long>, JpaSpecificationExecutor<AddressRelationDb> {

    companion object {
        fun byRelation(startAddress: LogisticAddressDb?, endAddress: LogisticAddressDb?, type: AddressRelationType?) =
            Specification<AddressRelationDb> { root, _, builder ->
                val predicates = mutableListOf<Predicate>()

                startAddress?.let {
                    predicates.add(builder.equal(root.get<LogisticAddressDb>(AddressRelationDb::startAddress.name), it))
                }

                endAddress?.let {
                    predicates.add(builder.equal(root.get<LogisticAddressDb>(AddressRelationDb::endAddress.name), it))
                }

                type?.let {
                    predicates.add(builder.equal(root.get<AddressRelationType>(AddressRelationDb::type.name), it))
                }

                builder.and(*predicates.toTypedArray())
            }
    }

    @Query("SELECT r FROM AddressRelationDb r WHERE r.type = :addressRelationType AND (r.startAddress = :address OR r.endAddress = :address)")
    fun findInSourceOrTarget(addressRelationType: AddressRelationType, address: LogisticAddressDb): Set<AddressRelationDb>

    fun findByTypeAndStartAddress(addressRelationType: AddressRelationType, address: LogisticAddressDb): Set<AddressRelationDb>
}