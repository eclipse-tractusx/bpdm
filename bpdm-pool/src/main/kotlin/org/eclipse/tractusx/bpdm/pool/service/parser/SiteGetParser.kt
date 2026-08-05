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

import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteGetParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SiteGetRequest
import org.springframework.stereotype.Service

/**
 * Turns a loose site fetch request into the normalized form the fetch operation looks the site up by.
 *
 * Unlike the upsert parsers this one returns its parsed value directly instead of a `ParseResult`: the requested BPN
 * cannot be rejected — that it names no site is the fetch's outcome, not a parse error.
 */
@Service
class SiteGetParser {

    /**
     * Normalizes the request so the site is looked up case-insensitively by its BPN.
     */
    fun parse(request: SiteGetRequest): SiteGetParsed =
        SiteGetParsed(siteBpn = request.siteBpn.uppercase())
}
