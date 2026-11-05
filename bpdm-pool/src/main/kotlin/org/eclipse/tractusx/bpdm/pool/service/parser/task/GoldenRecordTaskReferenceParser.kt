/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.pool.model.parsed.task.GoldenRecordTaskReferencesParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.task.BpnReferenceKind
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.SiteUpsertRaw
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.springframework.stereotype.Service

/**
 * Looks up the BPNs the request identifiers of a golden record task are mapped to.
 */
@Service
class GoldenRecordTaskReferenceParser(
    private val bpnRequestIdentifierRepository: BpnRequestIdentifierRepository
) {

    /**
     * Returns the mapping of every request identifier [goldenRecord] states that already has a BPN.
     */
    fun parse(goldenRecord: GoldenRecordRaw): GoldenRecordTaskReferencesParsed {
        val references = with(goldenRecord) {
            listOfNotNull(
                legalEntity.reference,
                legalEntity.legalAddress.reference,
                site?.reference,
                (site as? SiteUpsertRaw.WithOwnMainAddress)?.mainAddress?.reference,
                additionalAddress?.reference
            ) + additionalSites.map { it.reference }
        }
        val requestIdentifiers = references.filter { it.kind == BpnReferenceKind.RequestIdentifier }.mapNotNull { it.value }.distinct()
        val mappings = bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(requestIdentifiers)

        return GoldenRecordTaskReferencesParsed(mappings.associate { it.requestIdentifier to it.bpn })
    }
}
