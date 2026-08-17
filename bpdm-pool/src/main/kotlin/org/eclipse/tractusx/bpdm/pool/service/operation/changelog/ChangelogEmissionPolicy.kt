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

package org.eclipse.tractusx.bpdm.pool.service.operation.changelog

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.model.ChangelogRecord

/**
 * The changelog entries a transaction writes and the recorded entries they superseded.
 */
data class ChangelogEmissionPlan(
    val emitted: List<ChangelogRecord>,
    val suppressed: List<ChangelogRecord>
)

/**
 * Decides which recorded changelog entries are written and in which order.
 */
object ChangelogEmissionPolicy {

    /**
     * Collapses the recorded entries to one per business partner, a creation outranking an update, and orders them
     * legal entity before site before address.
     */
    fun plan(recorded: List<ChangelogRecord>): ChangelogEmissionPlan {
        val winningPositions = recorded.withIndex()
            .groupBy { (_, record) -> record.bpn }
            .map { (_, recordsOfPartner) -> winnerIn(recordsOfPartner).index }
            .toSet()

        val (winners, superseded) = recorded.withIndex().partition { it.index in winningPositions }

        return ChangelogEmissionPlan(
            emitted = winners.sortedWith(compareBy({ rank(it.value.businessPartnerType) }, { it.index })).map { it.value },
            suppressed = superseded.map { it.value }
        )
    }

    private fun winnerIn(recordsOfPartner: List<IndexedValue<ChangelogRecord>>): IndexedValue<ChangelogRecord> =
        recordsOfPartner.firstOrNull { (_, record) -> record.changelogType == ChangelogType.CREATE }
            ?: recordsOfPartner.first()

    private fun rank(businessPartnerType: BusinessPartnerType): Int =
        when (businessPartnerType) {
            BusinessPartnerType.LEGAL_ENTITY -> 0
            BusinessPartnerType.SITE -> 1
            BusinessPartnerType.ADDRESS -> 2
            BusinessPartnerType.GENERIC -> 3
        }
}
