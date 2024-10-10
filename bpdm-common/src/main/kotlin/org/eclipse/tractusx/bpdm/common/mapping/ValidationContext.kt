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

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

data class ValidationContext(
    val classType: KClass<*>?,
    val objectId: String,
    val propertyPath: List<KProperty<*>>,
) {
    companion object {
        val NoContext = ValidationContext(null, "", emptyList())

        fun fromRoot(classType: KClass<*>?, objectId: String) = ValidationContext(classType, objectId, emptyList())

        fun ValidationContext.onProperty(property: KProperty<*>) = copy(propertyPath = propertyPath + property)
    }

    val prefix = "Class ${classType?.simpleName} with Id $objectId on property ${propertyPath.joinToString()}: "
}
