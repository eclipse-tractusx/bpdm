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

import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalFormDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalFormRequestV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.inbound.LegalFormRequestMapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.LegalFormParseErrorMapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.LegalFormResponseMapperV6
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.service.operation.metadata.LegalFormCreateService
import org.eclipse.tractusx.bpdm.pool.service.parser.metadata.LegalFormCreateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "create legal form" operation, using the v6 request/response shapes.
 */
@Service
class LegalFormCreateApplicationV6Service(
    private val legalFormCreateParser: LegalFormCreateParser,
    private val legalFormCreateService: LegalFormCreateService,
    private val legalFormRequestMapperV6: LegalFormRequestMapperV6,
    private val legalFormParseErrorMapperV6: LegalFormParseErrorMapperV6,
    private val legalFormResponseMapperV6: LegalFormResponseMapperV6
) {

    /**
     * Creates the given legal form and returns it as stored.
     */
    @Transactional
    fun createLegalForm(legalForm: LegalFormRequestV6): LegalFormDtoV6 =
        when (val parsed = legalFormCreateParser.parse(legalFormRequestMapperV6.toCreateRequest(legalForm))) {
            is ParseResult.Failure -> throw legalFormParseErrorMapperV6.toCreateException(parsed.errors)
            is ParseResult.Success -> legalFormResponseMapperV6.toLegalForm(legalFormCreateService.create(parsed.parsed))
        }
}
