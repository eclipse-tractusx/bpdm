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

import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressUpsertPlan
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpsertPlan
import org.eclipse.tractusx.bpdm.pool.model.parsed.RecordSitePlan
import org.eclipse.tractusx.bpdm.pool.model.request.GoldenRecordUpsertRequest
import org.eclipse.tractusx.bpdm.pool.model.request.RecordSiteRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteUpsertRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Reports where the partners one golden record upsert states contradict each other.
 *
 * Most of these rules read the partners as stated, but completeness of the additional sites is decidable only against
 * the address they will sit on, which is why the planned partners are taken as well.
 */
@Service
class GoldenRecordCrossPartnerValidator(
    private val referenceResolutionParser: BpnReferenceResolutionParser,
    private val sharedLegalAddressScriptCodeValidator: SharedLegalAddressScriptCodeValidator,
    private val statedAddressDistinctnessValidator: StatedAddressDistinctnessValidator,
    private val parentConsistencyValidator: GoldenRecordParentConsistencyValidator,
    private val additionalSitesCompletenessValidator: AdditionalSitesCompletenessValidator,
    private val coverageWriteReader: GoldenRecordCoverageWriteReader,
    private val coverageValidator: ScriptVariantCoverageValidator
) {

    /**
     * Reports every contradiction between the partners [request] states and the partners planned from it, where
     * [errorsSoFar] holds what planning those partners has already rejected.
     */
    @Transactional(readOnly = true)
    fun validate(
        request: GoldenRecordUpsertRequest,
        legalEntity: LegalEntityUpsertPlan?,
        recordSite: RecordSitePlan?,
        additionalAddress: AddressUpsertPlan?,
        errorsSoFar: List<GoldenRecordUpsertParseError>
    ): List<CrossPartnerParseError> =
        sharedLegalAddressScriptCodeValidator.validate(request.legalEntity, request.recordSite.site)
            .plus(statedAddressDistinctness(request))
            .plus(additionalSitesWithoutSite(request.recordSite))
            .plus(parentConsistencyValidator.validate(request))
            .plus(additionalSitesCompletenessValidator.validate(legalEntity, recordSite, additionalAddress))
            .plus(coverageLosses(request, errorsSoFar))

    // The additional sites share the record address with the record's own site, so there has to be one.
    private fun additionalSitesWithoutSite(recordSite: RecordSiteRequest): List<CrossPartnerParseError> =
        if (recordSite.site == null && recordSite.additionalSites.isNotEmpty()) listOf(AdditionalSitesWithoutSite)
        else emptyList()

    private fun statedAddressDistinctness(request: GoldenRecordUpsertRequest): List<CrossPartnerParseError> =
        statedAddressDistinctnessValidator.validate(
            referenceResolutionParser.parse(request.legalEntity.legalAddress.reference),
            (request.recordSite.site as? SiteUpsertRequest.WithOwnMainAddress)?.let { referenceResolutionParser.parse(it.mainAddress.reference) },
            request.additionalAddress?.let { referenceResolutionParser.parse(it.reference) }
        )

    private fun coverageLosses(
        request: GoldenRecordUpsertRequest,
        errorsSoFar: List<GoldenRecordUpsertParseError>
    ): List<CrossPartnerParseError> {
        // Coverage reads the partners a written address is shared with. A partner this request failed to resolve is
        // not among them as far as the check can tell, so it would report the request taking away coverage it never
        // had. The other cross-partner checks resolve what they need themselves and stay meaningful.
        val unresolved = errorsSoFar.any {
            it is LegalEntityNotFound || it is SiteNotFound || it is SiteMainAddressNotFound || it is AdditionalAddressNotFound
        }
        if (unresolved) return emptyList()

        return coverageValidator.validate(listOf(coverageWriteReader.writesOf(request)))
            .single()
            .map { ScriptVariantCoverageLost(it) }
    }
}
