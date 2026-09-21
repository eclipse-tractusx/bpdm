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
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.GoldenRecordUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.RecordSiteParsed
import org.eclipse.tractusx.bpdm.pool.model.request.GoldenRecordUpsertRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Turns a golden record upsert request into what it will do, or into every reason it cannot be done.
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
    private val additionalSitesParser: AdditionalSitesParser,
    private val crossPartnerValidator: GoldenRecordCrossPartnerValidator
) {

    /**
     * Reports what [request] amounts to, or every problem that stops it.
     */
    @Transactional(readOnly = true)
    fun parse(request: GoldenRecordUpsertRequest): ParseResult<GoldenRecordUpsertParsed, GoldenRecordUpsertParseError> {
        val legalEntity = legalEntityUpsertParser.parse(request.legalEntity)
        val recordSite = parseRecordSite(request)
        val additionalAddress = request.additionalAddress?.let { additionalAddressUpsertParser.parse(it) } ?: ParseResult.Success(null)

        val partnerResults = listOf(legalEntity, recordSite, additionalAddress)
        val contradictions = crossPartnerValidator.validate(
            request, legalEntity.parsedOrNull(), recordSite.parsedOrNull(), additionalAddress.parsedOrNull(), partnerResults.failureErrors()
        )

        return zipParseResults(legalEntity, recordSite, additionalAddress) { legalEntityPlan, sitePlan, addressPlan ->
            toUpsertParsed(request.sharingMemberRecordId, legalEntityPlan, sitePlan, addressPlan)
        }.combine(contradictions) { it }
    }

    private fun parseRecordSite(request: GoldenRecordUpsertRequest): ParseResult<RecordSiteParsed?, GoldenRecordUpsertParseError> =
        request.recordSite.site?.let { siteRequest ->
            zipParseResults(
                siteUpsertParser.parse(siteRequest),
                additionalSitesParser.parse(request.recordSite.additionalSites, siteRequest.header.confidenceCriteria),
                ::RecordSiteParsed
            )
        } ?: ParseResult.Success(null)

    private fun toUpsertParsed(
        sharingMemberRecordId: String,
        legalEntity: LegalEntityUpsertParsed,
        recordSite: RecordSiteParsed?,
        additionalAddress: AddressUpsertParsed?
    ): GoldenRecordUpsertParsed =
        when {
            recordSite != null && additionalAddress != null ->
                GoldenRecordUpsertParsed.SiteAddressRecord(
                    sharingMemberRecordId, legalEntity, recordSite.site, additionalAddress, recordSite.additionalSites
                )
            recordSite != null ->
                GoldenRecordUpsertParsed.SiteRecord(sharingMemberRecordId, legalEntity, recordSite.site, recordSite.additionalSites)
            additionalAddress != null ->
                GoldenRecordUpsertParsed.LegalEntityAddressRecord(sharingMemberRecordId, legalEntity, additionalAddress)
            else ->
                GoldenRecordUpsertParsed.LegalEntityRecord(sharingMemberRecordId, legalEntity)
        }
}
