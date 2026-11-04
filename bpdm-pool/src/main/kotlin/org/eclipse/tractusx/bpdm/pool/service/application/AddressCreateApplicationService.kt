/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.common.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.mapper.DbToOutputMapper
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressCreateResult
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AddressCreateRaw
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressCreateService
import org.eclipse.tractusx.bpdm.pool.service.parser.address.UntypedParentAddressCreateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates additional addresses under existing legal entities or sites.
 */
@Service
class AddressCreateApplicationService(
    private val untypedParentAddressCreateParser: UntypedParentAddressCreateParser,
    private val addressCreateService: AddressCreateService,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Creates the addresses of [requests] under their parents and returns, per entry, either the address as stored or
     * every reason that entry is rejected.
     */
    @Transactional
    fun create(requests: List<AddressCreateRaw>): List<AddressCreateResult> =
        parseAndExecute(requests, untypedParentAddressCreateParser::parse, addressCreateService::create).map { result ->
            when (result) {
                is ParseResult.Success -> AddressCreateResult.Created(dbToOutputMapper.toRecord(result.parsed))
                is ParseResult.Failure -> AddressCreateResult.Rejected(result.errors)
            }
        }
}
