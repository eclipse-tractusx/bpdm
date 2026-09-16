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

package org.eclipse.tractusx.bpdm.pool.service.parser.task

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.error.GoldenRecordUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.error.MembershipSiteContentInvalid
import org.eclipse.tractusx.bpdm.pool.model.error.MembershipSiteNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.SiteNotFound
import org.eclipse.tractusx.bpdm.pool.model.parsed.MembershipSiteCreatePlan
import org.eclipse.tractusx.bpdm.pool.model.parsed.ResolvedReference
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteMembershipPlan
import org.eclipse.tractusx.bpdm.pool.model.request.ConfidenceCriteriaRequest
import org.eclipse.tractusx.bpdm.pool.model.request.GoldenRecordUpsertRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteHeaderRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteReferenceRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteHeaderParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides which sites a golden record upsert states as sharing its record address: the ones that already exist, and
 * the ones it asks to be created there.
 *
 * A site the membership asks to be created has had no confidence assessed of its own, so the caller passes the
 * assessment it borrows — the one the record's own site carries.
 */
@Service
class SiteMembershipParser(
    private val siteReferenceParser: SiteReferenceParser,
    private val siteHeaderParser: SiteHeaderParser
) {

    /**
     * Reports the membership [stated] amounts to, or every reason an entry of it cannot be accepted.
     */
    @Transactional(readOnly = true)
    fun parse(
        stated: List<SiteReferenceRequest>,
        borrowedConfidence: ConfidenceCriteriaRequest
    ): ParseResult<SiteMembershipPlan, GoldenRecordUpsertParseError> {
        // The same site stated twice is one statement written twice, not two memberships. An entry is identified by
        // the reference it carries and, carrying none, by the name its site is to be created under.
        val statedOnce = stated.distinctBy { it.reference.value ?: it.name }

        val resolutions = statedOnce.mapIndexed { index, entry -> resolve(entry, index) }
        val creations = parseCreations(statedOnce, resolutions, borrowedConfidence)

        val errors = resolutions.failureErrors() + creations.failureErrors()
        if (errors.isNotEmpty()) return ParseResult.Failure(errors)

        return ParseResult.Success(
            SiteMembershipPlan(
                existingSites = resolutions.mapNotNull { it.parsedOrNull()?.target },
                newSites = creations.mapNotNull { it.parsedOrNull() }
            )
        )
    }

    private fun resolve(stated: SiteReferenceRequest, index: Int): ParseResult<ResolvedReference<SiteDb>, GoldenRecordUpsertParseError> =
        when (val result = siteReferenceParser.parse(stated.reference)) {
            is ParseResult.Success -> result
            // The membership list is positional to the caller, so a rejection says which entry it is about.
            is ParseResult.Failure ->
                ParseResult.Failure(result.errors.map { if (it is SiteNotFound) MembershipSiteNotFound(index, it.bpn) else it })
        }

    private fun parseCreations(
        statedOnce: List<SiteReferenceRequest>,
        resolutions: List<ParseResult<ResolvedReference<SiteDb>, GoldenRecordUpsertParseError>>,
        confidence: ConfidenceCriteriaRequest
    ): List<ParseResult<MembershipSiteCreatePlan?, GoldenRecordUpsertParseError>> {
        // An entry naming no site yet asks for one to be created on the record's address.
        val stated = resolutions.withIndex().mapNotNull { (index, resolution) ->
            resolution.parsedOrNull()?.takeIf { it.target == null }?.let { index to it.reference }
        }

        val headers = siteHeaderParser.parse(
            stated.map { (index, _) -> SiteHeaderRequest(statedOnce[index].name, emptyList(), confidence, emptyList()) }
        )

        val creationByIndex = stated.zip(headers).associate { (entry, header) ->
            val (index, reference) = entry
            index to when (header) {
                is ParseResult.Success -> ParseResult.Success(MembershipSiteCreatePlan(reference, header.parsed))
                is ParseResult.Failure -> ParseResult.Failure(header.errors.map { MembershipSiteContentInvalid(index, it) })
            }
        }

        return resolutions.indices.map { creationByIndex[it] ?: ParseResult.Success(null) }
    }
}
