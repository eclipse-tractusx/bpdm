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

package org.eclipse.tractusx.bpdm.pool.service.application.v6

import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityWithLegalAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.toV6PoolDto
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityGetRequest
import org.eclipse.tractusx.bpdm.pool.service.operation.LegalEntityGetService
import org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntityGetParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "get legal entity" operation, using the v6 response shape.
 */
@Service
class LegalEntityGetApplicationV6Service(
    private val legalEntityGetParser: LegalEntityGetParser,
    private val legalEntityGetService: LegalEntityGetService
) {

    /**
     * Returns the legal entity carrying the given identifier value of the given identifier type, defaulting to the BPN
     * type, and fails with a not-found error when no legal entity carries it.
     */
    @Transactional(readOnly = true)
    fun getLegalEntity(identifierValue: String, identifierType: String?): LegalEntityWithLegalAddressVerboseDtoV6 {
        val criteria = legalEntityGetParser.parse(LegalEntityGetRequest(identifierValue, identifierType))
        val legalEntity = legalEntityGetService.get(criteria) ?: throw BpdmNotFoundException("Legal Entity", identifierValue)

        return legalEntity.toV6PoolDto()
    }
}
