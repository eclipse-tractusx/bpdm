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

package org.eclipse.tractusx.bpdm.pool.api.client

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.PoolSaasApi
import org.eclipse.tractusx.bpdm.pool.api.model.ImportIdEntry
import org.eclipse.tractusx.bpdm.pool.api.model.request.ImportIdFilterRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ImportIdMappingResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.SyncResponse
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange("/api/saas")
interface SaasApiClient : PoolSaasApi {
    @PostExchange("/identifier-mappings/filter")
    override fun getImportEntries(@RequestBody importIdFilterRequest: ImportIdFilterRequest): ImportIdMappingResponse

    @GetExchange("/identifier-mappings")
    override fun getImportEntries(paginationRequest: PaginationRequest): PageDto<ImportIdEntry>

    @GetExchange("/business-partner/sync")
    override fun getSyncStatus(): SyncResponse

    @PostExchange("/business-partner/sync")
    override fun importBusinessPartners(): SyncResponse
}