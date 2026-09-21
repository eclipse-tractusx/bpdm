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

package org.eclipse.tractusx.bpdm.pool.service.parser.legalentity

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityOwnershipParseError
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AlternativeHeadquarterValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The rules that govern writing the ultimate-owner flag onto a legal entity that already exists.
 *
 * Update-only: a legal entity being created carries no relations yet, so neither rule can be broken by one. The
 * relation writes that can break them enforce them on their own side.
 */
@Service
class LegalEntityOwnershipValidator(
    private val ultimateOwnerUniquenessValidator: UltimateOwnerUniquenessValidator,
    private val alternativeHeadquarterValidator: AlternativeHeadquarterValidator
) {

    /**
     * Reports, per entry, every ownership rule the stated flag would break, positional with [targets] and
     * [requestedFlags]: an unresolved target or a flag left unstated by the request yields none.
     */
    @Transactional(readOnly = true)
    fun validate(targets: List<LegalEntityDb?>, requestedFlags: List<Boolean?>): List<List<LegalEntityOwnershipParseError>> {
        val uniquenessViolations = ultimateOwnerUniquenessValidator.validate(targets, requestedFlags)
        val alternativeViolations = alternativeHeadquarterValidator.validate(targets, requestedFlags)

        return uniquenessViolations.zip(alternativeViolations) { uniqueness, alternative -> uniqueness + alternative }
    }
}
