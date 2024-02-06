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

import org.springframework.boot.autoconfigure.condition.ConditionOutcome
import org.springframework.boot.autoconfigure.condition.SpringBootCondition
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.annotation.MergedAnnotation
import org.springframework.core.type.AnnotatedTypeMetadata
import kotlin.reflect.KClass


@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention
@MustBeDocumented
@Conditional(OnConditionalOnEnablingProperty::class)
annotation class ConditionalOnBoundProperty(
    val prefix: String,
    val propertiesClass: KClass<out HasEnablingProperty>,
    val havingValue: Boolean
)

interface HasEnablingProperty {
    val enabled: Boolean
}

class OnConditionalOnEnablingProperty : SpringBootCondition() {
    companion object {
        private const val MISSING_PROPERTY = "Property is missing in environment and thus treated as not enabled."
    }

    override fun getMatchOutcome(context: ConditionContext?, metadata: AnnotatedTypeMetadata?): ConditionOutcome {
        val mergedAnnotation: MergedAnnotation<ConditionalOnBoundProperty> =
            metadata!!.annotations.get(ConditionalOnBoundProperty::class.java)
        val prefix = mergedAnnotation.getString(ConditionalOnBoundProperty::prefix.name)
        val targetClass = mergedAnnotation.getClass(ConditionalOnBoundProperty::propertiesClass.name)
        val shouldBeEnabled = mergedAnnotation.getBoolean(ConditionalOnBoundProperty::havingValue.name)
        if (!HasEnablingProperty::class.java.isAssignableFrom(targetClass)) {
            throw RuntimeException("Target type does not implement the ${HasEnablingProperty::class.simpleName} interface.")
        }
        val bean = Binder.get(context!!.environment).bind(prefix, targetClass).orElse(null)
            ?: return if (shouldBeEnabled)
                ConditionOutcome.noMatch(MISSING_PROPERTY)
            else
                ConditionOutcome.match(MISSING_PROPERTY)

        val props: HasEnablingProperty = bean as HasEnablingProperty
        return if (props.enabled == shouldBeEnabled) {
            ConditionOutcome.match()
        } else {
            ConditionOutcome.noMatch("Property not enabled")
        }
    }
}
