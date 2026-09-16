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
import org.eclipse.tractusx.bpdm.common.model.combine
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.model.error.GoldenRecordUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressUpsertPlan
import org.eclipse.tractusx.bpdm.pool.model.parsed.GoldenRecordUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpsertPlan
import org.eclipse.tractusx.bpdm.pool.model.parsed.RecordSitePlan
import org.eclipse.tractusx.bpdm.pool.model.request.GoldenRecordUpsertRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Turns a golden record upsert request into the plan of what it will do, or into every reason it cannot be done.
 *
 * One request at a time, not a batch: entries of one reservation may name the same request identifier and must then
 * reach the same record, which is only known once the earlier entry has been written. The request's own content is
 * still parsed through the batch-shaped content parsers, one entry's worth at a time.
 */
@Service
class GoldenRecordTaskUpsertParser(
    private val legalEntityUpsertParser: LegalEntityUpsertParser,
    private val siteUpsertParser: SiteUpsertParser,
    private val additionalAddressUpsertParser: AdditionalAddressUpsertParser,
    private val siteMembershipParser: SiteMembershipParser,
    private val crossPartnerValidator: GoldenRecordCrossPartnerValidator,
    private val recordAddressSitesParser: RecordAddressSitesParser
) {

    /**
     * Reports the plan [request] amounts to, or every problem that stops it.
     */
    @Transactional(readOnly = true)
    fun parse(request: GoldenRecordUpsertRequest): ParseResult<GoldenRecordUpsertParsed, GoldenRecordUpsertParseError> {
        val legalEntity: ParseResult<LegalEntityUpsertPlan, GoldenRecordUpsertParseError> =
            legalEntityUpsertParser.parse(request.legalEntity)
        val recordSite: ParseResult<RecordSitePlan?, GoldenRecordUpsertParseError> =
            request.site?.let { stated ->
                zipParseResults(siteUpsertParser.parse(stated, request.legalEntity), siteMembershipParser.parse(request), ::RecordSitePlan)
            } ?: ParseResult.Success(null)
        val additionalAddress: ParseResult<AddressUpsertPlan?, GoldenRecordUpsertParseError> =
            request.additionalAddress?.let { additionalAddressUpsertParser.parse(it) } ?: ParseResult.Success(null)

        val parsed = listOf(legalEntity, recordSite, additionalAddress)
        val contradictions = crossPartnerValidator.validate(request, parsed.failureErrors()) +
                recordAddressSitesParser.validate(legalEntity.parsedOrNull(), recordSite.parsedOrNull())

        return zipParseResults(legalEntity, recordSite, additionalAddress) { entity, site, address ->
            plan(request.sharingMemberRecordId, entity, recordAddressSitesParser.withBoundSites(entity, site, address), address)
        }.combine(contradictions) { it }
    }

    private fun plan(
        sharingMemberRecordId: String,
        legalEntity: LegalEntityUpsertPlan,
        recordSite: RecordSitePlan?,
        additionalAddress: AddressUpsertPlan?
    ): GoldenRecordUpsertParsed =
        when {
            recordSite != null && additionalAddress != null ->
                GoldenRecordUpsertParsed.SiteAddressRecord(
                    sharingMemberRecordId, legalEntity, recordSite.site, additionalAddress, recordSite.coLocatedSites
                )
            recordSite != null ->
                GoldenRecordUpsertParsed.SiteRecord(sharingMemberRecordId, legalEntity, recordSite.site, recordSite.coLocatedSites)
            additionalAddress != null ->
                GoldenRecordUpsertParsed.LegalEntityAddressRecord(sharingMemberRecordId, legalEntity, additionalAddress)
            else ->
                GoldenRecordUpsertParsed.LegalEntityRecord(sharingMemberRecordId, legalEntity)
        }
}
