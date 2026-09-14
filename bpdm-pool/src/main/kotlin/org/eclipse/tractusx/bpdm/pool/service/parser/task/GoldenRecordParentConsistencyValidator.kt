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
import org.eclipse.tractusx.bpdm.pool.model.BpnReferenceAllocation
import org.eclipse.tractusx.bpdm.pool.model.error.AdditionalAddressNotInRequestLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.error.GoldenRecordUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SiteNotInRequestLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.request.GoldenRecordUpsertRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteBpnParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The rule that the site and additional address an upsert states belong to the legal entity it states. A reference that
 * names no persisted business partner yet is not judged here: the operation that writes it decides it.
 */
@Service
class GoldenRecordParentConsistencyValidator(
    private val siteBpnParser: SiteBpnParser,
    private val addressBpnParser: AddressBpnParser
) {

    /**
     * Reports every violation of the rule in [request].
     */
    @Transactional(readOnly = true)
    fun validate(request: GoldenRecordUpsertRequest, bpnReferences: BpnReferenceAllocation): List<GoldenRecordUpsertParseError> {
        val legalEntityBpn = bpnReferences.resolve(request.legalEntity.reference)
        val siteBpn = request.site?.let { bpnReferences.resolve(it.reference) }
        val addressBpn = request.additionalAddress?.let { bpnReferences.resolve(it.reference) }

        val site = siteBpn?.let { resolveSiteIfPresent(it) }
        val address = addressBpn?.let { resolveAddressIfPresent(it) }

        return listOfNotNull(
            site?.takeIf { it.legalEntity.bpn != legalEntityBpn }?.let { SiteNotInRequestLegalEntity(it.bpn, legalEntityBpn) },
            address?.takeIf { it.legalEntity!!.bpn != legalEntityBpn }?.let { AdditionalAddressNotInRequestLegalEntity(it.bpn, legalEntityBpn) }
        )
    }

    private fun resolveSiteIfPresent(siteBpn: String): SiteDb? =
        when (val result = siteBpnParser.parse(listOf(siteBpn)).single()) {
            is ParseResult.Success -> result.parsed
            is ParseResult.Failure -> null
        }

    private fun resolveAddressIfPresent(addressBpn: String): LogisticAddressDb? =
        when (val result = addressBpnParser.parse(listOf(addressBpn)).single()) {
            is ParseResult.Success -> result.parsed
            is ParseResult.Failure -> null
        }
}
