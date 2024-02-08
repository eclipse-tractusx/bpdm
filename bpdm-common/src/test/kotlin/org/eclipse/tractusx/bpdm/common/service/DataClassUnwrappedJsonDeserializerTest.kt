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

package org.eclipse.tractusx.bpdm.common.service

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class DataClassUnwrappedJsonDeserializerTest {

    private val objectMapper: ObjectMapper = buildObjectMapper()

    @Test
    fun `test standard cases`() {
        testSerializeAndDeserialize(MainDto("a", 11, DetailDto("A", 23)))
        testSerializeAndDeserialize(MainDto("a", null, DetailDto("A", 23)))
        testSerializeAndDeserialize(MainDto("a", null, DetailDto(null, 23)))

        testSerializeAndDeserialize(WrapperDto(MainDto("a", null, DetailDto(null, 23)), "extra"))
        testSerializeAndDeserialize(WrapperDto(MainDto("a", null, DetailDto(null, 23)), null))
    }

    @Test
    fun `test complex object`() {
        val d1 = DetailDto("A", 1)
        val d2 = DetailDto(null, 2)
        val complexDto = ComplexDto(
            d1, Pair("first", 2),
            listOf("a", "", "c"),
            mapOf(Pair("d1", listOf(d1)), Pair("d2", listOf(d2)), Pair("all", listOf(d1, d2)))
        )
        testSerializeAndDeserialize(complexDto)
    }

    @Test
    fun `test error handling`() {
        // serialized MainDto
        val strOkay = """
            {"key": "mykey", "detail2": 42}
            """.trimIndent()
        val objOkay = objectMapper.readValue(strOkay, MainDto::class.java)
        assertThat(objOkay).isEqualTo(MainDto("mykey", null, DetailDto(null, 42)))

        // serialized MainDto
        val strNullField = """
            {"key": null, "detail2": 42}
            """.trimIndent()
        // IllegalArgumentException because "key" must not be null!
        assertThatIllegalArgumentException().isThrownBy {
            objectMapper.readValue(strNullField, MainDto::class.java)
        }

        // serialized MainDto
        val strMissingField = """
            {"detail2": 42}
            """.trimIndent()
        // IllegalArgumentException because "key" must not be null!
        assertThatIllegalArgumentException().isThrownBy {
            objectMapper.readValue(strMissingField, MainDto::class.java)
        }

        // serialized MainNonNullDto
        val strMissingNestedField = """
            {"detail1": null, "detail2": 42, "key": "mykey"}
        """.trimIndent()
        // MissingKotlinParameterException because "detail1" in DetailNonNullDto must not be null!
        assertThatExceptionOfType(MissingKotlinParameterException::class.java).isThrownBy {
            objectMapper.readValue(strMissingNestedField, MainNonNullDto::class.java)
        }
    }

    @Test
    fun `test limitations with null JsonUnwrapped property`() {
        // @JsonUnwrapped property is null
        val objOrg = MainDto("b", 42, null)
        val objRestored = serializeAndDeserialize(objOrg)
        // Deserialization of @JsonUnwrapped "details" property doesn't return null, but an "empty" DetailDto object!
        assertThat(objRestored).isEqualTo(
            MainDto(
                key = objOrg.key,
                optional = objOrg.optional,
                details = DetailDto(        // !!! it should be null, but we get an "empty" DetailDto
                    detail1 = null,
                    detail2 = 0
                )
            )
        )
    }

    @Test
    fun `test limitations with nested null int attribute`() {
        // serialized MainDto
        val str = """
            {"key": "mykey", "detail1": "ABC", "detail2": null}
            """.trimIndent()
        val obj = objectMapper.readValue(str, MainDto::class.java)
        // Standard deserializer (not DataClassUnwrappedJsonDeserializer) writes 0 into "detail2" (non-nullable) instead of throwing an exception
        assertThat(obj).isEqualTo(
            MainDto(
                key = "mykey",
                optional = null,
                details = DetailDto(
                    detail1 = "ABC",
                    detail2 = 0         // !!! it should be null, but field is not nullable
                )
            )
        )
    }

    @Test
    fun `test limitations with top level null int attribute`() {
        // serialized DetailDto
        val str = """
            {"detail1": "ABC", "detail2": null}
            """.trimIndent()
        val obj = objectMapper.readValue(str, DetailDto::class.java)
        // Standard deserializer writes 0 into "detail2" (non-nullable) instead of throwing an exception
        assertThat(obj).isEqualTo(
            DetailDto(
                detail1 = "ABC",
                detail2 = 0         // !!! it should be null, but field is not nullable
            )
        )
    }

    @Test
    fun `test standard error handling with top level null string attribute`() {
        // serialized DetailNonNullDto
        val str = """
            {"detail1": null, "detail2": 42}
            """.trimIndent()
        // Standard deserializer throws an exception because "detail1" is non-nullable and there is no default value for String
        assertThatExceptionOfType(MissingKotlinParameterException::class.java).isThrownBy {
            objectMapper.readValue(str, DetailNonNullDto::class.java)
        }
    }

    private fun buildObjectMapper() = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(KotlinModule.Builder().build())

    private inline fun <reified T> testSerializeAndDeserialize(obj: T) {
        assertThat(serializeAndDeserialize(obj)).isEqualTo(obj)
    }

    private inline fun <reified T> serializeAndDeserialize(obj: T): T {
        val objString = objectMapper.writeValueAsString(obj)
        return objectMapper.readValue(objString, T::class.java)
    }
}

@JsonDeserialize(using = DataClassUnwrappedJsonDeserializer::class)
data class WrapperDto(
    @field:JsonUnwrapped
    val dto: MainDto,
    val extra: String?,
)

@JsonDeserialize(using = DataClassUnwrappedJsonDeserializer::class)
data class MainDto(
    val key: String,
    val optional: Int?,
    @field:JsonUnwrapped
    val details: DetailDto?
)

@JsonDeserialize(using = DataClassUnwrappedJsonDeserializer::class)
data class ComplexDto(
    @field:JsonUnwrapped
    val details: DetailDto,
    val pair: Pair<String, Int>,
    val list: Collection<String>,
    val map: Map<String, Collection<DetailDto>>,
)

@JsonDeserialize(using = DataClassUnwrappedJsonDeserializer::class)
data class MainNonNullDto(
    val key: String,
    @field:JsonUnwrapped
    val detail: DetailNonNullDto,
)

data class DetailDto(
    val detail1: String?,
    val detail2: Int,
)

data class DetailNonNullDto(
    val detail1: String,
    val detail2: Int
)
