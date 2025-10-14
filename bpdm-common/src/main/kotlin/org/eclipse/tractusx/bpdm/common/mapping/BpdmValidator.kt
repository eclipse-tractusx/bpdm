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

package org.eclipse.tractusx.bpdm.common.mapping

import org.eclipse.tractusx.bpdm.common.exception.BpdmValidationErrorException
import org.eclipse.tractusx.bpdm.common.mapping.ValidationContext.Companion.onIndex

/**
 * Offers base logic to validate values
 */
interface BpdmValidator<T> {

    /**
     * The list of [BpdmValidation] that will be applied to values given to this validator
     */
    val validations: List<BpdmValidation<T>>

    /**
     * Validate given [value] within given [context] and return any found [ValidationError] in a list
     */
    fun validate(value: T, context: ValidationContext = ValidationContext.NoContext): List<ValidationError>{
        return validations.mapNotNull { it.validate(value, context) }
    }

    /**
     * Validate given [value] and throw exception if any error is found
     */
    fun assert(value: T, context: ValidationContext = ValidationContext.NoContext){
        val errors = validate(value, context)
        if(errors.isNotEmpty()) throw BpdmValidationErrorException(errors)
    }

    fun assert(listOfValues: List<T>, context: ValidationContext = ValidationContext.NoContext){
        val errors  = listOfValues.flatMapIndexed { index, value -> validate(value, context.onIndex(index)) }
        if(errors.isNotEmpty()) throw BpdmValidationErrorException(errors)
    }
}