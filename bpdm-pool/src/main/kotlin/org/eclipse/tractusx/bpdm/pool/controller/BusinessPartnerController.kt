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

package org.eclipse.tractusx.bpdm.pool.controller

import org.eclipse.tractusx.bpdm.pool.client.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.client.service.PoolClientBusinessPartnerInterface
import org.eclipse.tractusx.bpdm.pool.client.dto.response.ChangelogEntryResponse
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.web.bind.annotation.RestController

@RestController
class BusinessPartnerController(
    private val partnerChangelogService: PartnerChangelogService
) : PoolClientBusinessPartnerInterface {
    override fun getChangelogEntries(
        bpn: String,
        paginationRequest: PaginationRequest
    ): PageResponse<ChangelogEntryResponse> {
        return partnerChangelogService.getChangelogEntriesByBpn(bpn.uppercase(), paginationRequest.page, paginationRequest.size)
    }
}