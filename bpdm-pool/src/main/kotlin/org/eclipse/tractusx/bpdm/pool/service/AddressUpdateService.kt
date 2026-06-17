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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.AddressUpserted
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.springframework.stereotype.Service

/**
 * Updates existing logistic addresses in two explicit phases: [parse] validates loose requests and resolves the update
 * target entity (never re-parents); [update] applies changes to already-parsed addresses. Both honour the order-preserving
 * positional list contract (see [ParseResult]).
 */
@Service
class AddressUpdateService {

    fun parse(requests: List<AddressUpdateRequest>): List<ParseResult<AddressUpdateParsed, AddressUpdateParseError>> =
        TODO("parse: validate requests to bounded addresses and resolve the update target")

    fun update(parsed: List<AddressUpdateParsed>): List<UpsertResult<AddressUpserted>> =
        TODO("update: apply changes to parsed addresses and map to results")
}
