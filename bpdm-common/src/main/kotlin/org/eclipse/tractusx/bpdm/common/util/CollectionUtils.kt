/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.common.util

fun <T> MutableCollection<T>.replace(elements: Collection<T>) {
    clear()
    addAll(elements)
}


/**
 * Copy overlapping elements by index from [elements] to [this] collection by applying the [copyFunction].
 * Remove remaining elements in the original collection and add additional [elements] from given collection
 */
fun <T> MutableCollection<T>.copyAndSync(elements: Collection<T>, copyFunction: (T, T) -> T) {
    // copy the overlap of the two collections
    zip(elements).forEach { (fromState, toState) -> copyFunction(fromState, toState) }

    val sizeDifference = size - elements.size
    if (sizeDifference > 0) {
        //Remove the remaining elements from the original collection
        drop(elements.size).forEach { remove(it) }
    } else {
        //Add the additional elements to the original collection
        addAll(elements.drop(size))
    }
}

fun <T> Collection<T>.findDuplicates(): Set<T> =
    this.groupBy { it }
        .filter { it.value.size > 1 }
        .keys

/**
 * Merge Maps with collections as values.
 * The collections with the same key in the different maps are concatenated
 */
fun <KEY, VALUE> mergeMapsWithCollectionInValue(
    vararg inputMaps: Map<out KEY, Collection<VALUE>>
): Map<KEY, Collection<VALUE>> {
    return inputMaps
        .map { inputMap ->
            inputMap.flatMap { (key, values) -> values.map { value -> Pair(key, value) } }
        }
        .reduce { entries1, entries2 -> entries1 + entries2 }
        .groupBy({ it.first }, { it.second })
}