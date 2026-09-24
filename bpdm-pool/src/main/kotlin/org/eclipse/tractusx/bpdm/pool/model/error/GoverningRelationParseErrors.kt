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

package org.eclipse.tractusx.bpdm.pool.model.error

import java.time.LocalDate

sealed interface OwnershipUpsertParseError

sealed interface DataManagementUpsertParseError

/**
 * What a relation in which one legal entity governs another can be faulted for, whether it is an ownership or a data
 * management relation.
 */
sealed interface GoverningRelationParseError : OwnershipUpsertParseError, DataManagementUpsertParseError

/**
 * What the validity periods a relation states can be faulted for, independently of the partners it relates.
 */
sealed interface RelationValidityPeriodParseError : GoverningRelationParseError, AlternativeHeadquarterUpsertParseError

data class GovernedPartnerNotFound(val bpn: String) : GoverningRelationParseError

data class GoverningPartnerNotFound(val bpn: String) : GoverningRelationParseError

data class GovernedByItself(val bpn: String) : GoverningRelationParseError

data class RelationReasonCodeNotFound(val reasonCode: String) : GoverningRelationParseError, AlternativeHeadquarterUpsertParseError

data class GoverningPartnerRecordedInactive(val bpn: String, val validFrom: LocalDate, val validTo: LocalDate?) :
    GoverningRelationParseError

data object RelationValidityPeriodsMissing : RelationValidityPeriodParseError

data class RelationValidityPeriodEndsBeforeStart(val validFrom: LocalDate, val validTo: LocalDate) :
    RelationValidityPeriodParseError

data object RelationValidityPeriodsOverlap : RelationValidityPeriodParseError

data class OwnedAlreadyOwned(val ownedBpn: String, val existingOwnerBpn: String) : OwnershipUpsertParseError

data class OwnershipCycle(val ownedBpn: String, val ownerBpn: String) : OwnershipUpsertParseError

data class OwnershipPartnerIsAlternativeHeadquarter(val bpn: String, val mainBpn: String) : OwnershipUpsertParseError

data class ManagerNotDataSpaceParticipant(val bpn: String) : DataManagementUpsertParseError

data class ManagedAlreadyManaged(val managedBpn: String, val existingManagerBpns: List<String>) : DataManagementUpsertParseError

data class ManagedIsManager(val bpn: String) : DataManagementUpsertParseError

data class ManagerIsManaged(val bpn: String) : DataManagementUpsertParseError

data class PastValidityPeriodAdded(val validFrom: LocalDate) : DataManagementUpsertParseError

data class PastValidityPeriodEndMoved(val validFrom: LocalDate) : DataManagementUpsertParseError

data class PastValidityPeriodRemoved(val validFrom: LocalDate) : DataManagementUpsertParseError
