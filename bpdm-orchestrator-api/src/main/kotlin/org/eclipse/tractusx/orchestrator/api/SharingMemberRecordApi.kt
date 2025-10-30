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

package org.eclipse.tractusx.orchestrator.api

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.orchestrator.api.model.SharingMemberRecordQueryRequest
import org.eclipse.tractusx.orchestrator.api.model.SharingMemberRecordUpdateRequest
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping(value = [ApiCommons.BASE_PATH_V7_SHARING_MEMBER_RECORDS], produces = [MediaType.APPLICATION_JSON_VALUE])
interface SharingMemberRecordApi {
    @PutMapping
    fun update(@RequestBody request: SharingMemberRecordUpdateRequest): SharingMemberRecord

    @GetMapping
    fun queryRecords(
        @ParameterObject request: SharingMemberRecordQueryRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<SharingMemberRecord>
}