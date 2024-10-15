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

package org.eclipse.tractusx.bpdm.common.mapping

import org.eclipse.tractusx.bpdm.common.exception.BpdmValidationErrorException

/**
 * Holds the nullable result of a mapping to type [T]
 *
 * This can either be a successful result holding the mapped value or an invalid mapping holding validation errors
 */
data class NullableMappingResult<T>(
    private val optionalResult: T?,
    val errors: List<ValidationError>
){
    companion object{

        /**
         * Create a nullable mapping result type from a non-nullable mapping result
         */
        fun <T> fromResult(mappingResult: MappingResult<T>): NullableMappingResult<T>{
            return if(mappingResult.isSuccess){
                NullableMappingResult(mappingResult.successfulResult, mappingResult.errors)
            }else
                NullableMappingResult(null, mappingResult.errors)
        }

        /**
         * Create a successful mapping result for a valid null value
         */
        fun <T> ofValidNull(): NullableMappingResult<T>{
            return NullableMappingResult(null, emptyList())
        }
    }

    /**
     * Whether the mapping was a success
     */
    val isSuccess = errors.isEmpty()

    /**
     * On a successful mapping return the resulting value otherwise throw an exception
     */
    val successfulResult by lazy {
        if(!isSuccess) throw BpdmValidationErrorException(errors)

        optionalResult
    }
}
