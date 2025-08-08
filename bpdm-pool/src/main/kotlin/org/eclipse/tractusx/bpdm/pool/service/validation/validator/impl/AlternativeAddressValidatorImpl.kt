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

import org.eclipse.tractusx.bpdm.pool.dto.input.AlternativeAddress
import org.eclipse.tractusx.bpdm.pool.dto.valid.AlternativeAddressValid
import org.eclipse.tractusx.bpdm.pool.dto.validation.Validated
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.AlternativeAddressError
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.*
import org.springframework.stereotype.Service

@Service
class AlternativeAddressValidatorImpl(
    private val postalAddressValidator: PostalAddressValidator,
    private val deliveryServiceTypeValidator: DeliveryServiceTypeValidator,
    private val requiredStringValidator: RequiredStringValidator,
    private val optionalStringValidator: OptionalStringValidator
): AlternativeAddressValidator {

    override fun validate(alternativeAddresses: List<AlternativeAddress>): List<Validated<AlternativeAddressValid, AlternativeAddressError>> {
        val baseAddressContents = postalAddressValidator.validate(alternativeAddresses)
        val deliveryServiceTypes = deliveryServiceTypeValidator.validate(alternativeAddresses.map { it.deliveryServiceType })
        val deliveryServiceNumbers = requiredStringValidator.validate(alternativeAddresses.map { it.deliveryServiceNumber })
        val deliveryServiceQualifiers = optionalStringValidator.validate(alternativeAddresses.map { it.deliveryServiceQualifier })

        return alternativeAddresses.mapIndexed { index, address ->
            val errors = listOf(
                baseAddressContents[index].errors.map(AlternativeAddressError::PostalAddress),
                deliveryServiceTypes[index].errors.map(AlternativeAddressError::DeliveryServiceType),
                deliveryServiceNumbers[index].errors.map(AlternativeAddressError::DeliveryServiceNumber),
                deliveryServiceQualifiers[index].errors.map(AlternativeAddressError::DeliveryServiceQualifier)
            ).flatten()

            Validated.onEmpty(errors) {
                AlternativeAddressValid(
                    baseAddressContents[index].validValue,
                    deliveryServiceTypes[index].validValue,
                    deliveryServiceNumbers[index].validValue,
                    deliveryServiceQualifiers[index].validValue
                )
            }
        }
    }
}