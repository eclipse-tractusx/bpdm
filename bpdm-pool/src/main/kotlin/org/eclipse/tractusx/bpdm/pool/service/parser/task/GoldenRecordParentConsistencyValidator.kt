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
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.error.AdditionalAddressNotInRequestLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.error.GoldenRecordUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.error.MembershipSiteNotInLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.error.SiteNotInRequestLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.parsed.BpnReferenceParsed
import org.eclipse.tractusx.bpdm.pool.model.request.BpnReferenceRequest
import org.eclipse.tractusx.bpdm.pool.model.request.GoldenRecordUpsertRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteBpnParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The rule that the site, the additional address and the sites stated as sharing the record address all belong to the
 * legal entity the upsert states. A reference that names no persisted business partner yet is not judged here: the
 * operation that writes it decides it.
 *
 * A legal entity that is itself only about to be created owns no site yet, which the rule needs no case of its own
 * for: its BPN resolves to none, so every site that does resolve belongs to a different legal entity.
 */
@Service
class GoldenRecordParentConsistencyValidator(
    private val referenceResolutionParser: BpnReferenceResolutionParser,
    private val siteBpnParser: SiteBpnParser,
    private val addressBpnParser: AddressBpnParser
) {

    /**
     * Reports every violation of the rule in [request].
     */
    @Transactional(readOnly = true)
    fun validate(request: GoldenRecordUpsertRequest): List<GoldenRecordUpsertParseError> {
        val legalEntityBpn = resolveBpn(request.legalEntity.reference)
        val siteBpn = request.site?.let { resolveBpn(it.reference) }
        // A membership stated without a site is rejected on its own, so judging its entries here would fault the same
        // statement twice.
        val membershipBpns = request.site
            ?.let { request.addressSiteMembership.mapNotNull { stated -> resolveBpn(stated.reference) }.distinct() }
            ?: emptyList()
        val addressBpn = request.additionalAddress?.let { resolveBpn(it.reference) }

        val sitesByBpn = resolveSitesPresent(listOfNotNull(siteBpn) + membershipBpns)
        val address = addressBpn?.let { resolveAddressIfPresent(it) }

        return listOfNotNull(
            siteBpn?.let { sitesByBpn[it] }
                ?.takeIf { it.legalEntity.bpn != legalEntityBpn }
                ?.let { SiteNotInRequestLegalEntity(it.bpn, legalEntityBpn) },
            address?.takeIf { it.legalEntity!!.bpn != legalEntityBpn }?.let { AdditionalAddressNotInRequestLegalEntity(it.bpn, legalEntityBpn) }
        ) + membershipBpns.mapNotNull { bpn ->
            sitesByBpn[bpn]
                ?.takeIf { it.legalEntity.bpn != legalEntityBpn }
                ?.let { MembershipSiteNotInLegalEntity(it.bpn, legalEntityBpn) }
        }
    }

    private fun resolveBpn(reference: BpnReferenceRequest): String? =
        (referenceResolutionParser.parse(reference) as? BpnReferenceParsed.Existing)?.bpn

    private fun resolveSitesPresent(siteBpns: List<String>): Map<String, SiteDb> {
        val distinct = siteBpns.distinct()
        return distinct.zip(siteBpnParser.parse(distinct))
            .mapNotNull { (bpn, result) -> (result as? ParseResult.Success)?.let { bpn to it.parsed } }
            .toMap()
    }

    private fun resolveAddressIfPresent(addressBpn: String): LogisticAddressDb? =
        when (val result = addressBpnParser.parse(listOf(addressBpn)).single()) {
            is ParseResult.Success -> result.parsed
            is ParseResult.Failure -> null
        }
}
