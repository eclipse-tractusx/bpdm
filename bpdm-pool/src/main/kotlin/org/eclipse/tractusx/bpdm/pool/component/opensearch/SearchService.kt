/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.component.opensearch

import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.SuggestionType
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerSearchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SuggestionResponse

/**
 * Provides search functionality on the Catena-x data for the BPDM system
 */
interface SearchService {

    /**
     * Find business partners by matching their field values to [searchRequest] field query texts
     */
    fun searchBusinessPartners(searchRequest: BusinessPartnerSearchRequest,
                               paginationRequest: PaginationRequest
    ): PageResponse<BusinessPartnerSearchResponse>

    /**
     * In business partners matching the [filters], find [field] values matching [text].
     */
    fun getSuggestion(field: SuggestionType,
                      text: String?,
                      filters: BusinessPartnerSearchRequest,
                      paginationRequest: PaginationRequest
    ): PageResponse<SuggestionResponse>
}