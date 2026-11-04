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
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.LegalFormCreateResult
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.LegalFormCreateRaw
import org.eclipse.tractusx.bpdm.pool.service.operation.metadata.LegalFormCreateService
import org.eclipse.tractusx.bpdm.pool.service.parser.metadata.LegalFormCreateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates legal forms legal entities may be registered under, for every API version.
 */
@Service
class LegalFormCreateApplicationService(
    private val legalFormCreateParser: LegalFormCreateParser,
    private val legalFormCreateService: LegalFormCreateService,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Creates the legal form of [request] and returns it as stored, or every reason it is rejected.
     */
    @Transactional
    fun create(request: LegalFormCreateRaw): LegalFormCreateResult =
        when (val parseResult = legalFormCreateParser.parse(request)) {
            is ParseResult.Failure -> LegalFormCreateResult.Rejected(parseResult.errors)
            is ParseResult.Success -> LegalFormCreateResult.Created(
                dbToOutputMapper.toRecord(legalFormCreateService.create(parseResult.parsed))
            )
        }
}
