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

package org.eclipse.tractusx.bpdm.common.model

import org.eclipse.tractusx.bpdm.common.dto.saas.TypeKeyNameUrlSaas

/**
 * Our representation of a SaaS enum type which consists of a technical key (enum name) and name.
 * It can be converted to SaaS's internal DTO TypeKeyNameUrlSaas.
 */
interface SaasType : NamedType {
    fun getTechnicalKey(): String
}

fun SaasType.toSaasTypeDto() =
    TypeKeyNameUrlSaas(technicalKey = getTechnicalKey(), name = getTypeName())

inline fun <reified T> TypeKeyNameUrlSaas.toSaasType(): T?
        where T : Enum<T>, T : SaasType {
    return technicalKey?.let { enumValueOf<T>(it) }
}
