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

import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.BpnReferenceAllocation
import org.eclipse.tractusx.bpdm.pool.model.error.ScriptVariantCoverageParseError
import org.eclipse.tractusx.bpdm.pool.model.request.BpnReferenceRequest
import org.eclipse.tractusx.bpdm.pool.model.request.GoldenRecordUpsertRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteUpsertRequest
import org.eclipse.tractusx.bpdm.pool.model.request.UpsertIntent
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressPartnerScriptCodeReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides the script variant coverage a single golden record upsert cannot decide on its own: whether the upsert
 * leaves a business partner it does not write named in a script its address no longer covers.
 *
 * An upsert writes its legal entity, its site and their addresses in several operations, and one address can be
 * written twice when a site's main address is the legal address, so every operation sees a half-written state. Those
 * operations therefore parse without the coverage check and coverage is judged here, once, for the request as a whole.
 */
@Service
class GoldenRecordTaskCoverageValidator(
    private val logisticAddressRepository: LogisticAddressRepository,
    private val partnerReader: AddressPartnerScriptCodeReader,
    private val scriptVariantCoverageValidator: ScriptVariantCoverageValidator
) {

    /**
     * Reports every coverage [request] would take away from a business partner outside it.
     */
    @Transactional(readOnly = true)
    fun validate(
        request: GoldenRecordUpsertRequest,
        bpnReferences: BpnReferenceAllocation
    ): List<ScriptVariantCoverageParseError> {
        val rewrittenBpns = rewrittenPartnerBpns(request, bpnReferences)

        return writtenAddresses(request, bpnReferences).flatMap { (address, coveredScriptCodes) ->
            scriptVariantCoverageValidator.check(coveredScriptCodes, partnerReader.storedPartners(address, rewrittenBpns))
        }
    }

    /**
     * The already persisted addresses this request rewrites, each with the script codes its new payload will cover.
     * An address the request creates is not among them: it takes no coverage away from anyone.
     */
    private fun writtenAddresses(
        request: GoldenRecordUpsertRequest,
        bpnReferences: BpnReferenceAllocation
    ): List<Pair<LogisticAddressDb, List<String>>> {
        val site = request.site?.takeIf { it.intent == UpsertIntent.AlwaysWrite }
        val legalEntityWritten = request.legalEntity.intent == UpsertIntent.AlwaysWrite
        val legalEntityScriptCodes = request.legalEntity.header.scriptVariants.map { it.scriptCode }
        val siteScriptCodes = site?.header?.scriptVariants?.map { it.scriptCode }.orEmpty()

        // A site whose main address is the legal address is covered by that one address, so both partners' script codes
        // end up on it - the request writes their union there (see TaskStepBuildService.legalAddressCoverageNotStatedBy).
        val siteSharesLegalAddress = site is SiteUpsertRequest.WithLegalAddressAsMain
        val legalAddress = request.legalEntity.legalAddress.reference
            .takeIf { legalEntityWritten || siteSharesLegalAddress }
            ?.let { resolveAddress(it, bpnReferences) }
        val legalAddressScriptCodes =
            if (siteSharesLegalAddress) legalEntityScriptCodes.plus(siteScriptCodes).distinct() else legalEntityScriptCodes

        val siteMainAddress = (site as? SiteUpsertRequest.WithOwnMainAddress)
            ?.mainAddress?.reference
            ?.let { resolveAddress(it, bpnReferences) }

        return listOfNotNull(
            legalAddress?.let { it to legalAddressScriptCodes },
            siteMainAddress?.let { it to siteScriptCodes }
        )
    }

    /**
     * The BPNs of the business partners this request writes itself; a partner reported as unchanged is not among them,
     * so the coverage it has today has to survive the request.
     */
    private fun rewrittenPartnerBpns(request: GoldenRecordUpsertRequest, bpnReferences: BpnReferenceAllocation): Set<String> {
        val legalEntityBpn = request.legalEntity
            .takeIf { it.intent == UpsertIntent.AlwaysWrite }
            ?.let { bpnReferences.resolve(it.reference) }
        val siteBpn = request.site
            ?.takeIf { it.intent == UpsertIntent.AlwaysWrite }
            ?.let { bpnReferences.resolve(it.reference) }

        return setOfNotNull(legalEntityBpn, siteBpn)
    }

    private fun resolveAddress(reference: BpnReferenceRequest, bpnReferences: BpnReferenceAllocation): LogisticAddressDb? =
        bpnReferences.resolve(reference)?.let { logisticAddressRepository.findByBpn(it) }
}
