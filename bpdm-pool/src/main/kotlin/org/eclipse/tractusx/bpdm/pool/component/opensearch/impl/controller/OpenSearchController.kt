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

package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.controller


import org.eclipse.tractusx.bpdm.pool.api.PoolOpenSearchApi
import org.eclipse.tractusx.bpdm.pool.api.model.response.SyncDto
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service.OpenSearchSyncStarterService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class OpenSearchController(
    val openSearchSyncService: OpenSearchSyncStarterService
) : PoolOpenSearchApi {

    @PreAuthorize("hasAuthority(@poolSecurityConfigProperties.getManageOpensearchAsRole())")
    override fun export(): SyncResponse {
        return openSearchSyncService.exportAsync()
    }

    @PreAuthorize("hasAuthority(@poolSecurityConfigProperties.getManageOpensearchAsRole())")
    override fun getBusinessPartners(): SyncResponse {
        return openSearchSyncService.getExportStatus()
    }

    @PreAuthorize("hasAuthority(@poolSecurityConfigProperties.getManageOpensearchAsRole())")
    override fun clear() {
        openSearchSyncService.clearOpenSearch()
    }
}
