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

package org.eclipse.tractusx.bpdm.common.util

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class CollectionUtilsKtTest {

    @Test
    fun mergeMapsWithCollectionInValue() {

        val key1 = "Task1"
        val key2 = "Task2"
        val key3 = "Task3"

        val firstMap = mapOf(
            key1 to listOf("value1", "value2"),
            key2 to listOf("value3", "value2", "value1")
        )
        val secondMap = mapOf(
            key1 to listOf("value4", "value5"),
            key3 to listOf("value4", "value1")
        )
        val thirdMap = mapOf(
            key1 to listOf("value6"),
            key3 to listOf("value7")
        )

        val result = mergeMapsWithCollectionInValue(firstMap, secondMap, thirdMap)
        Assertions.assertThat(result[key1]).containsExactlyInAnyOrder("value1", "value2", "value4", "value5", "value6")
        Assertions.assertThat(result[key2]).containsExactlyInAnyOrder("value3", "value2", "value1")
        Assertions.assertThat(result[key3]).containsExactlyInAnyOrder("value4", "value1", "value7")
    }

}