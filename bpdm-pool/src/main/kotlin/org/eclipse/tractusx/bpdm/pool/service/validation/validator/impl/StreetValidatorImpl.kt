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

import org.eclipse.tractusx.bpdm.pool.dto.input.Street
import org.eclipse.tractusx.bpdm.pool.dto.valid.StreetValid
import org.eclipse.tractusx.bpdm.pool.dto.validation.Validated
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.StreetError
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.StreetError.*
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.OptionalStringValidator
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.StreetValidator
import org.eclipse.tractusx.bpdm.pool.util.mapErrorsOptional
import org.springframework.stereotype.Service

@Service
class StreetValidatorImpl(
    private val optionalStringValidator: OptionalStringValidator
): StreetValidator {

    override fun validate(streets: List<Street>): List<Validated<StreetValid, StreetError>> {

        val names = optionalStringValidator.validate(streets.map { it.name }).mapErrorsOptional(::Name)
        val houseNumbers = optionalStringValidator.validate(streets.map { it.houseNumber }).mapErrorsOptional(::HouseNumber)
        val houseNumberSupplements = optionalStringValidator.validate(streets.map { it.houseNumberSupplement }).mapErrorsOptional(::HouseNumberSupplement)
        val milestones = optionalStringValidator.validate(streets.map { it.milestone }).mapErrorsOptional(::Milestone)
        val directions = optionalStringValidator.validate(streets.map { it.direction }).mapErrorsOptional(::Direction)
        val namePrefixes = optionalStringValidator.validate(streets.map { it.namePrefix }).mapErrorsOptional(::NamePrefix)
        val additionalNamePrefixes = optionalStringValidator.validate(streets.map { it.additionalNamePrefix }).mapErrorsOptional(::AdditionalNamePrefix)
        val nameSuffixes = optionalStringValidator.validate(streets.map { it.nameSuffix }).mapErrorsOptional(::NameSuffix)
        val additionalNameSuffixes = optionalStringValidator.validate(streets.map { it.additionalNameSuffix }).mapErrorsOptional(::AdditionalNameSuffix)

        return streets.mapIndexed{ index, street ->
            val errors = listOf(
                names[index].errors,
                houseNumbers[index].errors,
                houseNumberSupplements[index].errors,
                milestones[index].errors,
                directions[index].errors,
                namePrefixes[index].errors,
                additionalNamePrefixes[index].errors,
                nameSuffixes[index].errors,
                additionalNameSuffixes[index].errors,
            ).flatten()

            Validated.onEmpty(errors){
                StreetValid(
                    name = names[index].validValue,
                    houseNumber = houseNumbers[index].validValue,
                    houseNumberSupplement = houseNumberSupplements[index].validValue,
                    milestone = milestones[index].validValue,
                    direction = directions[index].validValue,
                    namePrefix = namePrefixes[index].validValue,
                    additionalNamePrefix = additionalNamePrefixes[index].validValue,
                    nameSuffix = nameSuffixes[index].validValue,
                    additionalNameSuffix = additionalNameSuffixes[index].validValue
                )
            }
        }
    }

}