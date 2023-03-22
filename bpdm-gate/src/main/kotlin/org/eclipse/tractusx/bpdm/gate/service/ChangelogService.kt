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

package org.eclipse.tractusx.bpdm.gate.service

import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogResponse
import org.eclipse.tractusx.bpdm.gate.dto.response.ErrorInfo
import org.eclipse.tractusx.bpdm.gate.api.model.response.LsaType
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageChangeLogResponse
import org.eclipse.tractusx.bpdm.gate.api.exception.ChangeLogOutputError
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository.Specs.byCreatedAtGreaterThan
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository.Specs.byExternalIdsIn
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository.Specs.byLsaType
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ChangelogService(private val changelogRepository: ChangelogRepository) {

    fun getChangeLogByExternalId(externalIds: Collection<String>, createdAt: Instant?, page: Int, pageSize: Int): PageChangeLogResponse<ChangelogResponse> {

        val spec = Specification.allOf(byExternalIdsIn(externalIds = externalIds), byCreatedAtGreaterThan(createdAt = createdAt))
        val pageable = PageRequest.of(page, pageSize)
        val pageResponse = changelogRepository.findAll(spec, pageable)


        val pageDto = pageResponse.map {
            it.toGateDto()
        }

        val errorInfoList = externalIds.filterNot { id ->
            pageDto.content.any { it.externalId == id }
        }.map {
            ErrorInfo(
                ChangeLogOutputError.ExternalIdNotFound,
                "$it not found",
                "externalId not found"
            )
        }

        return PageChangeLogResponse(
            page = page, totalElements = pageDto.totalElements,
            totalPages = pageDto.totalPages,
            contentSize = pageDto.content.size,
            content = pageDto.content,
            invalidEntries = errorInfoList.size,
            errors = errorInfoList
        )
    }

    fun getChangeLogByLsaType(lsaType: LsaType?, createdAt: Instant?, page: Int, pageSize: Int): PageResponse<ChangelogResponse> {

        val spec = Specification.allOf(byCreatedAtGreaterThan(createdAt = createdAt), byLsaType(lsaType))
        val pageable = PageRequest.of(page, pageSize)
        val pageResponse = changelogRepository.findAll(spec, pageable)

        val pageDto = pageResponse.map {
            it.toGateDto()
        }

        return PageResponse(
            page = page,
            totalElements = pageDto.totalElements,
            totalPages = pageDto.totalPages,
            contentSize = pageDto.content.size,
            content = pageDto.content,
        )
    }
}