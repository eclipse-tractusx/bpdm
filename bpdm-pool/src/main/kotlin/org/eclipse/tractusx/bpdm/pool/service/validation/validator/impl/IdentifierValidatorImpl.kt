/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.validation.validator.impl

import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.dto.input.IdentifierIdentity
import org.eclipse.tractusx.bpdm.pool.dto.input.IdentifierWithBpn
import org.eclipse.tractusx.bpdm.pool.dto.valid.IdentifierValid
import org.eclipse.tractusx.bpdm.pool.dto.validation.Validated
import org.eclipse.tractusx.bpdm.pool.dto.validation.ValidationResult
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.IdentifierError
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.*
import org.eclipse.tractusx.bpdm.pool.util.letNonNull
import org.springframework.stereotype.Service

@Service
class IdentifierValidatorImpl(
    private val identifierTypeValidator: IdentifierTypeValidator,
    private val requiredStringValidator: RequiredStringValidator,
    private val optionalStringValidator: OptionalStringValidator,
    private val duplicationCheck: IdentifierDuplicationCheck
): IdentifierValidator {

    override fun validate(identifiers: List<IdentifierWithBpn>, type: IdentifierBusinessPartnerType): List<Validated<IdentifierValid, IdentifierError>> {

        val identifierTypes = identifierTypeValidator.validate(identifiers.map { it.typeKey }, type)
        val identifierValues =  requiredStringValidator.validate(identifiers.map { it.value })
        val issuingBodies = optionalStringValidator.validate(identifiers.map { it.issuingBody })

        val duplicationCheckResults = identifiers.mapIndexed { index, request ->
            letNonNull(identifierTypes[index].validatedValue, identifierValues[index].validatedValue){ idType, idValue ->
                IdentifierIdentity(idValue, idType.technicalKey, request.bpn)
            }
        }
            .letNonNull { duplicationCheck.validate(it, type) }
            .mapIndexed { index, result -> result?.let { ValidationResult.fail(it.errors.map { IdentifierError.Duplicate(index) }) } ?: ValidationResult.success() }

        return identifiers.mapIndexed { index, request ->

            val errors = listOf(
                identifierTypes[index].errors.map { error -> IdentifierError.Type(error, index) },
                identifierValues[index].errors.map{ error -> IdentifierError.Value(error, index) },
                issuingBodies[index].errors.map{ error -> IdentifierError.IssuingService(error, index) },
                duplicationCheckResults[index].errors.map{ IdentifierError.Duplicate(index) },
            ).flatten()

            Validated.onEmpty(errors){
                IdentifierValid(identifierValues[index].validValue, identifierTypes[index].validValue, issuingBodies[index].validValue)
            }
        }
    }
}