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

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.model.NamedType
import org.eclipse.tractusx.bpdm.common.model.NamedUrlType

fun <T : NamedUrlType> T.toDto(): TypeKeyNameUrlDto<T> {
    return TypeKeyNameUrlDto(this, getTypeName(), getUrl())
}

fun <T : NamedType> T.toDto(): TypeKeyNameVerboseDto<T> {
    return TypeKeyNameVerboseDto(this, getTypeName())
}

fun LanguageCode.toDto(): TypeKeyNameVerboseDto<LanguageCode> {
    return TypeKeyNameVerboseDto(this, getName())
}

fun CountryCode.toDto(): TypeKeyNameVerboseDto<CountryCode> {
    return TypeKeyNameVerboseDto(this, getName())
}
