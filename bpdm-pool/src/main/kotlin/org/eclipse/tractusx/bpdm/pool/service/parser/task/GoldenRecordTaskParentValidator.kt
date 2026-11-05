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

import org.eclipse.tractusx.bpdm.common.model.parsedOrNull
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.GoldenRecordTaskError
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.GoldenRecordTaskReferencesParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.SiteUpsertRaw
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteBpnParser
import org.springframework.stereotype.Service

/**
 * Reports an existing site, additional address or adopted site main address of a golden record task that belongs to
 * another legal entity than the task's.
 */
@Service
class GoldenRecordTaskParentValidator(
    private val siteBpnParser: SiteBpnParser,
    private val addressBpnParser: AddressBpnParser
) {

    /**
     * Reports every business partner of [goldenRecord] whose stored legal entity is not the one the task states.
     */
    fun validate(goldenRecord: GoldenRecordRaw, references: GoldenRecordTaskReferencesParsed): List<GoldenRecordTaskError> {
        val legalEntityBpn = references.bpnOf(goldenRecord.legalEntity.reference)
        val siteBpn = goldenRecord.site?.let { references.bpnOf(it.reference) }
        val site = siteBpn?.let { siteBpnParser.parse(listOf(it)).single().parsedOrNull() }
        // A new site whose main address already exists adopts that address and, with it, the address's legal entity.
        val adoptedMainAddress = (goldenRecord.site as? SiteUpsertRaw.WithOwnMainAddress)
            ?.takeIf { siteBpn == null }
            ?.let { references.bpnOf(it.mainAddress.reference) }
            ?.let { addressBpnParser.parse(listOf(it)).single().parsedOrNull() }
        val additionalAddress = goldenRecord.additionalAddress
            ?.let { references.bpnOf(it.reference) }
            ?.let { addressBpnParser.parse(listOf(it)).single().parsedOrNull() }

        return listOfNotNull(
            GoldenRecordTaskError.SiteNotInLegalEntity.takeIf { site != null && site.legalEntity.bpn != legalEntityBpn },
            GoldenRecordTaskError.SiteMainAddressNotInLegalEntity
                .takeIf { adoptedMainAddress != null && adoptedMainAddress.legalEntity?.bpn != legalEntityBpn },
            GoldenRecordTaskError.AdditionalAddressNotInLegalEntity
                .takeIf { additionalAddress != null && additionalAddress.legalEntity?.bpn != legalEntityBpn }
        )
    }
}
