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

package org.eclipse.tractusx.bpdm.common.service

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

/**
 * This is a generic JsonDeserializer that works with Kotlin data classes containing the annotation JsonUnwrapped.
 * The new object is initialized via the primary constructor.
 */
class DataClassUnwrappedJsonDeserializer : JsonDeserializer<Any?>(), ContextualDeserializer {
    // We need a ContextualDeserializer to find out the destination type.
    override fun createContextual(ctxt: DeserializationContext, property: BeanProperty?): JsonDeserializer<*> {
        val javaType: JavaType = ctxt.contextualType
            ?: throw IllegalStateException("ContextualType is missing from DeserializationContext")
        return DataClassUnwrappedJsonDeserializerForType(javaType)
    }

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): Any? {
        throw IllegalStateException("DataClassUnwrappedJsonDeserializer.deserialize() can not be used directly")
    }
}

private class DataClassUnwrappedJsonDeserializerForType(destinationJavaType: JavaType) : JsonDeserializer<Any>() {
    private val destinationClass: KClass<out Any>
    private val primaryConstructor: KFunction<Any>
    private val constructorParameters: List<ConstructorParameter>

    init {
        this.destinationClass = destinationJavaType.rawClass.kotlin
        this.primaryConstructor = destinationClass.primaryConstructor
            ?: throw IllegalStateException("Primary constructor required for '$destinationClass'")

        // Annotation @field:JsonUnwrapped is stored on the Java field, not the constructor parameter.
        val propertiesByName = destinationClass.memberProperties.associateBy { it.name }

        this.constructorParameters = primaryConstructor.parameters.map { param ->
            val name = param.name
                ?: throw IllegalStateException("Some primary constructor parameter of '$destinationClass' doesn't have a name")
            val type = param.type
            val jsonUnwrapped = propertiesByName[name]?.javaField?.getAnnotation(JsonUnwrapped::class.java) != null
            ConstructorParameter(name, type, jsonUnwrapped)
        }
    }

    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): Any {
        val rootNode = parser.codec.readTree<JsonNode>(parser)

        val constructorValues = constructorParameters.map { param ->
            val jacksonType = ctxt.typeFactory.constructType(param.type.javaType)
            val node = if (param.jsonUnwrapped) rootNode else rootNode.get(param.name)
            val value = readTreeAsValue(ctxt, node, jacksonType)
            if (value == null && !param.type.isMarkedNullable)
                throw IllegalArgumentException("Field '${param.name}' of '$destinationClass' is required")
            value
        }

        return primaryConstructor.call(args = constructorValues.toTypedArray())
    }

    private fun readTreeAsValue(ctxt: DeserializationContext, node: JsonNode?, javaType: JavaType) : Any? =
        if (node == null || node.isNull) null
        else ctxt.readTreeAsValue<Any?>(node, javaType)
}

private data class ConstructorParameter(
    val name: String,
    val type: KType,
    val jsonUnwrapped: Boolean,
)
