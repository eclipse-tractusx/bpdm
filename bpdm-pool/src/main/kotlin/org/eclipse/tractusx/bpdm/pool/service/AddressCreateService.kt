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
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateRequest
import org.eclipse.tractusx.bpdm.pool.model.AddressUpserted
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.springframework.stereotype.Service

/**
 * Creates logistic addresses in two explicit phases so callers can route validation failures themselves:
 * [parse] validates loose requests and resolves parents to entities; [create] persists already-parsed addresses.
 * Both honour the order-preserving positional list contract (see [ParseResult]).
 */
@Service
class AddressCreateService {

    fun parse(requests: List<AddressCreateRequest>): List<ParseResult<AddressCreateParsed, AddressCreateParseError>> =
        TODO("parse: validate requests to bounded addresses and resolve legal entity/site parents")

    fun create(parsed: List<AddressCreateParsed>): List<UpsertResult<AddressUpserted>> =
        TODO("create: persist parsed addresses and map to results")
}
