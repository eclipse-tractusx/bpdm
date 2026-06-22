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

import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates logistic addresses whose parents are given by BPN (the "additional address" REST and task paths). This is the
 * thin, resolving layer on top of [AddressCreateService]: it resolves the legal entity / site BPNs to entities (yielding
 * `UnresolvableLegalEntity`/`UnresolvableSite` on a miss) and delegates content validation and persistence to the lower
 * service. Order-preserving positional contract (see [ParseResult]).
 */
@Service
class AdditionalAddressCreateService(
    private val addressCreateService: AddressCreateService,
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val siteBpnParser: SiteBpnParser
) {

    fun parse(requests: List<AddressCreateRequest>): List<ParseResult<AddressCreateParsed, AddressCreateParseError>> {
        val contentResults = addressCreateService.parseContent(requests.map { it.content })
        val legalEntityResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn })
        val siteResults = siteBpnParser.parse(requests.map { it.siteBpn })

        return zipParseResults(contentResults, legalEntityResults, siteResults) { content, legalEntity, site ->
            AddressCreateParsed(legalEntity, site, content.address, content.scriptVariants)
        }
    }

    @Transactional
    fun parseAndCreate(requests: List<AddressCreateRequest>): List<ParseResult<LogisticAddressDb, AddressCreateParseError>> =
        addressCreateService.parseAndCreate(parse(requests))
}
