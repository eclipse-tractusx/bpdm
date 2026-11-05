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

package org.eclipse.tractusx.bpdm.pool.model.parsed.relation

import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.ReasonCodeDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteRelationDb
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationValidityPeriodRaw

/**
 * A relation between two distinct business partners of one kind that may be written: its validity periods are present,
 * none ends before it starts and none overlaps another, and every rule of its relation type held at parse time.
 */
sealed interface RelationUpsertParsed {
    val validityPeriods: List<RelationValidityPeriodRaw>
    val reasonCode: ReasonCodeDb?
}

data class LegalEntityRelationUpsertParsed(
    val type: LegalEntityRelationTypeV7,
    val source: LegalEntityDb,
    val target: LegalEntityDb,
    override val validityPeriods: List<RelationValidityPeriodRaw>,
    override val reasonCode: ReasonCodeDb?,
    val existingRelation: RelationDb?
) : RelationUpsertParsed

data class SiteRelationUpsertParsed(
    val source: SiteDb,
    val target: SiteDb,
    override val validityPeriods: List<RelationValidityPeriodRaw>,
    override val reasonCode: ReasonCodeDb?,
    val existingRelation: SiteRelationDb?
) : RelationUpsertParsed

data class AddressRelationUpsertParsed(
    val source: LogisticAddressDb,
    val target: LogisticAddressDb,
    override val validityPeriods: List<RelationValidityPeriodRaw>,
    override val reasonCode: ReasonCodeDb?,
    val existingRelation: AddressRelationDb?
) : RelationUpsertParsed
