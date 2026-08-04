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

package org.eclipse.tractusx.bpdm.pool.service.parser

import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressGetParsed
import org.eclipse.tractusx.bpdm.pool.model.request.AddressGetRequest
import org.springframework.stereotype.Service

/**
 * Turns a loose address fetch request into the normalized form the fetch operation looks the address up by.
 *
 * Unlike the upsert parsers this one returns its parsed value directly instead of a `ParseResult`: the requested BPN
 * cannot be rejected — that it names no address is the fetch's outcome, not a parse error.
 */
@Service
class AddressGetParser {

    /**
     * Normalizes the request so the address is looked up case-insensitively by its BPN.
     */
    fun parse(request: AddressGetRequest): AddressGetParsed =
        AddressGetParsed(addressBpn = request.addressBpn.uppercase())
}
