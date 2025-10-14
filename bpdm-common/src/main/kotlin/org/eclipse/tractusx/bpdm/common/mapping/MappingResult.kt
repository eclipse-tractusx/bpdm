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

/**
 * Holds the non-null result of a mapping to type [T]
 *
 * This can either be a successful result holding the mapped value or an invalid mapping holding validation errors
 */
data class MappingResult<T>(
    private val optionalResult: T?,
    override val errors: List<ValidationError>
): HasMappingResult<T>{
    companion object{

        /**
         * Create a mapping result based on a given [optionalResult], adding the given [errorsIfNull]
         */
        fun <T> errorOnNull(optionalResult: T?, errorsIfNull: () -> List<ValidationError>): MappingResult<T>{
            return MappingResult(optionalResult, optionalResult?.let { emptyList() } ?: errorsIfNull())
        }

        /**
         * Create an invalid mapping result if the given list of [errors] is not empty, otherwise create a successful mapping result with the given value [resultOnNoError]
         */
        fun <T> invalidOnError(errors: List<ValidationError>, resultOnNoError: () -> T): MappingResult<T>{
            return MappingResult(if(errors.isEmpty()) resultOnNoError() else null, errors)
        }

        /**
         * Create a mapping result for an encountered invalid  null value
         */
        fun <T> ofInvalidNull(context: ValidationContext = ValidationContext.NoContext): MappingResult<T>{
            return MappingResult(null, listOf(ValidationError(CommonValidationErrorCodes.IsNull.name, CommonValidationErrorCodes.IsNull.description,null, context)))
        }
    }

    init {
        if(optionalResult == null)
            require(errors.isNotEmpty())

        if(errors.isNotEmpty())
            require(optionalResult == null)
    }

    /**
     * Whether the mapping was a success
     */
    override val isSuccess = errors.isEmpty()

    /**
     * On a successful mapping return the resulting value otherwise throw an exception
     */
    override val successfulResult by lazy {
        if(!isSuccess) throw BpdmValidationErrorException(errors)

        optionalResult!!
    }

    /**
     * On a successful mapping return the resulting value otherwise return  null
     */
    val successfulResultOrNull = if (isSuccess) optionalResult else null
}