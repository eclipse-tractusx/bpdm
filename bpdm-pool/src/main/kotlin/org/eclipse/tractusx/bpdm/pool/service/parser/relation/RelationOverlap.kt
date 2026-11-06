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

package org.eclipse.tractusx.bpdm.pool.service.parser.relation

import org.eclipse.tractusx.bpdm.common.model.BaseEntityWithExplicitTimestamps
import org.eclipse.tractusx.bpdm.pool.entity.RelationTimePeriod
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationValidityPeriodRaw

/**
 * Returns those of [relations], other than [upsertedRelation], that are valid on a date one of [validityPeriods] covers.
 */
internal fun <R : BaseEntityWithExplicitTimestamps> overlappingRelations(
    relations: Collection<R>,
    upsertedRelation: R?,
    validityPeriods: Collection<RelationValidityPeriodRaw>,
    validityPeriodsOf: (R) -> Collection<RelationValidityPeriodDb>
): List<R> =
    relations
        .filterNot { relation -> upsertedRelation?.id == relation.id }
        .filter { relation -> validityPeriods.any { period -> validityPeriodsOf(relation).any { overlaps(period, it) } } }

private fun overlaps(period: RelationValidityPeriodRaw, otherPeriod: RelationValidityPeriodDb): Boolean =
    RelationTimePeriod.fromUnlimited(period.validFrom, period.validTo)
        .hasOverlap(RelationTimePeriod.fromUnlimited(otherPeriod.validFrom, otherPeriod.validTo))
