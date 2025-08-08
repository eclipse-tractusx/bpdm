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

package org.eclipse.tractusx.bpdm.pool.dto.validation

import org.eclipse.tractusx.bpdm.pool.dto.validation.error.ValidationError


data class Validated<T, E: ValidationError>(
    val validatedValue: T?,
    override val errors: Set<E>
): IsValidatedRequired<T, E>{

    companion object{
        fun<T, E: ValidationError> success(validValue: T): Validated<T, E> = Validated(validValue, emptySet())
        fun<T, E: ValidationError> fail(errors: Collection<E>): Validated<T, E> = Validated(null, errors.toSet())
        fun<T, E: ValidationError> fail(error: E): Validated<T, E> = fail(setOf(error))

        fun<A, A_ERROR: R_ERROR, B, B_ERROR: R_ERROR, R, R_ERROR: ValidationError> merge(a: Validated<A, A_ERROR>, b: Validated<B, B_ERROR>, transform: (A, B) -> R): Validated<R, R_ERROR>{
            val mergedErrors = a.errors + b.errors
            return if(mergedErrors.isEmpty()) success(transform(a.validValue, b.validValue)) else fail(mergedErrors)
        }

        fun<T, E: R_ERROR, R_ERROR: ValidationError> fromOptional(a: ValidatedOptional<T, E>, isMissingError: R_ERROR): Validated<T, R_ERROR>{
            if(a.errors.isNotEmpty()) return fail(a.errors)
            return a.validatedValue?.let { success(it) } ?: fail(isMissingError)
        }

        fun <T, E: ValidationError> onEmpty(errors: Collection<E>, initializer: () -> T): Validated<T, E>{
            return if(errors.isEmpty()) success(initializer()) else fail(errors)
        }
    }

    init {
        if(validatedValue == null) require(errors.isNotEmpty()) else require(errors.isEmpty())
    }

    override val validValue: T by lazy {
        require(errors.isEmpty())
        require(validatedValue != null)
        validatedValue
    }

    fun <R: ValidationError> mapErrors(transform: (E) -> R): Validated<T, R>{
        return Validated(validatedValue, errors.map(transform).toSet())
    }
}





