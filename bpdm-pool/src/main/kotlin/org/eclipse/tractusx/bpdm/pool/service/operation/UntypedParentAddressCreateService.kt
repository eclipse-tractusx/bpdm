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

package org.eclipse.tractusx.bpdm.pool.service.operation

import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateUntypedParentRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.chainParseResults
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.service.parser.AddressParentResolutionParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
/**
 * Creates logistic addresses from a single, *untyped* parent BPN (the V7 "additional address" REST path). This is the
 * topmost stage of the parent-resolution pipeline: it resolves the BPN into the explicit (legalEntity, site) parents
 * [AdditionalAddressCreateService] expects — reporting the precise [org.eclipse.tractusx.bpdm.pool.model.InvalidParentBpn]/[org.eclipse.tractusx.bpdm.pool.model.UnresolvableLegalEntity]/
 * [org.eclipse.tractusx.bpdm.pool.model.UnresolvableSite] errors that the typed stage cannot distinguish (see [org.eclipse.tractusx.bpdm.pool.service.parser.AddressParentResolutionParser]) — then
 * delegates all content validation and persistence to that lower service. Order-preserving positional contract (see
 * [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class UntypedParentAddressCreateService(
    private val additionalAddressCreateService: AdditionalAddressCreateService,
    private val addressParentResolutionParser: AddressParentResolutionParser
) {

    fun parse(requests: List<AddressCreateUntypedParentRequest>): List<ParseResult<AddressCreateParsed, AddressCreateParseError>> =
        chainParseResults(addressParentResolutionParser.parse(requests)) { typed -> additionalAddressCreateService.parse(typed) }

    fun create(parsed: List<AddressCreateParsed>): List<LogisticAddressDb> =
        additionalAddressCreateService.create(parsed)

    @Transactional
    fun parseAndCreate(requests: List<AddressCreateUntypedParentRequest>): List<ParseResult<LogisticAddressDb, AddressCreateParseError>> =
        parseAndExecute(requests, ::parse, ::create)
}