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