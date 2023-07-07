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

import org.eclipse.tractusx.bpdm.common.model.OutputInputEnum
import org.eclipse.tractusx.bpdm.gate.api.exception.ChangeLogOutputError
import org.eclipse.tractusx.bpdm.gate.api.model.LsaType
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageChangeLogResponse
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository.Specs.byCreatedAtGreaterThan
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository.Specs.byExternalIdsIn
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository.Specs.byLsaTypes
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository.Specs.byOutputInputEnum
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ChangelogService(private val changelogRepository: ChangelogRepository) {

    fun getChangeLogEntries(
        externalIds: Set<String>?,
        lsaTypes: Set<LsaType>?,
        createdAt: Instant?,
        outputInputEnum: OutputInputEnum?,
        page: Int,
        pageSize: Int
    ): PageChangeLogResponse<ChangelogGateDto> {

        val nonNullExternalIds = externalIds ?: emptySet()

        val spec = Specification.allOf(
            byExternalIdsIn(externalIds = nonNullExternalIds),
            byCreatedAtGreaterThan(createdAt = createdAt),
            byLsaTypes(lsaTypes),
            byOutputInputEnum(outputInputEnum)
        )

        val pageable = PageRequest.of(page, pageSize)
        val pageResponse = changelogRepository.findAll(spec, pageable)
        val setDistinctList = changelogRepository.findExternalIdsInListDistinct(nonNullExternalIds)


        val pageDto = pageResponse.map {
            it.toGateDto()
        }

        val errorList = (nonNullExternalIds - setDistinctList).map {
            ErrorInfo(
                ChangeLogOutputError.ExternalIdNotFound,
                "$it not found",
                it
            )
        }


        return PageChangeLogResponse(
            page = page, totalElements = pageDto.totalElements,
            totalPages = pageDto.totalPages,
            contentSize = pageDto.content.size,
            content = pageDto.content,
            invalidEntries = errorList.size,
            errors = errorList
        )
    }

}