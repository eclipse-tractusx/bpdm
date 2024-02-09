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

import org.springdoc.core.annotations.ParameterObject
import org.springframework.core.MethodParameter
import org.springframework.format.support.DefaultFormattingConversionService
import org.springframework.web.service.invoker.HttpRequestValues
import org.springframework.web.service.invoker.HttpServiceArgumentResolver
import kotlin.reflect.full.memberProperties

/**
 * This handles complex parameters with ParameterObject annotation in API controller methods, e.g. PaginationRequest.
 * For executing an API call we want all the parameter-object's properties added to the request as individual query parameters.
 * This doesn't work out of the box, so we added this HttpServiceArgumentResolver that can be added to HttpServiceProxyFactory.
 */
class ParameterObjectArgumentResolver : HttpServiceArgumentResolver {
    private val conversionService = DefaultFormattingConversionService()
    override fun resolve(argument: Any?, parameter: MethodParameter, requestValues: HttpRequestValues.Builder): Boolean {
        val annot = parameter.getParameterAnnotation(ParameterObject::class.java)
        if (annot != null && argument != null) {
            for (memberProperty in argument.javaClass.kotlin.memberProperties) {
                val propName = memberProperty.name
                val propValue = memberProperty.get(argument)
                val propValueString = when (propValue) {
                    is String -> propValue
                    else -> conversionService.convert(propValue, String::class.java)
                }
                if (propValue != null) {
                    requestValues.addRequestParameter(propName, propValueString)
                }
            }
            return true
        }
        return false
    }
}