/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.bpdm.pool.mapper.apiv6

import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteStateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteWithMainAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessStateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.LegalEntitySiteSearchError
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.LegalEntitySiteSearchError.LegalEntityNotFound
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteGetError
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteGetError.SiteNotFound
import org.springframework.stereotype.Component

/**
 * Maps site records and rejections to their API v6 form.
 */
@Component
class ApiV6SiteMapper(
    private val apiV6AddressMapper: ApiV6AddressMapper,
    private val apiV6BusinessPartnerMapper: ApiV6BusinessPartnerMapper
) {

    /**
     * Returns the API DTO for [record], together with its main address.
     */
    fun toDto(record: SiteRecord): SiteWithMainAddressVerboseV6Dto =
        SiteWithMainAddressVerboseV6Dto(
            site = toSiteDto(record),
            mainAddress = apiV6AddressMapper.toDto(record.mainAddress)
        )

    /**
     * Returns the exception the API reports a rejected site fetch with.
     */
    fun toGetException(errors: List<SiteGetError>): BpdmNotFoundException =
        when (val error = errors.first()) {
            is SiteNotFound -> BpdmNotFoundException("Site", error.bpn)
        }

    /**
     * Returns the exception the API reports a rejected search for the sites of a legal entity with.
     */
    fun toLegalEntitySiteSearchException(errors: List<LegalEntitySiteSearchError>): BpdmNotFoundException =
        when (val error = errors.first()) {
            is LegalEntityNotFound -> BpdmNotFoundException("Business Partner", error.bpn)
        }

    /**
     * Returns the API DTO for [record], without its main address.
     */
    fun toSiteDto(record: SiteRecord): SiteVerboseV6Dto =
        SiteVerboseV6Dto(
            bpns = record.bpn,
            name = record.name,
            states = record.states.map { toStateDto(it) },
            bpnLegalEntity = record.legalEntityBpn,
            confidenceCriteria = apiV6BusinessPartnerMapper.toDto(record.confidenceCriteria),
            isCatenaXMemberData = record.isDataSpaceParticipant,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )

    private fun toStateDto(record: BusinessStateRecord): SiteStateVerboseV6Dto =
        SiteStateVerboseV6Dto(
            BusinessStateDates.reportValidFrom(record.validFrom),
            BusinessStateDates.reportValidTo(record.validTo),
            record.type.toDto()
        )
}
