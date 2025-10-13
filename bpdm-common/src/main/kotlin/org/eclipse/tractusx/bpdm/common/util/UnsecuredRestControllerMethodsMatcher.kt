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

import org.springframework.aop.MethodMatcher
import org.springframework.core.annotation.MergedAnnotations
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.reflect.Method

/**
 * Matches all methods which are in a RestController but are not secured by MethodSecurity-Annotations
 */
class UnsecuredRestControllerMethodsMatcher(
    private val packagePrefix: String
): MethodMatcher {
    override fun matches(method: Method, targetClass: Class<*>): Boolean {
        val methodAnnotations = MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
        val classAnnotations = MergedAnnotations.from(targetClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)

        if(!targetClass.packageName.startsWith(packagePrefix)) return false

        val classIsRestController = classAnnotations.get(RestController::class.java).isPresent
        if(!classIsRestController) return false

        val methodHasRequestMapping = methodAnnotations.get(RequestMapping::class.java).isPresent
                || methodAnnotations.get(GetMapping::class.java).isPresent
                || methodAnnotations.get(PostMapping::class.java).isPresent
                || methodAnnotations.get(PutMapping::class.java).isPresent
                || methodAnnotations.get(DeleteMapping::class.java).isPresent

        if(!methodHasRequestMapping) return false

        val methodHasMethodSecurity = methodAnnotations.get(PreAuthorize::class.java).isPresent
                || methodAnnotations.get(PostAuthorize::class.java).isPresent

        return !methodHasMethodSecurity
    }

    override fun isRuntime(): Boolean = false

    override fun matches(method: Method, targetClass: Class<*>, vararg args: Any?): Boolean = matches(method, targetClass)
}