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

package org.eclipse.tractusx.bpdm.pool.util

import org.eclipse.tractusx.bpdm.pool.dto.validation.Validated
import org.eclipse.tractusx.bpdm.pool.dto.validation.ValidatedOptional
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.ValidationError

/**
 * @return a list of [Validated] values built from the elements of this list and the other list with the same index using the provided transform function applied to each pair of elements.
 *
 * @exception IllegalArgumentException if both lists are not the same size
 *
 * @see [zip]
 */
fun <A, A_ERROR: R_ERROR, B, B_ERROR: R_ERROR, R, R_ERROR: ValidationError> List<Validated<A, A_ERROR>>.zipValidated(bList: List<Validated<B, B_ERROR>>, transform: (A, B) -> R): List<Validated<R, R_ERROR>>{
    require(this.size == bList.size)

    return mapIndexed { index, a -> Validated.merge(a, bList[index], transform) }
}


/**
 * @return a list of [Validated] elements equal to [this] list but with the [ValidationError] exchanged according to the [transform] function
 */
fun <T, E: ValidationError, R: ValidationError> List<Validated<T, E>>.mapErrors(transform: (E) -> R): List<Validated<T, R>>{
    return map{ it.mapErrors(transform) }
}

/**
 * @return a list of [ValidatedOptional] elements equal to [this] list but with the [ValidationError] exchanged according to the [transform] function
 */
fun <T, E: ValidationError, R: ValidationError> List<ValidatedOptional<T, E>>.mapErrorsOptional(transform: (E) -> R): List<ValidatedOptional<T, R>>{
    return map{ it.mapErrors(transform) }
}

/**
 * @return the result of [transform] applied on [this] list for all valid [Validated] elements.
 * If an element is not valid it will be null in the result
 */
fun <T, E: ValidationError, R> List<Validated<T, E>>.letValid(transform: (List<T>) -> List<R>): List<R?>{
    return mapToNullUnless { it.errors.isEmpty() }.mapNonNull { it.validValue }.letNonNull(transform)
}


/**
 * Applies the given [transform] to the non-null [INPUT] elements of this [List]
 * @return A [List] with each [OUTPUT] element either transformed or NULL, if the corresponding [INPUT] element was NULL
 */
fun <INPUT, OUTPUT> List<INPUT?>.letNonNull(transform: (List<INPUT>) -> List<OUTPUT>): List<OUTPUT?>{
    val result = transform(filterNotNull())
    var resultIndex = -1
    return map { element ->
        if(element == null) return@map null
        resultIndex++
        result[resultIndex]

    }
}

/**
 * Applies the given [transform] to the non-null [INPUT] elements of this [List]
 * @return A [List] with each [OUTPUT] element either transformed or NULL, if the corresponding [INPUT] element was NULL
 */
fun <INPUT, OUTPUT> List<INPUT?>.mapNonNull(transform: (INPUT) -> OUTPUT): List<OUTPUT?>{
    return map { if(it == null) null else transform(it) }
}

/**
 * @return a copy of [this] list in which each element either fulfills the condition of the [discriminator] function or the element is null
 */
fun <T> List<T>.mapToNullUnless(discriminator: (T) -> Boolean): List<T?>{
    return map{ if(!discriminator(it)) null else it }
}

/**
 * @return a copy of [this] nested list in which all elements have been applied to the [transform] function.
 *
 * This function makes sure that the [transform] function is executed only once.
 */
fun <T, R> List<List<T>>.letNested(transform: (List<T>) -> List<R>): List<List<R>>{
    val flatIndexedValues = flatMapIndexed { index, subList -> subList.map { IndexedValue(index, it) } }
    val transformedValues = flatIndexedValues.map { it.value }.let(transform)

    val indexedTransformedValues = flatIndexedValues.map { it.index }.zip(transformedValues){ originalIndex, transformedValue -> IndexedValue(originalIndex, transformedValue) }
    val sortedTransformedValues = indexedTransformedValues.groupBy { it.index }

    return this.mapIndexed { listIndex, list -> sortedTransformedValues[listIndex]?.map { indexedElement -> indexedElement.value } ?: emptyList() }
}

