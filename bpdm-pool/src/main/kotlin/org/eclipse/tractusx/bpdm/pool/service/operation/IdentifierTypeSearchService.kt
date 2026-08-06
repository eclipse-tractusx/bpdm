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

package org.eclipse.tractusx.bpdm.pool.service.operation

import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.IdentifierTypeSearchParsed
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository.Specs.byBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository.Specs.byCountry
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Queries the identifier types business partners may be identified by.
 */
@Service
class IdentifierTypeSearchService(
    private val identifierTypeRepository: IdentifierTypeRepository
) {

    /**
     * Returns the requested page of identifier types issued for the given business partner type, restricted to those
     * applying to the given country where one is named.
     */
    @Transactional(readOnly = true)
    fun search(criteria: IdentifierTypeSearchParsed, pageable: Pageable): Page<IdentifierTypeDb> {
        val specification = Specification.allOf(
            byBusinessPartnerType(criteria.businessPartnerType),
            byCountry(criteria.country)
        )

        return identifierTypeRepository.findAll(specification, pageable)
    }
}
