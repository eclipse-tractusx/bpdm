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
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressUpdateResult
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AddressUpdateRaw
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressPayloadUpdateService
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressUpdateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Updates existing addresses, optionally together with the complete set of sites they belong to.
 */
@Service
class AddressUpdateApplicationService(
    private val addressUpdateParser: AddressUpdateParser,
    private val addressPayloadUpdateService: AddressPayloadUpdateService,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Applies each entry of [requests] to the address it addresses by BPN and returns, per entry, either the address as
     * stored or every reason that entry is rejected.
     */
    @Transactional
    fun update(requests: List<AddressUpdateRaw>): List<AddressUpdateResult> =
        parseAndExecute(requests, addressUpdateParser::parse, addressPayloadUpdateService::update).map { result ->
            when (result) {
                is ParseResult.Success -> AddressUpdateResult.Updated(dbToOutputMapper.toRecord(result.parsed.value))
                is ParseResult.Failure -> AddressUpdateResult.Rejected(result.errors)
            }
        }
}
