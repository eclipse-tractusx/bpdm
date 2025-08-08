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
import org.eclipse.tractusx.bpdm.pool.dto.input.AddressUpsert
import org.eclipse.tractusx.bpdm.pool.dto.input.Identifier
import org.eclipse.tractusx.bpdm.pool.dto.input.IdentifierWithBpn
import org.eclipse.tractusx.bpdm.pool.dto.valid.AddressValid
import org.eclipse.tractusx.bpdm.pool.dto.valid.IdentifierValid
import org.eclipse.tractusx.bpdm.pool.dto.validation.Validated
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.AddressContentError
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.ExceedLengthError
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.*
import org.eclipse.tractusx.bpdm.pool.util.letNested
import org.eclipse.tractusx.bpdm.pool.util.letNonNull
import org.eclipse.tractusx.bpdm.pool.util.mapErrors
import org.eclipse.tractusx.bpdm.pool.util.zipValidated
import org.springframework.stereotype.Service

@Service
class AddressContentValidatorImpl(
    private val optionalStringValidator: OptionalStringValidator,
    private val identifierValidator: IdentifierValidator,
    private val businessStateValidator: BusinessStateValidator,
    private val confidenceValidator: ConfidenceValidator,
    private val physicalAddressValidator: PhysicalAddressValidator,
    private val alternativeAddressValidator: AlternativeAddressValidator
): AddressContentValidator {

    companion object{
        const val IDENTIFIER_AMOUNT_LIMIT = 100
    }

    override fun validate(requests: List<AddressUpsert>): List<Validated<AddressValid, AddressContentError>> {
        val names = optionalStringValidator.validate(requests.map { it.content.name })

        val identifierLists = validateIdentifierLists(requests)

        val businessStates = requests
            .map { request -> request.content.businessStates }
            .letNested { businessStateValidator.validate(it) }

        val confidences = requests
            .map { it.content.confidenceCriteria }
            .let { confidenceValidator.validate(it) }

        val physicalAddresses = requests
            .map { it.content.physicalAddress }
            .let { physicalAddressValidator.validate(it) }

        val alternativeAddresses = requests
            .map { it.content.alternativeAddress }
            .letNonNull { alternativeAddressValidator.validate(it) }

        return requests.mapIndexed {  index, request ->
            val errors = listOfNotNull(
                names[index].errors.map(AddressContentError::Name),
                identifierLists[index].errors,
                businessStates[index].flatMap { it.errors }.map(AddressContentError::BusinessState),
                confidences[index].errors.map(AddressContentError::Confidence),
                physicalAddresses[index].errors.map(AddressContentError::PhysicalAddress),
                alternativeAddresses[index]?.errors?.map(AddressContentError::AlternativeAddress)
            ).flatten()

            Validated.onEmpty(errors){
                AddressValid(
                    names[index].validValue,
                    identifierLists[index].validValue,
                    businessStates[index].map { it.validValue },
                    confidences[index].validValue,
                    physicalAddresses[index].validValue,
                    alternativeAddresses[index]?.validValue
                )
            }
        }
    }

    private fun validateIdentifierLists(requests: List<AddressUpsert>): List<Validated<List<IdentifierValid>, AddressContentError>>{

        val sizeCheckResults = requests
            .map { it.content.identifiers }
            .map { if(it.size > IDENTIFIER_AMOUNT_LIMIT) Validated.fail(ExceedLengthError(IDENTIFIER_AMOUNT_LIMIT)) else Validated.success<List<Identifier>, ExceedLengthError>(it) }
            .mapErrors(AddressContentError::IdentifierList)

        val identifiersResults = requests
            .map { request -> request.content.identifiers.map { IdentifierWithBpn(it.value, it.typeKey, it.issuingBody, request.bpnA) } }
            .letNested { identifierValidator.validate(it, IdentifierBusinessPartnerType.ADDRESS) }
            .map { idResultList -> idResultList.mapIndexed { index, idResult -> idResult.copy(errors = idResult.errors.map { error -> error.withNewIndex(index) }.toSet()) } }

        val identifierErrors = identifiersResults.map { it.flatMap { it.errors } }
        val aggregatedIdentifierResults = identifiersResults
            .zip(identifierErrors){ result, errors -> Validated.onEmpty(errors){ result.map { it.validValue } } }
            .mapErrors(AddressContentError::Identifier)

        return sizeCheckResults.zipValidated(aggregatedIdentifierResults){ _, identifierResult -> identifierResult  }
    }
}