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

package org.eclipse.tractusx.bpdm.gate.controller

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.GateRelationChangelogApi
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageChangeLogDto
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.GoldenRecordType
import org.eclipse.tractusx.bpdm.gate.service.ChangelogService
import org.eclipse.tractusx.bpdm.gate.util.PrincipalUtil
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class RelationChangelogController(
    private val changelogService: ChangelogService,
    private val principalUtil: PrincipalUtil
): GateRelationChangelogApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_INPUT_CHANGELOG})")
    override fun getInputChangelog(
        paginationRequest: PaginationRequest,
        searchRequest: ChangelogSearchRequest
    ): PageChangeLogDto<ChangelogGateDto> {
        return changelogService.getChangeLogEntries(
            searchRequest.externalIds,
            principalUtil.resolveTenantBpnl().value,
            searchRequest.timestampAfter,
            StageType.Input,
            GoldenRecordType.Relation,
            paginationRequest.page,
            paginationRequest.size
        )
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_OUTPUT_CHANGELOG})")
    override fun getOutputChangelog(
        paginationRequest: PaginationRequest,
        searchRequest: ChangelogSearchRequest
    ): PageChangeLogDto<ChangelogGateDto> {
        return changelogService.getChangeLogEntries(
            searchRequest.externalIds,
            principalUtil.resolveTenantBpnl().value,
            searchRequest.timestampAfter,
            StageType.Output,
            GoldenRecordType.Relation,
            paginationRequest.page,
            paginationRequest.size
        )
    }
}