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

import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.api.PoolSuggestionApi
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.BusinessPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.SuggestionResponse
import org.eclipse.tractusx.bpdm.pool.component.opensearch.SearchService
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.SuggestionType
import org.springframework.web.bind.annotation.RestController

@RestController
class SuggestionController(
    val searchService: SearchService
) : PoolSuggestionApi {

    override fun getNameSuggestion(
        text: String?,
        bpSearchRequest: LegalEntityPropertiesSearchRequest,
        addressSearchRequest: AddressPropertiesSearchRequest,
        siteSearchRequest: SitePropertiesSearchRequest,
        pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.NAME,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest, siteSearchRequest),
            pageRequest
        )
    }

    override fun getLegalFormSuggestion(
        text: String?,
        bpSearchRequest: LegalEntityPropertiesSearchRequest,
        addressSearchRequest: AddressPropertiesSearchRequest,
        siteSearchRequest: SitePropertiesSearchRequest,
        pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.LEGAL_FORM,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest, siteSearchRequest),
            pageRequest
        )
    }


    override fun getSiteSuggestion(
        text: String?,
        bpSearchRequest: LegalEntityPropertiesSearchRequest,
        addressSearchRequest: AddressPropertiesSearchRequest,
        siteSearchRequest: SitePropertiesSearchRequest,
        pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.SITE,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest, siteSearchRequest),
            pageRequest
        )
    }

    override fun getStatusSuggestion(
        text: String?,
        bpSearchRequest: LegalEntityPropertiesSearchRequest,
        addressSearchRequest: AddressPropertiesSearchRequest,
        siteSearchRequest: SitePropertiesSearchRequest,
        pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.STATUS,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest, siteSearchRequest),
            pageRequest
        )
    }

    override fun getClassificationSuggestion(
        text: String?,
        bpSearchRequest: LegalEntityPropertiesSearchRequest,
        addressSearchRequest: AddressPropertiesSearchRequest,
        siteSearchRequest: SitePropertiesSearchRequest,
        pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.CLASSIFICATION,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest, siteSearchRequest),
            pageRequest
        )
    }

    override fun getAdminAreaSuggestion(
        text: String?,
        bpSearchRequest: LegalEntityPropertiesSearchRequest,
        addressSearchRequest: AddressPropertiesSearchRequest,
        siteSearchRequest: SitePropertiesSearchRequest,
        pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.ADMIN_AREA,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest, siteSearchRequest),
            pageRequest
        )
    }


    override fun getPostcodeSuggestion(
        text: String?,
        bpSearchRequest: LegalEntityPropertiesSearchRequest,
        addressSearchRequest: AddressPropertiesSearchRequest,
        siteSearchRequest: SitePropertiesSearchRequest,
        pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.POSTCODE,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest, siteSearchRequest),
            pageRequest
        )
    }

    override fun getLocalitySuggestion(
        text: String?,
        bpSearchRequest: LegalEntityPropertiesSearchRequest,
        addressSearchRequest: AddressPropertiesSearchRequest,
        siteSearchRequest: SitePropertiesSearchRequest,
        pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.LOCALITY,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest, siteSearchRequest),
            pageRequest
        )
    }

    override fun getThoroughfareSuggestion(
        text: String?,
        bpSearchRequest: LegalEntityPropertiesSearchRequest,
        addressSearchRequest: AddressPropertiesSearchRequest,
        siteSearchRequest: SitePropertiesSearchRequest,
        pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.THOROUGHFARE,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest, siteSearchRequest),
            pageRequest
        )
    }

    override fun getPremiseSuggestion(
        text: String?,
        bpSearchRequest: LegalEntityPropertiesSearchRequest,
        addressSearchRequest: AddressPropertiesSearchRequest,
        siteSearchRequest: SitePropertiesSearchRequest,
        pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.PREMISE,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest, siteSearchRequest),
            pageRequest
        )
    }


    override fun getPostalDeliverPointSuggestion(
        text: String?,
        bpSearchRequest: LegalEntityPropertiesSearchRequest,
        addressSearchRequest: AddressPropertiesSearchRequest,
        siteSearchRequest: SitePropertiesSearchRequest,
        pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        return searchService.getSuggestion(
            SuggestionType.POSTAL_DELIVERY_POINT,
            text,
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest, siteSearchRequest),
            pageRequest
        )
    }
}