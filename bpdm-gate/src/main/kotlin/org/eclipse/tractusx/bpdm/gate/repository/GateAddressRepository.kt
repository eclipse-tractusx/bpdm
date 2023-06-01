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

package org.eclipse.tractusx.bpdm.gate.repository

import org.eclipse.tractusx.bpdm.common.model.OutputInputEnum
import org.eclipse.tractusx.bpdm.gate.entity.LogisticAddress
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface GateAddressRepository : PagingAndSortingRepository<LogisticAddress, Long>, CrudRepository<LogisticAddress, Long> {

    fun findByExternalIdIn(externalId: Collection<String>): Set<LogisticAddress>

    fun findByExternalId(externalId: String): LogisticAddress?

    fun findByExternalIdAndDataType(externalId: String, dataType: OutputInputEnum): LogisticAddress

    fun findByExternalIdInAndDataType(externalId: Collection<String>?, dataType: OutputInputEnum, pageable: Pageable): Page<LogisticAddress>

    fun findByDataType(dataType: OutputInputEnum, pageable: Pageable): Page<LogisticAddress>
}