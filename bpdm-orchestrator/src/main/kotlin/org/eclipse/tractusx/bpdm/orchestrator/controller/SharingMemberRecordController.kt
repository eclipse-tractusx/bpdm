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

package org.eclipse.tractusx.bpdm.orchestrator.controller

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.orchestrator.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.service.SharingMemberRecordService
import org.eclipse.tractusx.orchestrator.api.SharingMemberRecord
import org.eclipse.tractusx.orchestrator.api.SharingMemberRecordApi
import org.eclipse.tractusx.orchestrator.api.model.SharingMemberRecordQueryRequest
import org.eclipse.tractusx.orchestrator.api.model.SharingMemberRecordUpdateRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class SharingMemberRecordController(
    private val sharingMemberRecordService: SharingMemberRecordService
): SharingMemberRecordApi{

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.CREATE_TASK})")
    override fun update(request: SharingMemberRecordUpdateRequest): SharingMemberRecord {
       return sharingMemberRecordService.updateRecord(request)
    }

    @PreAuthorize("@stepSecurityService.assertHasReservationAuthority(authentication, \"PoolSync\")")
    override fun queryRecords(
        request: SharingMemberRecordQueryRequest,
        paginationRequest: PaginationRequest
    ): PageDto<SharingMemberRecord> {
        return sharingMemberRecordService.queryRecords(request, paginationRequest)
    }
}