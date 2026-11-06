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

package org.eclipse.tractusx.bpdm.pool.service.parser.task

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.orRecord
import org.eclipse.tractusx.bpdm.common.model.parseResultOf
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.AdditionalSiteCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.AdditionalSitesParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.BpnReferenceParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.GoldenRecordTaskError
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.GoldenRecordTaskReferencesParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.task.AdditionalSiteRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.SiteUpsertRaw
import org.eclipse.tractusx.bpdm.pool.model.request.SiteHeaderRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteHeaderParser
import org.springframework.stereotype.Service

/**
 * Resolves the further sites a golden record task states next to its own site: the existing ones it links and the new
 * ones it creates.
 */
@Service
class AdditionalSitesParser(
    private val siteBpnParser: SiteBpnParser,
    private val siteHeaderParser: SiteHeaderParser
) {

    /**
     * Reports the existing and new sites among [additionalSites], or every problem that stops them.
     */
    fun parse(
        additionalSites: List<AdditionalSiteRaw>,
        site: SiteUpsertRaw,
        references: GoldenRecordTaskReferencesParsed
    ): ParseResult<AdditionalSitesParsed, GoldenRecordTaskError> {
        val errors = mutableListOf<GoldenRecordTaskError>()

        // The same site stated twice is one statement, identified by its reference or, carrying none, by its name.
        val statedOnce = additionalSites.distinctBy { it.reference.value ?: it.name }
        val (known, unknown) = statedOnce.map { it to references.resolve(it.reference) }.partition { it.second is BpnReferenceParsed.Existing }

        val existingSites = siteBpnParser.parse(known.map { (it.second as BpnReferenceParsed.Existing).bpn })
            .mapNotNull { it.orRecord(errors) { error -> GoldenRecordTaskError.AdditionalSiteInvalid(error) } }
        // A new site has had no confidence assessed of its own, so it borrows the one stated for the task's site.
        val newSites = unknown.mapNotNull { (additionalSite, reference) ->
            siteHeaderParser.parse(listOf(SiteHeaderRequest(additionalSite.name, emptyList(), site.header.confidenceCriteria, emptyList()))).single()
                .orRecord(errors) { GoldenRecordTaskError.AdditionalSiteCreateInvalid(it) }
                ?.let { AdditionalSiteCreateParsed(reference, it) }
        }

        return parseResultOf(AdditionalSitesParsed(existingSites, newSites), errors)
    }
}
