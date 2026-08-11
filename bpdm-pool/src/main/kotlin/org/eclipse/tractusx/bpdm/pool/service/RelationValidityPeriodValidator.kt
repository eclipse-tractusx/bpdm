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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.pool.entity.RelationTimePeriod
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.springframework.stereotype.Service

/**
 * Checks the validity periods a relation task carries, independently of which kind of business partner it relates.
 */
@Service
class RelationValidityPeriodValidator {

    /**
     * Rejects the given relation when it carries no validity period, when a period ends before it starts, or when two
     * of its periods overlap.
     */
    fun validate(relation: BusinessPartnerRelations) {
        val orderedValidityPeriods = relation.validityPeriods.sortedBy { it.validFrom }

        if (orderedValidityPeriods.isEmpty()) {
            throw BpdmValidationException("Relation validity periods cannot be empty, at least one validity needed.")
        }

        orderedValidityPeriods.first().let { period ->
            if (period.validTo != null && period.validFrom.isAfter(period.validTo)) {
                throw BpdmValidationException("Relation validity period validFrom '${period.validFrom}' cannot be after validTo '${period.validTo}'.")
            }
        }

        val orderedTimePeriods = orderedValidityPeriods.map { RelationTimePeriod.fromUnlimited(it.validFrom, it.validTo) }
        val anyOverlap = orderedTimePeriods.zip(orderedTimePeriods.drop(1))
            .any { (period, nextPeriod) -> period.hasOverlap(nextPeriod) }

        if (anyOverlap) {
            throw BpdmValidationException("Relation validity periods must not overlap.")
        }
    }
}
