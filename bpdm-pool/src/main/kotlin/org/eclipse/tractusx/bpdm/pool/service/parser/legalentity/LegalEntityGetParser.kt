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

package org.eclipse.tractusx.bpdm.pool.service.parser.legalentity

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityGetParseError
import org.eclipse.tractusx.bpdm.pool.model.error.UnresolvableLegalEntityIdentifier
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityGetRequest
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.springframework.stereotype.Service

/**
 * Resolves a legal entity fetch request to the legal entity it names.
 *
 * The request names its legal entity either by BPN or by one of its other identifiers, which are looked up by different
 * queries; the identifier type the request leaves out defaults to the BPN type.
 */
@Service
class LegalEntityGetParser(
    private val bpnConfigProperties: BpnConfigProperties,
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val legalEntityRepository: LegalEntityRepository
) {

    /**
     * Resolves the requested identifier to its legal entity, failing when no legal entity carries it.
     */
    fun parse(request: LegalEntityGetRequest): ParseResult<LegalEntityDb, LegalEntityGetParseError> {
        val identifierType = request.identifierType ?: bpnConfigProperties.id

        return when (identifierType) {
            bpnConfigProperties.id -> legalEntityBpnParser.parse(listOf(request.identifierValue)).single()
            else -> resolveByIdentifier(identifierType, request.identifierValue)
        }
    }

    private fun resolveByIdentifier(identifierTypeKey: String, identifierValue: String): ParseResult<LegalEntityDb, LegalEntityGetParseError> {
        val legalEntity = legalEntityRepository.findByIdentifierTypeKeyAndValueIgnoreCase(
            IdentifierBusinessPartnerType.LEGAL_ENTITY,
            identifierTypeKey,
            identifierValue
        )

        return when (legalEntity) {
            null -> ParseResult.ofSingleFailure(UnresolvableLegalEntityIdentifier(identifierTypeKey, identifierValue))
            else -> ParseResult.Success(legalEntity)
        }
    }
}
