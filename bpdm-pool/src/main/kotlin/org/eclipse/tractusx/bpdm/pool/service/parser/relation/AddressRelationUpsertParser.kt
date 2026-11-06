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

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.parseResultOf
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.model.parsed.relation.AddressRelationUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.RelationSubject
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.RelationUpsertError
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationRaw
import org.eclipse.tractusx.bpdm.pool.repository.AddressRelationRepository
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.repository.ReasonCodeRepository
import org.springframework.stereotype.Service

/**
 * Resolves a succession between two addresses and checks it against the succession rules.
 */
@Service
class AddressRelationUpsertParser(
    private val addressRepository: LogisticAddressRepository,
    private val reasonCodeRepository: ReasonCodeRepository,
    private val addressRelationRepository: AddressRelationRepository,
    private val validityPeriodValidator: RelationValidityPeriodValidator,
    private val successionValidator: SuccessionRelationValidator
) {

    /**
     * Reports the address succession [relation] states, or every reason it cannot be written.
     */
    fun parse(relation: RelationRaw): ParseResult<AddressRelationUpsertParsed, RelationUpsertError> {
        if (relation.sourceBpn == relation.targetBpn)
            return ParseResult.ofSingleFailure(RelationUpsertError.SelfReference(RelationSubject.Address, relation.sourceBpn))

        val source = addressRepository.findByBpn(relation.sourceBpn)
        val target = addressRepository.findByBpn(relation.targetBpn)
        val reasonCode = relation.reasonCode?.let { reasonCodeRepository.findByTechnicalKey(it) }

        val inputErrors = listOfNotNull(
            RelationUpsertError.SourceNotFound(RelationSubject.Address, relation.sourceBpn).takeIf { source == null },
            RelationUpsertError.TargetNotFound(RelationSubject.Address, relation.targetBpn).takeIf { target == null },
            relation.reasonCode?.takeIf { reasonCode == null }?.let { RelationUpsertError.ReasonCodeNotFound(it) }
        ) + validityPeriodValidator.validate(relation.validityPeriods)
        if (source == null || target == null || inputErrors.isNotEmpty()) return ParseResult.Failure(inputErrors)

        val existingRelation = addressRelationRepository
            .findAll(AddressRelationRepository.byRelation(startAddress = source, endAddress = target, type = AddressRelationTypeV7.IsReplacedBy))
            .singleOrNull()
        val ruleErrors = successionValidator.validate(source, target, relation.validityPeriods, existingRelation)

        return parseResultOf(AddressRelationUpsertParsed(source, target, relation.validityPeriods, reasonCode, existingRelation), ruleErrors)
    }
}
