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

import org.eclipse.tractusx.bpdm.pool.entity.RelationTimePeriod
import org.eclipse.tractusx.bpdm.pool.model.parsed.relation.RelationUpsertError
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationValidityPeriodRaw
import org.springframework.stereotype.Service

/**
 * Checks the validity periods of a relation, independently of which kind of business partner it relates.
 */
@Service
class RelationValidityPeriodValidator {

    /**
     * Reports [validityPeriods] that are empty, of which one ends before it starts, or of which two overlap.
     */
    fun validate(validityPeriods: List<RelationValidityPeriodRaw>): List<RelationUpsertError> {
        if (validityPeriods.isEmpty()) return listOf(RelationUpsertError.ValidityPeriodsEmpty)
        val orderedValidityPeriods = validityPeriods.sortedBy { it.validFrom }

        val inverted = orderedValidityPeriods.firstNotNullOfOrNull { period ->
            period.validTo
                ?.takeIf { period.validFrom.isAfter(it) }
                ?.let { RelationUpsertError.ValidityPeriodInverted(period.validFrom, it) }
        }

        val orderedTimePeriods = orderedValidityPeriods.map { RelationTimePeriod.fromUnlimited(it.validFrom, it.validTo) }
        val overlap = RelationUpsertError.ValidityPeriodsOverlap
            .takeIf { orderedTimePeriods.zipWithNext().any { (period, nextPeriod) -> period.hasOverlap(nextPeriod) } }

        return listOfNotNull(inverted, overlap)
    }
}
