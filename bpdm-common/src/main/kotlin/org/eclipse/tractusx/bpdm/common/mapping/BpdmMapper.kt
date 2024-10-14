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

import org.eclipse.tractusx.bpdm.common.mapping.ValidationContext.Companion.onIndex

/**
 * Offers base logic to validate and map from one type to another
 */
interface BpdmMapper<FROM_TYPE, TO_TYPE> {

    /**
     * Try to map given non-null [valueToMap] within the given [context]
     *
     *  Return a [NullableMappingResult] containing either the result or validation errors
     */
    fun map(valueToMap: FROM_TYPE, context: ValidationContext = ValidationContext.NoContext): MappingResult<TO_TYPE>

    fun map(listToMap: List<FROM_TYPE>, context: ValidationContext = ValidationContext.NoContext): MappingResult<List<TO_TYPE>>{
        val entryResults = listToMap.mapIndexed { index, entry -> map(entry, context.onIndex(index)) }
        val entryErrors = entryResults.flatMap { it.errors }
        return MappingResult.invalidOnError(entryErrors){ entryResults.map { it.successfulResult } }
    }

    /**
     * Check whether a non-null [valueToMap] of the [FROM_TYPE] should be treated as a valid null mapping result
     *
     */
    fun checkTreatAsNull(valueToMap: FROM_TYPE) = false

    fun checkTreatAsNull(listToMap: List<FROM_TYPE>) = false

    /**
     * Try to map given nullable [valueToMap] within the given [context] whereby null is an acceptable value
     *
     *  Return a [NullableMappingResult] containing either the result or validation errors
     */
    fun mapValidNull(valueToMap: FROM_TYPE?, context: ValidationContext = ValidationContext.NoContext): NullableMappingResult<TO_TYPE>{
        return valueToMap
            ?.let { if(checkTreatAsNull(it)) null else it }
            ?.let {  NullableMappingResult.fromResult(map(it, context)) }
            ?: NullableMappingResult.ofValidNull()
    }

    fun mapValidNull(listToMap: List<FROM_TYPE>?, context: ValidationContext = ValidationContext.NoContext): NullableMappingResult<List<TO_TYPE>>{
        return listToMap
            ?.let { if(checkTreatAsNull(it)) null else it }
            ?.let { NullableMappingResult.fromResult(map(it, context)) }
            ?: NullableMappingResult.ofValidNull()
    }

    /**
     * Try to map given nullable [valueToMap] within the given [context] whereby null is not acceptable
     *
     *  Return a [NullableMappingResult] containing either the result or validation errors
     */
    fun mapInvalidNull(valueToMap: FROM_TYPE?, context: ValidationContext = ValidationContext.NoContext): MappingResult<TO_TYPE>{
        return valueToMap?.let { map(it, context) } ?: MappingResult.ofInvalidNull()
    }

    fun mapInvalidNull(listToMap: List<FROM_TYPE>?, context: ValidationContext = ValidationContext.NoContext): MappingResult<List<TO_TYPE>>{
        return listToMap?.let { map(it, context) } ?: MappingResult.ofInvalidNull()
    }

}