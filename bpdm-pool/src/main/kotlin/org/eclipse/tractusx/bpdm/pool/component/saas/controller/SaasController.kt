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

package org.eclipse.tractusx.bpdm.pool.component.saas.controller

import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.api.PoolSaasApi
import org.eclipse.tractusx.bpdm.pool.api.model.ImportIdEntry
import org.eclipse.tractusx.bpdm.pool.api.model.request.ImportIdFilterRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ImportIdMappingResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.SyncResponse
import org.eclipse.tractusx.bpdm.pool.component.saas.config.SaasAdapterConfigProperties
import org.eclipse.tractusx.bpdm.pool.component.saas.service.ImportStarterService
import org.eclipse.tractusx.bpdm.pool.exception.BpdmRequestSizeException
import org.springframework.web.bind.annotation.RestController

@RestController
class SaasController(
    private val partnerImportService: ImportStarterService,
    private val adapterConfigProperties: SaasAdapterConfigProperties
) : PoolSaasApi {

    override fun importBusinessPartners(): SyncResponse {
        return partnerImportService.importAsync()
    }


    override fun getSyncStatus(): SyncResponse {
        return partnerImportService.getImportStatus()
    }


    override fun getImportEntries(importIdFilterRequest: ImportIdFilterRequest): ImportIdMappingResponse {
        if (importIdFilterRequest.importIdentifiers.size > adapterConfigProperties.requestSizeLimit)
            BpdmRequestSizeException(importIdFilterRequest.importIdentifiers.size, adapterConfigProperties.requestSizeLimit)
        return partnerImportService.getImportIdEntries(importIdFilterRequest.importIdentifiers)
    }


    override fun getImportEntries(paginationRequest: PaginationRequest): PageResponse<ImportIdEntry> {
        if (paginationRequest.size > adapterConfigProperties.requestSizeLimit)
            BpdmRequestSizeException(paginationRequest.size, adapterConfigProperties.requestSizeLimit)
        return partnerImportService.getImportIdEntries(paginationRequest)
    }
}