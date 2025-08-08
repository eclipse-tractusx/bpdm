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

data class ValidatedOptional<T, E: ValidationError>(
    val validatedValue: T?,
    override val errors: Set<E>
): IsValidated<T, E> {

    companion object{
        fun<T, E: ValidationError> success(validValue: T?): ValidatedOptional<T, E> = ValidatedOptional(validValue, emptySet())
        fun<T, E: ValidationError> fail(errors: Collection<E>): ValidatedOptional<T, E> = ValidatedOptional(null, errors.toSet())
        fun<T, E: ValidationError> fail(error: E): ValidatedOptional<T, E> = fail(setOf(error))

        fun <T, E: ValidationError> onEmpty(errors: Collection<E>, initializer: () -> T?): ValidatedOptional<T, E>{
            return if(errors.isEmpty()) success(initializer()) else fail(errors)
        }
    }

    init {
        if(errors.isNotEmpty()) require(validatedValue == null)
    }

    override val validValue: T? by lazy {
        require(errors.isEmpty())
        validatedValue
    }

    fun <R : ValidationError> mapErrors(transform: (E) -> R): ValidatedOptional<T, R> {
        return ValidatedOptional(validatedValue, errors.map(transform).toSet())
    }

}