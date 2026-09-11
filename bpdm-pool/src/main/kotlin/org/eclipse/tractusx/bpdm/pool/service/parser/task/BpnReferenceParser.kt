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

import org.eclipse.tractusx.bpdm.pool.model.parsed.ResolvedBpnReferences
import org.eclipse.tractusx.bpdm.pool.model.request.BpnReferenceKind
import org.eclipse.tractusx.bpdm.pool.model.request.BpnReferenceRequest
import org.eclipse.tractusx.bpdm.pool.model.request.GoldenRecordUpsertRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteUpsertRequest
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Resolves the request identifiers a batch of golden record upserts states to the BPNs they already name.
 */
@Service
class BpnReferenceParser(
    private val bpnRequestIdentifierRepository: BpnRequestIdentifierRepository
) {

    /** The BPNs the batch's request identifiers stand for, looked up in one query. */
    @Transactional(readOnly = true)
    fun parse(requests: List<GoldenRecordUpsertRequest>): ResolvedBpnReferences {
        val usedRequestIdentifiers = statedReferences(requests)
            .filter { it.type == BpnReferenceKind.RequestIdentifier }
            .mapNotNull { it.value }
            .distinct()

        val mappings = bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(usedRequestIdentifiers)

        return ResolvedBpnReferences(mappings.associate { it.requestIdentifier to it.bpn })
    }

    private fun statedReferences(requests: List<GoldenRecordUpsertRequest>): List<BpnReferenceRequest> {
        val references = requests.map { it.legalEntity.reference } +
                requests.map { it.legalEntity.legalAddress.reference } +
                requests.mapNotNull { it.site?.reference } +
                requests.mapNotNull { (it.site as? SiteUpsertRequest.WithOwnMainAddress)?.mainAddress?.reference } +
                requests.mapNotNull { it.additionalAddress?.reference } +
                requests.flatMap { request -> request.addressSiteMembership.map { it.reference } }

        return references.filter { it.value != null && it.type != null }
    }
}
