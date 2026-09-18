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

import org.eclipse.tractusx.bpdm.pool.model.AddressCoverageWrite
import org.eclipse.tractusx.bpdm.pool.model.parsed.BpnReferenceParsed
import org.eclipse.tractusx.bpdm.pool.model.PartnerScriptCodes
import org.eclipse.tractusx.bpdm.pool.model.request.BpnReferenceRequest
import org.eclipse.tractusx.bpdm.pool.model.request.GoldenRecordUpsertRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteUpsertRequest
import org.eclipse.tractusx.bpdm.pool.model.request.UpsertIntent
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Reads what one golden record upsert writes, as script variant coverage sees it.
 *
 * An upsert writes its legal entity, its site and their addresses in several operations, and one address can be
 * written twice when a site's main address is the legal address. No single one of those operations sees the state the
 * task leaves behind, which is why the whole task is stated as one set of writes.
 */
@Service
class GoldenRecordCoverageWriteReader(
    private val referenceResolutionParser: BpnReferenceResolutionParser,
    private val logisticAddressRepository: LogisticAddressRepository
) {

    /**
     * Reports every address [request] writes, with the script codes it will carry and the partners stated on it.
     */
    @Transactional(readOnly = true)
    fun writesOf(request: GoldenRecordUpsertRequest): List<AddressCoverageWrite> {
        val writtenSite = request.recordSite.site?.takeIf { it.intent == UpsertIntent.AlwaysWrite }
        val legalEntityWritten = request.legalEntity.intent == UpsertIntent.AlwaysWrite

        // A site sharing the legal address writes that one address too, with the legal entity's payload, so the
        // address ends up carrying the legal entity's script codes whichever of the two the request writes.
        val legalAddress = request.legalEntity.legalAddress.reference
            .takeIf { legalEntityWritten || writtenSite is SiteUpsertRequest.WithLegalAddressAsMain }
            ?.let { resolveAddress(it) }
        val siteMainAddress = (writtenSite as? SiteUpsertRequest.WithOwnMainAddress)
            ?.mainAddress?.reference
            ?.let { resolveAddress(it) }
        // An additional address is written whenever it is stated, so it needs no intent of its own.
        val additionalAddress = request.additionalAddress?.let { resolveAddress(it.reference) }

        val legalEntityPartner = request.legalEntity
            .takeIf { legalEntityWritten }
            ?.let { PartnerScriptCodes(resolveBpn(it.reference), it.header.scriptVariants.map { variant -> variant.scriptCode }) }
        val sitePartner = writtenSite
            ?.let { PartnerScriptCodes(resolveBpn(it.reference), it.header.scriptVariants.map { variant -> variant.scriptCode }) }

        return listOfNotNull(
            legalAddress?.let {
                AddressCoverageWrite.Rewritten(
                    address = it,
                    partners = listOfNotNull(legalEntityPartner, sitePartner.takeIf { writtenSite is SiteUpsertRequest.WithLegalAddressAsMain }),
                    scriptCodes = request.legalEntity.header.scriptVariants.map { variant -> variant.scriptCode }
                )
            },
            siteMainAddress?.let {
                AddressCoverageWrite.Rewritten(
                    address = it,
                    partners = listOfNotNull(sitePartner),
                    scriptCodes = writtenSite.header.scriptVariants.map { variant -> variant.scriptCode }
                )
            },
            additionalAddress?.let {
                AddressCoverageWrite.Rewritten(
                    address = it,
                    partners = emptyList(),
                    scriptCodes = request.additionalAddress.content.scriptVariants.map { variant -> variant.scriptCode }
                )
            }
        )
    }

    private fun resolveAddress(reference: BpnReferenceRequest): LogisticAddressDb? =
        resolveBpn(reference)?.let { logisticAddressRepository.findByBpn(it) }

    private fun resolveBpn(reference: BpnReferenceRequest): String? =
        (referenceResolutionParser.parse(reference) as? BpnReferenceParsed.Existing)?.bpn
}
