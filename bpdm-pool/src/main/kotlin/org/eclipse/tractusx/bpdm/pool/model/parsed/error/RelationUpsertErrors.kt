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

package org.eclipse.tractusx.bpdm.pool.model.parsed.error

import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationType
import java.time.LocalDate

/**
 * A reason a relation between two business partners cannot be written.
 */
sealed interface RelationUpsertError {

    data class UnsupportedRelation(val sourceBpn: String, val targetBpn: String, val type: RelationType) : RelationUpsertError

    data class SelfReference(val subject: RelationSubject, val bpn: String) : RelationUpsertError

    data class SourceNotFound(val subject: RelationSubject, val bpn: String) : RelationUpsertError

    data class TargetNotFound(val subject: RelationSubject, val bpn: String) : RelationUpsertError

    data class ReasonCodeNotFound(val technicalKey: String) : RelationUpsertError

    data object ValidityPeriodsEmpty : RelationUpsertError

    data class ValidityPeriodInverted(val validFrom: LocalDate, val validTo: LocalDate) : RelationUpsertError

    data object ValidityPeriodsOverlap : RelationUpsertError

    data class MultipleOwners(val ownedBpn: String, val ownerBpn: String, val existingOwnerBpn: String) : RelationUpsertError

    data class OwnershipCycle(val ownedBpn: String, val ownerBpn: String) : RelationUpsertError

    data class MultipleUltimateOwners(val ownedBpn: String, val ownerBpn: String, val flaggedBpns: List<String>) : RelationUpsertError

    data class OwnershipOfAlternative(val legalEntityBpn: String, val mainBpn: String) : RelationUpsertError

    data class AlternativeAlreadyMain(val alternativeBpn: String, val mainBpn: String, val otherAlternativeBpn: String) : RelationUpsertError

    data class AlternativeAlreadyAlternative(val alternativeBpn: String, val mainBpn: String, val otherMainBpn: String) : RelationUpsertError

    data class MainAlreadyAlternativeTo(val mainBpn: String, val alternativeBpn: String, val otherMainBpn: String) : RelationUpsertError

    data class MainAlreadyAlternative(val mainBpn: String, val alternativeBpn: String, val otherMainBpn: String) : RelationUpsertError

    data class ReversedAlternative(val alternativeBpn: String, val mainBpn: String) : RelationUpsertError

    data class AlternativeInOwnership(val alternativeBpn: String, val isOwned: Boolean, val otherBpn: String) : RelationUpsertError

    data class AlternativeFlaggedUltimate(val alternativeBpn: String) : RelationUpsertError

    data class ManagerNotParticipant(val managerBpn: String) : RelationUpsertError

    data class MultipleManagers(val managedBpn: String, val managerBpns: List<String>) : RelationUpsertError

    data class ManagedIsManager(val managedBpn: String) : RelationUpsertError

    data class ManagerIsManaged(val managerBpn: String) : RelationUpsertError

    data class PastValidityPeriodAdded(val validFrom: LocalDate) : RelationUpsertError

    data class PastValidityPeriodLimited(val validFrom: LocalDate) : RelationUpsertError

    data class PastValidityPeriodDeleted(val validFrom: LocalDate) : RelationUpsertError

    data class MultipleSuccessors(
        val subject: RelationSubject,
        val predecessorBpn: String,
        val successorBpn: String,
        val existingSuccessorBpn: String
    ) : RelationUpsertError

    data class SuccessionCycle(val subject: RelationSubject, val predecessorBpn: String, val successorBpn: String) : RelationUpsertError

    data class SuccessionAcrossLegalEntities(
        val subject: RelationSubject,
        val predecessorBpn: String,
        val successorBpn: String,
        val predecessorLegalEntityBpn: String,
        val successorLegalEntityBpn: String
    ) : RelationUpsertError
}

/**
 * The kind of business partner a relation connects.
 */
enum class RelationSubject {
    LegalEntity,
    Site,
    Address
}
