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
import org.eclipse.tractusx.bpdm.pool.model.error.RelationValidityPeriodEndsBeforeStart
import org.eclipse.tractusx.bpdm.pool.model.error.RelationValidityPeriodParseError
import org.eclipse.tractusx.bpdm.pool.model.error.RelationValidityPeriodsMissing
import org.eclipse.tractusx.bpdm.pool.model.error.RelationValidityPeriodsOverlap
import org.eclipse.tractusx.bpdm.pool.model.request.RelationValidityPeriodRequest
import org.springframework.stereotype.Service

/**
 * Checks the validity periods a relation states, independently of the partners it relates.
 */
@Service
class RelationValidityPeriodsValidator {

    /**
     * Reports a relation stating no validity period, every period ending before it starts, and periods overlapping one
     * another.
     */
    fun validate(validityPeriods: List<RelationValidityPeriodRequest>): List<RelationValidityPeriodParseError> {
        if (validityPeriods.isEmpty()) return listOf(RelationValidityPeriodsMissing)

        val invertedPeriodErrors = validityPeriods
            .filter { it.validTo != null && it.validTo < it.validFrom }
            .map { RelationValidityPeriodEndsBeforeStart(it.validFrom, it.validTo!!) }

        val orderedTimePeriods = validityPeriods
            .sortedBy { it.validFrom }
            .map { RelationTimePeriod.fromUnlimited(it.validFrom, it.validTo) }
        val anyOverlap = orderedTimePeriods.zipWithNext().any { (period, nextPeriod) -> period.hasOverlap(nextPeriod) }

        return if (anyOverlap) invertedPeriodErrors + RelationValidityPeriodsOverlap else invertedPeriodErrors
    }
}
