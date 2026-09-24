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

package org.eclipse.tractusx.bpdm.pool.service.parser.relation

import org.eclipse.tractusx.bpdm.pool.entity.AddressStateDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityStateDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationTimePeriod
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteStateDb
import org.eclipse.tractusx.bpdm.pool.model.PartnerActivityTimeline
import org.eclipse.tractusx.bpdm.pool.model.RecordedState
import org.eclipse.tractusx.bpdm.pool.model.error.PredecessorRecordedActive
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessionContentParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessorRecordedInactive
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Checks a succession against what its two partners record about being in use: a predecessor still recorded as active
 * from the succession's start date onwards, and a successor recorded as inactive on that date, are both rejected.
 */
@Service
class SuccessionStateValidator {

    /**
     * Reports what the two legal entities' states say against a succession starting on [validFrom].
     */
    fun validate(predecessor: LegalEntityDb, successor: LegalEntityDb, validFrom: LocalDate): List<SuccessionContentParseError> =
        validate(predecessor.bpn, predecessor.states.toTimeline(), successor.bpn, successor.states.toTimeline(), validFrom)

    /**
     * Reports what the two sites' states say against a succession starting on [validFrom].
     */
    fun validate(predecessor: SiteDb, successor: SiteDb, validFrom: LocalDate): List<SuccessionContentParseError> =
        validate(predecessor.bpn, predecessor.states.toTimeline(), successor.bpn, successor.states.toTimeline(), validFrom)

    /**
     * Reports what the two addresses' states say against a succession starting on [validFrom].
     */
    fun validate(predecessor: LogisticAddressDb, successor: LogisticAddressDb, validFrom: LocalDate): List<SuccessionContentParseError> =
        validate(predecessor.bpn, predecessor.states.toTimeline(), successor.bpn, successor.states.toTimeline(), validFrom)

    private fun validate(
        predecessorBpn: String,
        predecessorActivity: PartnerActivityTimeline,
        successorBpn: String,
        successorActivity: PartnerActivityTimeline,
        validFrom: LocalDate
    ): List<SuccessionContentParseError> {
        val errors = mutableListOf<SuccessionContentParseError>()

        if (predecessorActivity.isRecordedActiveWithin(RelationTimePeriod.fromUnlimited(validFrom, null)))
            errors.add(PredecessorRecordedActive(predecessorBpn, validFrom))

        if (successorActivity.isRecordedInactiveWithin(RelationTimePeriod(validFrom, validFrom.plusDays(1))))
            errors.add(SuccessorRecordedInactive(successorBpn, validFrom))

        return errors
    }

    @JvmName("legalEntityStatesToTimeline")
    private fun Collection<LegalEntityStateDb>.toTimeline() =
        PartnerActivityTimeline(map { RecordedState(it.validFrom, it.validTo, it.type) })

    @JvmName("siteStatesToTimeline")
    private fun Collection<SiteStateDb>.toTimeline() =
        PartnerActivityTimeline(map { RecordedState(it.validFrom, it.validTo, it.type) })

    @JvmName("addressStatesToTimeline")
    private fun Collection<AddressStateDb>.toTimeline() =
        PartnerActivityTimeline(map { RecordedState(it.validFrom, it.validTo, it.type) })
}
