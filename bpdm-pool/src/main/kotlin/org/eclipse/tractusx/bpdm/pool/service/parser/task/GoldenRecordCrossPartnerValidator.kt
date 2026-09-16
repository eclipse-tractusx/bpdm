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
import org.eclipse.tractusx.bpdm.pool.model.request.GoldenRecordUpsertRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteUpsertRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Reports where the partners one golden record upsert states contradict each other.
 */
@Service
class GoldenRecordCrossPartnerValidator(
    private val referenceResolutionParser: BpnReferenceResolutionParser,
    private val sharedLegalAddressScriptCodeValidator: SharedLegalAddressScriptCodeValidator,
    private val statedAddressDistinctnessValidator: StatedAddressDistinctnessValidator,
    private val parentConsistencyValidator: GoldenRecordParentConsistencyValidator,
    private val coverageWriteReader: GoldenRecordCoverageWriteReader,
    private val coverageValidator: ScriptVariantCoverageValidator
) {

    /**
     * Reports every contradiction between the partners [request] states, where [errorsSoFar] holds what parsing those
     * partners has already rejected.
     */
    @Transactional(readOnly = true)
    fun validate(
        request: GoldenRecordUpsertRequest,
        errorsSoFar: List<GoldenRecordUpsertParseError>
    ): List<GoldenRecordUpsertParseError> =
        sharedLegalAddressScriptCodeValidator.validate(request.legalEntity, request.site)
            .plus(statedAddressDistinctness(request))
            .plus(membershipWithoutSite(request))
            .plus(parentConsistencyValidator.validate(request))
            .plus(coverageLosses(request, errorsSoFar))

    // The sites stated as sharing the record address share it with the record's own site, so there has to be one.
    private fun membershipWithoutSite(request: GoldenRecordUpsertRequest): List<GoldenRecordUpsertParseError> =
        if (request.site == null && request.addressSiteMembership.isNotEmpty()) listOf(AdditionalSitesWithoutSite)
        else emptyList()

    private fun statedAddressDistinctness(request: GoldenRecordUpsertRequest): List<GoldenRecordUpsertParseError> =
        statedAddressDistinctnessValidator.validate(
            referenceResolutionParser.parse(request.legalEntity.legalAddress.reference),
            (request.site as? SiteUpsertRequest.WithOwnMainAddress)?.let { referenceResolutionParser.parse(it.mainAddress.reference) },
            request.additionalAddress?.let { referenceResolutionParser.parse(it.reference) }
        )

    private fun coverageLosses(
        request: GoldenRecordUpsertRequest,
        errorsSoFar: List<GoldenRecordUpsertParseError>
    ): List<GoldenRecordUpsertParseError> {
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
