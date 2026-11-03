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

package org.eclipse.tractusx.bpdm.pool.api.v7.client

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.ApiCommons
import org.eclipse.tractusx.bpdm.pool.api.v7.PoolMembersV7Api
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressInvariantVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.changelog.ChangelogEntryVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.changelog.ChangelogSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntitySearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange
interface MembersV7ApiClient : PoolMembersV7Api {

    @PostExchange(value = ApiCommons.MEMBERS_LEGAL_ENTITIES_SEARCH_PATH_V7)
    override fun searchLegalEntities(
        @RequestBody searchRequest: LegalEntitySearchV7Request,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseV7Dto>

    @PostExchange(value = ApiCommons.MEMBERS_SITES_SEARCH_PATH_V7)
    override fun postSiteSearch(
        @RequestBody searchRequest: SiteSearchV7Request,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseV7Dto>

    @PostExchange(value = ApiCommons.MEMBERS_ADDRESSES_SEARCH_PATH_V7)
    override fun searchAddresses(
        @RequestBody searchRequest: AddressSearchV7Request,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressInvariantVerboseV7Dto>

    @PostExchange(value = ApiCommons.MEMBERS_CHANGELOG_SEARCH_PATH_V7)
    override fun searchChangelogEntries(
        @RequestBody changelogSearchRequest: ChangelogSearchV7Request,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<ChangelogEntryVerboseV7Dto>
}