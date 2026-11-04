/*******************************************************************************
 * Copyright (c) 2021,2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.application

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.mapper.DbToOutputMapper
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.IdentifierTypeCreateResult
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.IdentifierTypeCreateRaw
import org.eclipse.tractusx.bpdm.pool.service.operation.metadata.IdentifierTypeCreateService
import org.eclipse.tractusx.bpdm.pool.service.parser.metadata.IdentifierTypeCreateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates identifier types business partners may be identified by.
 */
@Service
class IdentifierTypeCreateApplicationService(
    private val identifierTypeCreateParser: IdentifierTypeCreateParser,
    private val identifierTypeCreateService: IdentifierTypeCreateService,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Creates the identifier type of [request] and returns it as stored, or every reason it is rejected.
     */
    @Transactional
    fun create(request: IdentifierTypeCreateRaw): IdentifierTypeCreateResult =
        when (val parseResult = identifierTypeCreateParser.parse(request)) {
            is ParseResult.Failure -> IdentifierTypeCreateResult.Rejected(parseResult.errors)
            is ParseResult.Success -> IdentifierTypeCreateResult.Created(
                dbToOutputMapper.toRecord(identifierTypeCreateService.create(parseResult.parsed))
            )
        }
}
