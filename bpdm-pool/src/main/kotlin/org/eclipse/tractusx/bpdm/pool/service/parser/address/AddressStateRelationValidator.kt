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
package org.eclipse.tractusx.bpdm.pool.service.parser.address

import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationDb
import org.eclipse.tractusx.bpdm.pool.model.PartnerActivityTimeline
import org.eclipse.tractusx.bpdm.pool.model.error.AddressStateRelationParseError
import org.eclipse.tractusx.bpdm.pool.model.request.LogisticAddressRequest
import org.eclipse.tractusx.bpdm.pool.model.toActivityTimeline
import org.eclipse.tractusx.bpdm.pool.repository.AddressRelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Checks the states an address write states against the successions the address already takes part in.
 */
@Service
class AddressStateRelationValidator(
    private val addressRelationRepository: AddressRelationRepository
) {

    /**
     * Reports, per address content, each succession the states it states would contradict. [ownerBpns] is positional
     * with [contents]: null for an address being created, which takes part in no succession yet.
     */
    @Transactional(readOnly = true)
    fun validate(contents: List<LogisticAddressRequest>, ownerBpns: List<String?>): List<List<AddressStateRelationParseError>> {
        val statedOwnerBpns = ownerBpns.filterNotNull()
        val relations =
            if (statedOwnerBpns.isEmpty()) emptySet()
            else addressRelationRepository.findByStartAddressBpnInOrEndAddressBpnIn(statedOwnerBpns)

        return contents.zip(ownerBpns) { content, bpn ->
            if (bpn == null) return@zip emptyList()
            val activity = content.states.toActivityTimeline()
            relations
                .filter { it.startAddress.bpn == bpn || it.endAddress.bpn == bpn }
                .flatMap { validate(bpn, activity, it) }
        }
    }

    private fun validate(bpn: String, activity: PartnerActivityTimeline, relation: AddressRelationDb): List<AddressStateRelationParseError> =
        relation.validityPeriods.sortedBy { it.validFrom }.flatMap { validityPeriod ->
            val validFrom = validityPeriod.validFrom
            listOfNotNull(
                if (relation.startAddress.bpn == bpn && activity.isRecordedActiveOnceReplacedFrom(validFrom))
                    AddressStateRelationParseError.ReplacedAddressRecordedActive(bpn, relation.endAddress.bpn, validFrom) else null,
                if (relation.endAddress.bpn == bpn && activity.isRecordedInactiveWhenReplacingFrom(validFrom))
                    AddressStateRelationParseError.ReplacingAddressRecordedInactive(bpn, relation.startAddress.bpn, validFrom) else null
            )
        }
}
