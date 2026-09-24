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

package org.eclipse.tractusx.bpdm.pool.model

import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.pool.entity.AddressStateDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityStateDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationTimePeriod
import org.eclipse.tractusx.bpdm.pool.entity.SiteStateDb
import java.time.LocalDateTime

/**
 * What a business partner's states record about it being in use, over the days a rule asks about.
 *
 * The two questions are answered independently rather than resolved against each other, because nothing stops a
 * partner from recording both over the same time: a day recorded active and inactive at once answers yes to both, and
 * is therefore rejected by whichever rule asks. A day neither covers is recorded as nothing and is never a violation.
 */
data class PartnerActivityTimeline(
    private val recordedStates: List<RecordedState>
) {
    /**
     * Whether the partner is recorded as active on any day of [span].
     */
    fun isRecordedActiveWithin(span: RelationTimePeriod): Boolean =
        isRecordedWithin(BusinessStateType.ACTIVE, span)

    /**
     * Whether the partner is recorded as inactive on any day of [span].
     */
    fun isRecordedInactiveWithin(span: RelationTimePeriod): Boolean =
        isRecordedWithin(BusinessStateType.INACTIVE, span)

    private fun isRecordedWithin(type: BusinessStateType, span: RelationTimePeriod): Boolean {
        val spanStart = span.validFrom.atStartOfDay()
        val spanEnd = span.validTo.atStartOfDay()

        return recordedStates.any { state ->
            state.type == type &&
                    (state.validFrom == null || state.validFrom < spanEnd) &&
                    (state.validTo == null || state.validTo > spanStart)
        }
    }
}

data class RecordedState(
    val validFrom: LocalDateTime?,
    val validTo: LocalDateTime?,
    val type: BusinessStateType
)

/**
 * Returns what these legal entity states record about the legal entity being in use.
 */
@JvmName("legalEntityStatesToActivityTimeline")
fun Collection<LegalEntityStateDb>.toActivityTimeline() =
    PartnerActivityTimeline(map { RecordedState(it.validFrom, it.validTo, it.type) })

/**
 * Returns what these site states record about the site being in use.
 */
@JvmName("siteStatesToActivityTimeline")
fun Collection<SiteStateDb>.toActivityTimeline() =
    PartnerActivityTimeline(map { RecordedState(it.validFrom, it.validTo, it.type) })

/**
 * Returns what these address states record about the address being in use.
 */
@JvmName("addressStatesToActivityTimeline")
fun Collection<AddressStateDb>.toActivityTimeline() =
    PartnerActivityTimeline(map { RecordedState(it.validFrom, it.validTo, it.type) })
