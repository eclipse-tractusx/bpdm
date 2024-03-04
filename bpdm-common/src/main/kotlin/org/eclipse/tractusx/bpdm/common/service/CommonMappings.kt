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

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerbose
import org.eclipse.tractusx.bpdm.common.model.NamedType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

fun <T : NamedType> T.toDto(): TypeKeyNameVerbose<T> {
    return TypeKeyNameVerbose(this, getTypeName())
}

fun LanguageCode.toDto(): TypeKeyNameVerbose<LanguageCode> {
    return TypeKeyNameVerbose(this, getName())
}

fun CountryCode.toDto(): TypeKeyNameVerbose<CountryCode> {
    return TypeKeyNameVerbose(this, getName())
}

fun PaginationRequest.toPageRequest(sort: Sort = Sort.unsorted()) =
    PageRequest.of(page, size, sort)

fun <T, R> Page<T>.toPageDto(contentMapper: (T) -> R): PageDto<R> =
    PageDto(
        page = number,
        totalElements = totalElements,
        totalPages = totalPages,
        contentSize = content.size,
        content = content.map(contentMapper)
    )
