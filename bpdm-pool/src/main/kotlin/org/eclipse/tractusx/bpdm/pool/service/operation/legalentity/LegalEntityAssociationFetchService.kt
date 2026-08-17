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

package org.eclipse.tractusx.bpdm.pool.service.operation.legalentity

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressAssociationFetchService
import org.springframework.stereotype.Service

/**
 * Loads the associations a legal entity response is built from, one query per association instead of one per legal
 * entity.
 */
@Service
class LegalEntityAssociationFetchService(
    private val legalEntityRepository: LegalEntityRepository,
    private val legalEntityIdentifierRepository: LegalEntityIdentifierRepository,
    private val addressAssociationFetchService: AddressAssociationFetchService
) {

    /**
     * Loads the legal form, identifiers with their types, states, relations and legal address of the given legal
     * entities into the persistence context, along with everything those legal addresses are rendered from.
     */
    fun fetch(legalEntities: Set<LegalEntityDb>) {
        if (legalEntities.isEmpty()) return

        legalEntityRepository.joinLegalForm(legalEntities)
        legalEntityRepository.joinIdentifiers(legalEntities)
        legalEntityRepository.joinStates(legalEntities)
        legalEntityRepository.joinRelations(legalEntities)
        legalEntityIdentifierRepository.joinType(legalEntities.flatMap { it.identifiers }.toSet())

        legalEntityRepository.joinLegalAddresses(legalEntities)
        addressAssociationFetchService.fetch(legalEntities.map { it.legalAddress }.toSet())
    }
}
