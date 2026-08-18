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

package org.eclipse.tractusx.bpdm.pool.service.parser.address

import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.isValidOn
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeHeadquarterCannotOwnUltimately
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Validates that an alternative headquarter cannot carry the ultimate-owner flag.
 * An entity is considered an alternative if it is the source (startNode) in an IsAlternativeHeadquarterFor relation
 * that is valid today.
 */
@Service
class AlternativeHeadquarterValidator(
    private val relationRepository: RelationRepository
) {

    /**
     * Checks if a legal entity is an alternative headquarter today and if it's being set to ownershipUltimate = true.
     * Returns a violation if both conditions are true.
     */
    @Transactional(readOnly = true)
    fun validateFlagOnAlternative(target: LegalEntityDb?, requestedFlag: Boolean?): List<AlternativeHeadquarterCannotOwnUltimately> {
        if (target == null || requestedFlag != true) {
            return emptyList()
        }

        val today = LocalDate.now()
        val alternativeRelations = relationRepository.findByTypeAndStartNode(LegalEntityRelationType.IsAlternativeHeadquarterFor, target)
        val validToday = alternativeRelations.any { it.isValidOn(today) }

        return if (validToday) {
            listOf(AlternativeHeadquarterCannotOwnUltimately(target.bpn))
        } else {
            emptyList()
        }
    }

    /**
     * Validates each target-flag pair in batch: checks if each legal entity is an alternative headquarter today
     * and if it's being set to ownershipUltimate = true. Returns violations for each entry.
     */
    @Transactional(readOnly = true)
    fun validate(targets: List<LegalEntityDb?>, requestedFlags: List<Boolean?>): List<List<AlternativeHeadquarterCannotOwnUltimately>> {
        require(targets.size == requestedFlags.size) { "targets and requestedFlags must be positionally aligned" }
        return targets.zip(requestedFlags).map { (target, flag) -> validateFlagOnAlternative(target, flag) }
    }
}