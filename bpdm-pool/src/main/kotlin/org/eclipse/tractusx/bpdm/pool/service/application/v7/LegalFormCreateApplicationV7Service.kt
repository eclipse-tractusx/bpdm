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

package org.eclipse.tractusx.bpdm.pool.service.application.v7

import org.eclipse.tractusx.bpdm.pool.api.model.LegalFormDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.LegalFormRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.LegalFormParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.LegalFormResponseMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.service.operation.metadata.LegalFormCreateService
import org.eclipse.tractusx.bpdm.pool.service.parser.metadata.LegalFormCreateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalFormRequest as LegalFormRequestDto

/**
 * The REST-API boundary for the V7 "create legal form" operation.
 */
@Service
class LegalFormCreateApplicationV7Service(
    private val legalFormCreateParser: LegalFormCreateParser,
    private val legalFormCreateService: LegalFormCreateService,
    private val legalFormRequestMapper: LegalFormRequestMapper,
    private val legalFormParseErrorMapper: LegalFormParseErrorMapper,
    private val legalFormResponseMapper: LegalFormResponseMapper
) {

    /**
     * Creates the given legal form and returns it as stored.
     */
    @Transactional
    fun createLegalForm(legalForm: LegalFormRequestDto): LegalFormDto =
        when (val parsed = legalFormCreateParser.parse(legalFormRequestMapper.toCreateRequest(legalForm))) {
            is ParseResult.Failure -> throw legalFormParseErrorMapper.toCreateException(parsed.errors)
            is ParseResult.Success -> legalFormResponseMapper.toLegalForm(legalFormCreateService.create(parsed.parsed))
        }
}
