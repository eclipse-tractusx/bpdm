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
import org.eclipse.tractusx.bpdm.pool.entity.LegalFormDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalFormCreateParsed
import org.eclipse.tractusx.bpdm.pool.repository.LegalFormRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Persists legal forms legal entities may be registered under.
 */
@Service
class LegalFormCreateService(
    private val legalFormRepository: LegalFormRepository
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Persists the given legal form and returns it as stored.
     */
    @Transactional
    fun create(legalForm: LegalFormCreateParsed): LegalFormDb {
        logger.info { "Create new Legal-Form with key ${legalForm.technicalKey} and name ${legalForm.name}" }

        val entity = LegalFormDb(
            technicalKey = legalForm.technicalKey,
            name = legalForm.name,
            transliteratedName = legalForm.transliteratedName,
            abbreviation = legalForm.abbreviations,
            transliteratedAbbreviations = legalForm.transliteratedAbbreviations,
            countryCode = legalForm.country,
            languageCode = legalForm.language,
            administrativeArea = legalForm.administrativeArea,
            isActive = legalForm.isActive
        )

        return legalFormRepository.save(entity)
    }
}
