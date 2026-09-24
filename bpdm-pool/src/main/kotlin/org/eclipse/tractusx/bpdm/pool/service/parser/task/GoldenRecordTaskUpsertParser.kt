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
import org.eclipse.tractusx.bpdm.pool.model.error.GoldenRecordTaskUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.GoldenRecordTaskUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.RecordAddressSitesParsed
import org.eclipse.tractusx.bpdm.pool.model.request.GoldenRecordTaskUpsertRequest
import org.eclipse.tractusx.bpdm.pool.util.parsedOrNull
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
    private val crossPartnerValidator: GoldenRecordTaskCrossPartnerValidator
) {

    /**
     * Reports what [request] amounts to, or every problem that stops it.
     */
    @Transactional(readOnly = true)
    fun parse(request: GoldenRecordTaskUpsertRequest): ParseResult<GoldenRecordTaskUpsertParsed, GoldenRecordTaskUpsertParseError> {
        val legalEntity = legalEntityUpsertParser.parse(request.legalEntity)
        val recordAddressSites = parseRecordAddressSites(request)
        val additionalAddress = request.additionalAddress?.let { additionalAddressUpsertParser.parse(it) } ?: ParseResult.Success(null)

        val contradictions = crossPartnerValidator.validate(
            request, legalEntity.parsedOrNull(), recordAddressSites.parsedOrNull(), additionalAddress.parsedOrNull()
        )

        return zipParseResults(legalEntity, recordAddressSites, additionalAddress) { legalEntityPlan, sitesPlan, addressPlan ->
            toUpsertParsed(request.sharingMemberRecordId, legalEntityPlan, sitesPlan, addressPlan)
        }.combine(contradictions) { it }
    }

    private fun parseRecordAddressSites(request: GoldenRecordTaskUpsertRequest): ParseResult<RecordAddressSitesParsed?, GoldenRecordTaskUpsertParseError> =
        request.sites.recordSite?.let { siteRequest ->
            zipParseResults(
                siteUpsertParser.parse(siteRequest),
                additionalSitesParser.parse(request.sites.additionalSites, siteRequest.header.confidenceCriteria),
                ::RecordAddressSitesParsed
            )
        } ?: ParseResult.Success(null)

    private fun toUpsertParsed(
        sharingMemberRecordId: String,
        legalEntity: LegalEntityUpsertParsed,
        recordAddressSites: RecordAddressSitesParsed?,
        additionalAddress: AddressUpsertParsed?
    ): GoldenRecordTaskUpsertParsed =
        when {
            recordAddressSites != null && additionalAddress != null ->
                GoldenRecordTaskUpsertParsed.SiteAddress(sharingMemberRecordId, legalEntity, recordAddressSites, additionalAddress)
            recordAddressSites != null ->
                GoldenRecordTaskUpsertParsed.Site(sharingMemberRecordId, legalEntity, recordAddressSites)
            additionalAddress != null ->
                GoldenRecordTaskUpsertParsed.LegalEntityAddress(sharingMemberRecordId, legalEntity, additionalAddress)
            else ->
                GoldenRecordTaskUpsertParsed.LegalEntity(sharingMemberRecordId, legalEntity)
        }
}
