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
import org.eclipse.tractusx.bpdm.gate.dto.response.ErrorInfo
import org.eclipse.tractusx.bpdm.gate.dto.response.LsaType
import org.eclipse.tractusx.bpdm.gate.dto.response.PageChangeLogResponse
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntity
import org.eclipse.tractusx.bpdm.gate.exception.ChangeLogOutputError
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ChangelogService(private val changelogRepository: ChangelogRepository) {



    fun getChangeLogByExternalId(externalIds: Collection<String>, fromTime: Instant?, page: Int, pageSize: Int): PageChangeLogResponse<ChangelogEntity> {

        val pageResponse = changelogRepository.run {
            val pageable = PageRequest.of(page, pageSize)
            if (fromTime == null) {
                changelogRepository.findAllByExternalIdIn(externalIds, pageable)
            } else {
                changelogRepository.findAllByExternalIdInAndCreatedAtGreaterThanEqual(
                    externalIds, fromTime, pageable
                )
            }
        }

        val errorInfoList = externalIds.filterNot { id ->
            pageResponse.content.any { it.externalId == id }
        }.map {
            ErrorInfo(
                ChangeLogOutputError.ExternalIdNotFound,
                "$it not found",
                "externalId not found"
            )
        }

        return PageChangeLogResponse(
            page = page, totalElements = pageResponse.totalElements,
            totalPages = pageResponse.totalPages,
            contentSize = pageResponse.content.size,
            content = pageResponse.content,
            invalidEntries = errorInfoList.size,
            errors = errorInfoList
        )
    }

    fun getChangeLogByLsaType(lsaType: LsaType?, fromTime: Instant?, page: Int, pageSize: Int): PageResponse<ChangelogEntity> {

        val pageResponse = changelogRepository.run {
            val pageable = PageRequest.of(page, pageSize)
            if (fromTime == null && lsaType == null) {
                findAll(pageable)
            } else if (fromTime == null ) {
                findAllByBusinessPartnerType(businessPartnerType = lsaType, pageable = pageable)
            } else if ( lsaType == null) {
                findAllByCreatedAtGreaterThanEqual(createdAt = fromTime, pageable = pageable)
            } else {
                findAllByBusinessPartnerTypeAndCreatedAtGreaterThanEqual(
                    businessPartnerType = lsaType,
                    createdAt = fromTime,
                    pageable = pageable
                )
            }
        }

        return PageResponse(
            page = page, totalElements = pageResponse.totalElements,
            totalPages = pageResponse.totalPages,
            contentSize = pageResponse.content.size,
            content = pageResponse.content,
        )
    }
}