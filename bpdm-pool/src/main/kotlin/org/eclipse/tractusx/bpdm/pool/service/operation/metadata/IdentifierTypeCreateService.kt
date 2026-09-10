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

package org.eclipse.tractusx.bpdm.pool.service.operation.metadata

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDetailDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.IdentifierTypeCreateParsed
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Persists identifier types business partners may be identified by.
 */
@Service
class IdentifierTypeCreateService(
    private val identifierTypeRepository: IdentifierTypeRepository
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Persists the given identifier type with its categories and country details, and returns it as stored.
     */
    @Transactional
    fun create(identifierType: IdentifierTypeCreateParsed): IdentifierTypeDb {
        val entity = IdentifierTypeDb(
            technicalKey = identifierType.technicalKey,
            businessPartnerType = identifierType.businessPartnerType,
            name = identifierType.name,
            abbreviation = identifierType.abbreviation,
            transliteratedName = identifierType.transliteratedName,
            transliteratedAbbreviation = identifierType.transliteratedAbbreviation,
            format = identifierType.format
        )
        entity.categories.addAll(identifierType.categories)
        entity.details.addAll(identifierType.details.map { IdentifierTypeDetailDb(entity, it.country, it.mandatory) })

        return identifierTypeRepository.save(entity)
            .also {
                logger.info {
                    "Created identifier type '${it.technicalKey}' for ${it.businessPartnerType} with name ${it.name}"
                }
            }
    }
}
