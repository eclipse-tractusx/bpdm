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

package org.eclipse.tractusx.bpdm.pool.service.parser.site

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.model.error.SiteCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SiteCreateRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityBpnParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates site-create requests that bring a main address of their own: the parent legal entity, the header content
 * and that new address.
 */
@Service
class SiteCreateWithOwnMainAddressParser(
    private val siteContentParser: SiteContentParser,
    private val legalEntityBpnParser: LegalEntityBpnParser
) {

    /**
     * Validates each request and reports either the validated site with its resolved parent or every problem found in
     * that entry.
     */
    @Transactional(readOnly = true)
    fun parse(requests: List<SiteCreateRequest>): List<ParseResult<SiteCreateParsed, SiteCreateParseError>> {
        val contents = requests.map { it.content }
        val contentResults = siteContentParser.parse(contents, contents.map { null })
        val legalEntityResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn })

        return zipParseResults(contentResults, legalEntityResults) { content, legalEntity ->
            SiteCreateParsed(legalEntity, content)
        }
    }
}
