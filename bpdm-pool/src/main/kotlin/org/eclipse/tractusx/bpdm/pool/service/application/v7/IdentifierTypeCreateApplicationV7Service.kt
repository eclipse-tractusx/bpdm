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

import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierTypeDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.IdentifierTypeRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.IdentifierTypeResponseMapper
import org.eclipse.tractusx.bpdm.pool.mapper.shared.outbound.IdentifierTypeParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.service.operation.metadata.IdentifierTypeCreateService
import org.eclipse.tractusx.bpdm.pool.service.parser.metadata.IdentifierTypeCreateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the V7 "create identifier type" operation.
 */
@Service
class IdentifierTypeCreateApplicationV7Service(
    private val identifierTypeCreateParser: IdentifierTypeCreateParser,
    private val identifierTypeCreateService: IdentifierTypeCreateService,
    private val identifierTypeRequestMapper: IdentifierTypeRequestMapper,
    private val identifierTypeResponseMapper: IdentifierTypeResponseMapper,
    private val identifierTypeParseErrorMapper: IdentifierTypeParseErrorMapper
) {

    /**
     * Creates the given identifier type and returns it as stored.
     */
    @Transactional
    fun createIdentifierType(identifierType: IdentifierTypeDto): IdentifierTypeDto =
        when (val parsed = identifierTypeCreateParser.parse(identifierTypeRequestMapper.toCreateRequest(identifierType))) {
            is ParseResult.Failure -> throw identifierTypeParseErrorMapper.toCreateException(parsed.errors)
            is ParseResult.Success -> identifierTypeResponseMapper.toIdentifierType(identifierTypeCreateService.create(parsed.parsed))
        }
}
