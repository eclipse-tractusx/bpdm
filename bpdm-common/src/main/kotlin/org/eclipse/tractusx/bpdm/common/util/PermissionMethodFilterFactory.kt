/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

import org.springdoc.core.filters.OpenApiMethodFilter
import org.springframework.security.access.prepost.PreAuthorize
import java.lang.reflect.Method

/**
 * Creates OpenAPI method filters that restrict a documentation group to the endpoints a set of permissions grants.
 */
class PermissionMethodFilterFactory(
    private val permissionProperties: Any
) {

    /**
     * Creates a filter accepting exactly those endpoints whose required permission is one of the given permissions.
     */
    fun grantedBy(permissions: Set<String>): OpenApiMethodFilter =
        OpenApiMethodFilter { method -> requiredPermission(method) in permissions }

    /**
     * Returns the permission an endpoint requires, or null if it declares none or one this factory cannot resolve.
     */
    fun requiredPermission(method: Method): String? {
        val expression = method.getAnnotation(PreAuthorize::class.java)?.value ?: return null
        val getterName = PERMISSION_GETTER.find(expression)?.groupValues?.get(1) ?: return null
        return runCatching { permissionProperties.javaClass.getMethod(getterName).invoke(permissionProperties) }
            .getOrNull() as? String
    }

    companion object {
        /**
         * Kotlin interpolates the permission constants into the annotation at compile time, so what survives into the
         * class file is the SpEL bean reference they expand to, for example
         * `hasAuthority(@'bpdm.security.permissions-…PermissionConfigProperties'.getReadPartner())`.
         */
        private val PERMISSION_GETTER = Regex("""hasAuthority\(@'[^']+'\.(get\w+)\(\)\)""")
    }
}
