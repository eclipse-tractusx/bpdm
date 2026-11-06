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

import org.eclipse.tractusx.bpdm.pool.model.raw.relation.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.parseResultOf
import org.eclipse.tractusx.bpdm.pool.model.parsed.relation.LegalEntityRelationUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.RelationSubject
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.RelationUpsertError
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationType
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.ReasonCodeRepository
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service

/**
 * Resolves a relation between two legal entities and checks it against the rules of its relation type.
 */
@Service
class LegalEntityRelationUpsertParser(
    private val legalEntityRepository: LegalEntityRepository,
    private val reasonCodeRepository: ReasonCodeRepository,
    private val relationRepository: RelationRepository,
    private val validityPeriodValidator: RelationValidityPeriodValidator,
    private val ownershipValidator: OwnershipRelationValidator,
    private val alternativeHeadquarterValidator: AlternativeHeadquarterRelationValidator,
    private val managementValidator: ManagementRelationValidator,
    private val successionValidator: SuccessionRelationValidator
) {

    /**
     * Reports the legal entity relation [relation] states, or every reason it cannot be written.
     */
    fun parse(relation: RelationRaw): ParseResult<LegalEntityRelationUpsertParsed, RelationUpsertError> {
        if (relation.sourceBpn == relation.targetBpn)
            return ParseResult.ofSingleFailure(RelationUpsertError.SelfReference(RelationSubject.LegalEntity, relation.sourceBpn))

        val source = legalEntityRepository.findByBpnIgnoreCase(relation.sourceBpn)
        val target = legalEntityRepository.findByBpnIgnoreCase(relation.targetBpn)
        val reasonCode = relation.reasonCode?.let { reasonCodeRepository.findByTechnicalKey(it) }

        val inputErrors = listOfNotNull(
            RelationUpsertError.SourceNotFound(RelationSubject.LegalEntity, relation.sourceBpn).takeIf { source == null },
            RelationUpsertError.TargetNotFound(RelationSubject.LegalEntity, relation.targetBpn).takeIf { target == null },
            relation.reasonCode?.takeIf { reasonCode == null }?.let { RelationUpsertError.ReasonCodeNotFound(it) }
        ) + validityPeriodValidator.validate(relation.validityPeriods)
        if (source == null || target == null || inputErrors.isNotEmpty()) return ParseResult.Failure(inputErrors)

        if (source.bpn == target.bpn)
            return ParseResult.ofSingleFailure(RelationUpsertError.SelfReference(RelationSubject.LegalEntity, source.bpn))

        val type = toLegalEntityRelationType(relation.type)
        val existingRelation = relationRepository.findAll(RelationRepository.byRelation(startNode = source, endNode = target, type = type)).singleOrNull()
        val validityPeriods = relation.validityPeriods

        val ruleErrors = when (type) {
            LegalEntityRelationType.IsOwnedBy -> ownershipValidator.validate(source, target, validityPeriods, existingRelation)
            LegalEntityRelationType.IsAlternativeHeadquarterFor -> alternativeHeadquarterValidator.validate(source, target, validityPeriods, existingRelation)
            LegalEntityRelationType.IsManagedBy -> managementValidator.validate(source, target, validityPeriods, existingRelation)
            LegalEntityRelationType.IsReplacedBy -> successionValidator.validate(source, target, validityPeriods, existingRelation)
        }

        return parseResultOf(LegalEntityRelationUpsertParsed(type, source, target, validityPeriods, reasonCode, existingRelation), ruleErrors)
    }

    private fun toLegalEntityRelationType(type: RelationType): LegalEntityRelationType =
        when (type) {
            RelationType.IsAlternativeHeadquarterFor -> LegalEntityRelationType.IsAlternativeHeadquarterFor
            RelationType.IsManagedBy -> LegalEntityRelationType.IsManagedBy
            RelationType.IsOwnedBy -> LegalEntityRelationType.IsOwnedBy
            RelationType.IsReplacedBy -> LegalEntityRelationType.IsReplacedBy
        }
}
